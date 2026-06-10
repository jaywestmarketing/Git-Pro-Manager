package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Project
import com.example.data.ProjectStatus
import com.example.network.GithubRepo
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel,
    onProjectClick: (Int) -> Unit,
    onAddFromRepoClick: (Long) -> Unit
) {
    val projects by viewModel.uiState.collectAsStateWithLifecycle()
    val githubToken by viewModel.githubToken.collectAsStateWithLifecycle()
    val newRepos by viewModel.newRepos.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }

    val totalInitiatives = projects.size
    val totalCost = projects.sumOf { it.cost }
    val avgRoi = if (totalCost > 0) {
        projects.sumOf { it.returnOnCost } / totalCost * 100
    } else 0.0

    if (showConfigDialog) {
        GithubConfigDialog(
            currentPat = githubToken,
            onDismiss = { showConfigDialog = false },
            onSave = { token ->
                viewModel.saveToken(token)
                showConfigDialog = false
                viewModel.syncGitHubRepos(token)
            },
            onClear = {
                viewModel.clearToken()
                showConfigDialog = false
            }
        )
    }

    if (newRepos.isNotEmpty()) {
        NewRepoSyncDialog(
            repos = newRepos,
            onDismiss = { newRepos.forEach { repo -> viewModel.dismissNewRepo(repo.id) } }, // Dismiss all
            onAddRepo = { repo -> 
                onAddFromRepoClick(repo.id)
                viewModel.dismissNewRepo(repo.id)
            },
            onIgnoreRepo = { repo ->
                viewModel.dismissNewRepo(repo.id)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("P", color = CardBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Center",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Text(
                    text = "PORTFOLIO OVERVIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubtitleTextColor
                )
            }
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), color = PrimaryPurple, strokeWidth = 2.dp)
            } else if (!githubToken.isNullOrEmpty()) {
                IconButton(onClick = { viewModel.syncGitHubRepos() }) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = TextMain)
                }
            }
            IconButton(onClick = { showConfigDialog = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = TextMain)
            }
        }


        // Key Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Invested Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PrimaryPurpleContainer, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text("TOTAL INVESTED", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryPurple)
                Text(formatCurrencyShort(totalCost), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("+12% vs last mo.", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextMain.copy(alpha = 0.6f))
                }
            }
            
            // Avg ROC Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SecondaryBlueContainer, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text("AVG. ROC", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSecondaryBlueContainer)
                Text(String.format(Locale.getDefault(), "%.1f%%", avgRoi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSecondaryBlueContainer, modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SecondaryBlueAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("High Performing", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SecondaryBlueAccent)
                }
            }
        }

        // Projects Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ACTIVE INITIATIVES ($totalInitiatives)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SubtitleTextColor,
                letterSpacing = 1.sp
            )
            Text(
                "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple,
                modifier = Modifier.padding(4.dp)
            )
        }

        if (projects.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(project = project, onClick = { onProjectClick(project.id) })
                }
            }
        }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    val (statusBg, statusText) = when(project.status) {
        ProjectStatus.PLANNING -> StatusNewBg to StatusNewText
        ProjectStatus.IN_PROGRESS -> StatusOnTrackBg to StatusOnTrackText
        ProjectStatus.COMPLETED -> StatusPausedBg to StatusPausedText // Using paused as similar to completed for contrast
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_card_${project.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = project.name,
                    fontSize = 15.sp,
                    color = ValueTextColor,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.status.name.replace("_", " "),
                        fontSize = 10.sp,
                        color = statusText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 1.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("COST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SubtitleTextColor.copy(alpha = 0.7f))
                    Text(formatCurrencyShort(project.cost), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ValueTextColor)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("ROC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SubtitleTextColor.copy(alpha = 0.7f))
                    val rocColor = if (project.returnOnCost > 0) Color(0xFF059669) else if (project.returnOnCost < 0) Color(0xFFB91C1C) else ValueTextColor
                    val rocPrefix = if (project.returnOnCost > 0) "+" else ""
                    Text("$rocPrefix${formatCurrencyShort(project.returnOnCost)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = rocColor)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("DEADLINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SubtitleTextColor.copy(alpha = 0.7f))
                    Text(formatDate(project.deadline), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ValueTextColor)
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BusinessCenter,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SubtitleTextColor.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No initiatives tracking right now.",
            color = SubtitleTextColor,
            fontSize = 14.sp
        )
    }
}

fun formatCurrencyShort(amount: Double): String {
    if (amount == 0.0) return "$0"
    if (amount >= 1000) {
        val kAmount = amount / 1000.0
        return String.format(Locale.getDefault(), "$%.1fk", kAmount)
    }
    return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}

fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Not set"
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun GithubConfigDialog(
    currentPat: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var token by remember { mutableStateOf(currentPat ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub Integration", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter a Personal Access Token (PAT) to analyze and track your GitHub repositories automatically.", fontSize = 14.sp, color = SubtitleTextColor)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Personal Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(token) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            if (!currentPat.isNullOrEmpty()) {
                TextButton(onClick = onClear, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Remove")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun NewRepoSyncDialog(
    repos: List<GithubRepo>,
    onDismiss: () -> Unit,
    onAddRepo: (GithubRepo) -> Unit,
    onIgnoreRepo: (GithubRepo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Repositories Found", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Would you like to track these new GitHub repositories as projects?", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(repos) { repo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(repo.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (!repo.description.isNullOrEmpty()) {
                                    Text(repo.description.take(50), fontSize = 12.sp, color = SubtitleTextColor)
                                }
                            }
                            Row {
                                TextButton(onClick = { onIgnoreRepo(repo) }) {
                                    Text("Ignore")
                                }
                                Button(
                                    onClick = { onAddRepo(repo) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                        HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss All")
            }
        }
    )
}

