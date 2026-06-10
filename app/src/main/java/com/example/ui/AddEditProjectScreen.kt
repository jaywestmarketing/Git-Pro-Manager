package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.data.ProjectStatus
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProjectScreen(
    viewModel: ProjectViewModel,
    projectId: Int?,
    githubRepoId: Long? = null,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var returnOnCostStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ProjectStatus.PLANNING) }
    
    // For simplicity, hardcode deadline or use fixed for now.
    // In a full app, we'd use DatePicker
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, 1)
    var deadline by remember { mutableLongStateOf(calendar.timeInMillis) }

    var existingProject by remember { mutableStateOf<Project?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var internalGithubRepoId by remember { mutableStateOf<Long?>(githubRepoId) }

    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.getProject(projectId).collect { proj ->
                if (proj != null) {
                    existingProject = proj
                    name = proj.name
                    description = proj.description
                    costStr = proj.cost.toString()
                    returnOnCostStr = proj.returnOnCost.toString()
                    status = proj.status
                    deadline = proj.deadline
                    internalGithubRepoId = proj.githubRepoId
                }
            }
        }
    }

    LaunchedEffect(githubRepoId, existingProject) {
        if (githubRepoId != null && existingProject == null) {
            // We are adding a new project from a github repo. Analyze it!
            isAnalyzing = true
            viewModel.analyzeGithubRepo(githubRepoId, onResult = { result ->
                isAnalyzing = false
                name = result.name
                description = result.description
                if (result.costEstimate != null) costStr = result.costEstimate.replace("[^0-9.]".toRegex(), "")
                if (result.roiProjection != null) returnOnCostStr = result.roiProjection.replace("[^0-9.]".toRegex(), "")
                
                result.statusText?.let { statStr ->
                    ProjectStatus.entries.find { it.name.equals(statStr, ignoreCase = true) }?.let {
                        status = it
                    }
                }
            }, onError = { error ->
                isAnalyzing = false
                analysisError = error
                // Even on error, we just fallback to blank form where user can type
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (projectId == null) "New Initiative" else "Edit Initiative") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (projectId != null && existingProject != null) {
                    IconButton(onClick = {
                        viewModel.deleteProject(existingProject!!)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Analyzing Repo via Local LLM...")
                    }
                }
            } else if (analysisError != null) {
                Text(
                    text = "Analysis failed: $analysisError. Please enter details manually.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Initiative Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = textFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Cost ($)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = textFieldColors()
                )

                OutlinedTextField(
                    value = returnOnCostStr,
                    onValueChange = { returnOnCostStr = it },
                    label = { Text("Return ($)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = textFieldColors()
                )
            }

            Text("Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectStatus.entries.forEach { stat ->
                    FilterChip(
                        selected = status == stat,
                        onClick = { status = stat },
                        label = { Text(stat.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        val returnCost = returnOnCostStr.toDoubleOrNull() ?: 0.0
                        
                        val newProj = Project(
                            id = projectId ?: 0,
                            name = name,
                            description = description,
                            deadline = deadline,
                            cost = cost,
                            returnOnCost = returnCost,
                            status = status,
                            githubRepoId = internalGithubRepoId
                        )

                        if (projectId == null) {
                            viewModel.insertProject(newProj)
                        } else {
                            viewModel.updateProject(newProj)
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_button"),
                enabled = name.isNotBlank()
            ) {
                Text(if (projectId == null) "Create Initiative" else "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
