package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    COMPLETED
}

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val deadline: Long, // timestamp in ms
    val cost: Double = 0.0,
    val returnOnCost: Double = 0.0, // Expected return amount
    val status: ProjectStatus = ProjectStatus.PLANNING,
    val githubRepoId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
