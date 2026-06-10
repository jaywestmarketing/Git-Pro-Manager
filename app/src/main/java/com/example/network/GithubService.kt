package com.example.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface GithubService {
    @GET("user/repos?sort=updated&per_page=100")
    suspend fun getUserRepos(
        @Header("Authorization") token: String
    ): List<GithubRepo>

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getRepoReadme(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubReadme
}
