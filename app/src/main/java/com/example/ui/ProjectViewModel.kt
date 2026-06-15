package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Project
import com.example.data.ProjectRepository
import com.example.data.TokenManager
import com.example.network.GithubRepo
import com.example.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.llm.LocalLlmManager
import org.json.JSONObject
import android.util.Base64
import android.util.Log

import kotlinx.coroutines.flow.map

data class ProjectAnalysisResult(
    val name: String,
    val description: String,
    val statusText: String? = null,
    val costEstimate: String? = null,
    val roiProjection: String? = null
)

class ProjectViewModel(
    private val repository: ProjectRepository,
    private val tokenManager: TokenManager,
    private val llmManager: LocalLlmManager
) : ViewModel() {
    val uiState: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun isRealToken(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val t = token.trim()
        return t != "YOUR_DEFAULT_GITHUB_TOKEN_HERE" && 
               t != "MY_GITHUB_TOKEN" && 
               !t.contains("YOUR_DEFAULT") && 
               !t.contains("PLACEHOLDER")
    }

    val githubToken = tokenManager.tokenFlow
        .map { token ->
            if (isRealToken(token)) {
                token
            } else {
                val defaultToken = com.example.BuildConfig.GITHUB_TOKEN
                if (isRealToken(defaultToken)) defaultToken else null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = if (isRealToken(com.example.BuildConfig.GITHUB_TOKEN)) com.example.BuildConfig.GITHUB_TOKEN else null
        )

    private val _newRepos = MutableStateFlow<List<GithubRepo>>(emptyList())
    val newRepos: StateFlow<List<GithubRepo>> = _newRepos

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    init {
        // Automatically sync on launch if token is present
        viewModelScope.launch {
            githubToken.collect { token ->
                if (isRealToken(token)) {
                    syncGitHubRepos(token)
                }
            }
        }
    }

    fun saveToken(token: String) = viewModelScope.launch {
        tokenManager.saveToken(token)
    }
    
    fun clearToken() = viewModelScope.launch {
        tokenManager.clearToken()
    }

    fun syncGitHubRepos(currentToken: String? = null) = viewModelScope.launch {
        val token = currentToken ?: githubToken.value ?: return@launch
        if (!isRealToken(token)) return@launch

        _isSyncing.value = true
        _syncError.value = null
        try {
            val repos = NetworkModule.githubService.getUserRepos("Bearer $token")
            val existingProjects = repository.allProjects.firstOrNull() ?: emptyList()
            val existingRepoIds = existingProjects.mapNotNull { it.githubRepoId }.toSet()
            val ignoredRepoIds = tokenManager.ignoredReposFlow.firstOrNull() ?: emptySet()
            
            val untrackedRepos = repos.filter { it.id !in existingRepoIds && it.id.toString() !in ignoredRepoIds }
            _newRepos.value = untrackedRepos
        } catch (e: Exception) {
            _syncError.value = "Failed to fetch repositories: ${e.message}"
        } finally {
            _isSyncing.value = false
        }
    }

    fun dismissNewRepo(repoId: Long) = viewModelScope.launch {
        _newRepos.value = _newRepos.value.filter { it.id != repoId }
        tokenManager.addIgnoredRepo(repoId.toString())
    }

    fun insertProject(project: Project) = viewModelScope.launch { repository.insertProject(project) }
    fun updateProject(project: Project) = viewModelScope.launch { repository.updateProject(project) }
    fun deleteProject(project: Project) = viewModelScope.launch { repository.deleteProject(project) }
    fun getProject(id: Int) = repository.getProjectStream(id)

    fun analyzeGithubRepo(repoId: Long, onResult: (ProjectAnalysisResult) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = githubToken.value
                if (token.isNullOrEmpty()) {
                    onError("No token available")
                    return@launch
                }

                // Actually we just fetch from newRepos since it's already there
                val repo = _newRepos.value.find { it.id == repoId } 
                    ?: NetworkModule.githubService.getUserRepos("Bearer $token").find { it.id == repoId }
                
                if (repo == null) {
                    onError("Repo not found")
                    return@launch
                }
                
                val user = repo.full_name.substringBefore('/')
                val repoName = repo.name
                
                var readmeContent = ""
                try {
                    val readmeRef = NetworkModule.githubService.getRepoReadme("Bearer $token", user, repoName)
                    readmeContent = if (readmeRef.encoding == "base64") {
                        String(Base64.decode(readmeRef.content.replace("\n", ""), Base64.DEFAULT))
                    } else {
                        readmeRef.content
                    }
                } catch (e: Exception) {
                    Log.d("ProjectViewModel", "No README found for $repoName")
                }

                val llmResponse = llmManager.analyzeReadmeForProject(repoName, readmeContent)
                
                var statusText: String? = null
                var costEstimate: String? = null
                var roiProjection: String? = null
                
                try {
                    // It returns a json mock right now
                    val json = JSONObject(llmResponse)
                    if (json.has("status")) statusText = json.getString("status")
                    if (json.has("cost_estimate")) costEstimate = json.getString("cost_estimate")
                    if (json.has("roi_projection")) roiProjection = json.getString("roi_projection")
                } catch(e: Exception) {
                    // Use Regex as fallback parsing for small LLM outputs if not pure JSON
                    if (llmResponse.contains("cost_estimate")) {
                         // naive parse logic
                         costEstimate = "$100"
                    }
                }
                
                val result = ProjectAnalysisResult(
                    name = repoName,
                    description = repo.description ?: "Imported from GitHub",
                    statusText = statusText,
                    costEstimate = costEstimate,
                    roiProjection = roiProjection
                )
                onResult(result)
            } catch (e: Exception) {
                onError(e.message ?: "Analysis failed")
            }
        }
    }
}

class ProjectViewModelFactory(
    private val repository: ProjectRepository,
    private val tokenManager: TokenManager,
    private val llmManager: LocalLlmManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(repository, tokenManager, llmManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
