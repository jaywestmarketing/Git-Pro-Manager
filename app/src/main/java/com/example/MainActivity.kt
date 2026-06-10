package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.data.TokenManager
import com.example.ui.ProjectApp
import com.example.ui.ProjectViewModel
import com.example.ui.ProjectViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val database = AppDatabase.getDatabase(this)
    val repository = ProjectRepository(database.projectDao())
    val tokenManager = TokenManager(this)
    val localLlmManager = com.example.llm.LocalLlmManager(this)
    val factory = ProjectViewModelFactory(repository, tokenManager, localLlmManager)
    val viewModel = ViewModelProvider(this, factory)[ProjectViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      ProjectApp(viewModel = viewModel)
    }
  }
}
