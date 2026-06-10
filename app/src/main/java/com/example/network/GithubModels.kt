package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubRepo(
    val id: Long,
    val name: String,
    val description: String?,
    val pushed_at: String?,
    val full_name: String,
    val owner: GithubOwner
)

@JsonClass(generateAdapter = true)
data class GithubOwner(
    val login: String
)

@JsonClass(generateAdapter = true)
data class GithubReadme(
    val content: String,
    val encoding: String
)
