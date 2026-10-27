package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRepoItem(
    val fullName: String,
    val name: String,
    val htmlUrl: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val description: String
)

sealed interface GitHubConnectionState {
    data object NotConnected : GitHubConnectionState
    data object Checking : GitHubConnectionState
    data class ReposLoaded(
        val username: String,
        val repos: List<GitHubRepoItem>
    ) : GitHubConnectionState
    data class RepoVerified(
        val repo: GitHubRepoItem,
        val statusNote: String
    ) : GitHubConnectionState
    data class PrivateRepoSelected(
        val fullName: String,
        val htmlUrl: String,
        val explanation: String
    ) : GitHubConnectionState
    data class Error(val message: String) : GitHubConnectionState
}

/**
 * Handles official GitHub web authorization / repo creation intents and live GitHub REST API
 * repository discovery without ever asking the user to paste passwords or personal access tokens.
 */
object GitHubSyncManager {

    /**
     * Opens GitHub's official repository creation page in the user's authenticated browser,
     * pre-selecting PRIVATE visibility as requested in Step 1.
     */
    fun openOfficialGitHubCreateRepo(
        context: Context,
        suggestedRepoName: String = "my-personal-ai-assistant",
        isPrivate: Boolean = true
    ) {
        val visibility = if (isPrivate) "private" else "public"
        val url = "https://github.com/new?name=${Uri.encode(suggestedRepoName)}&visibility=$visibility&description=${Uri.encode("Native Android Personal AI Assistant (Kotlin + Jetpack Compose)")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the selected GitHub repository's web upload page in the user's authenticated browser
     * so they can commit the verified source ZIP/files without exposing tokens to third-party apps.
     */
    fun openGitHubRepoInBrowser(context: Context, repoFullName: String) {
        val clean = repoFullName.trim().removePrefix("https://github.com/").trim('/')
        val url = if (clean.isNotBlank()) "https://github.com/$clean" else "https://github.com"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Fetches a user's existing repositories via the official GitHub REST API (`api.github.com`)
     * so the user can pick an existing repository after signing in.
     */
    suspend fun fetchUserRepositories(usernameOrOrg: String): GitHubConnectionState =
        withContext(Dispatchers.IO) {
            val cleanUser = usernameOrOrg
                .trim()
                .removePrefix("https://github.com/")
                .split("/")
                .firstOrNull()
                ?.trim()
                .orEmpty()

            if (cleanUser.isBlank()) {
                return@withContext GitHubConnectionState.Error("Kripya apna GitHub username enter karein (jaise: octocat).")
            }

            try {
                val endpoint = URL("https://api.github.com/users/$cleanUser/repos?sort=updated&per_page=15")
                val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "PersonalAIAssistant-Android")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    val list = mutableListOf<GitHubRepoItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            GitHubRepoItem(
                                fullName = obj.optString("full_name"),
                                name = obj.optString("name"),
                                htmlUrl = obj.optString("html_url"),
                                isPrivate = obj.optBoolean("private", false),
                                defaultBranch = obj.optString("default_branch", "main"),
                                description = obj.optString("description", "")
                            )
                        )
                    }
                    GitHubConnectionState.ReposLoaded(username = cleanUser, repos = list)
                } else if (code == 404) {
                    GitHubConnectionState.Error("GitHub user '$cleanUser' nahi mila. Username check karein.")
                } else {
                    GitHubConnectionState.Error("GitHub API response HTTP $code.")
                }
            } catch (e: Exception) {
                GitHubConnectionState.Error("Internet ya GitHub connection error: ${e.localizedMessage}")
            }
        }

    /**
     * Verifies a specific `owner/repo` on GitHub. If it returns 404 because it is a PRIVATE repository,
     * explains clearly how private repositories work without asking for personal access tokens.
     */
    suspend fun verifyRepositoryStatus(
        repoInput: String,
        preferPrivate: Boolean = true
    ): GitHubConnectionState = withContext(Dispatchers.IO) {
        val clean = repoInput
            .trim()
            .removePrefix("https://github.com/")
            .removeSuffix(".git")
            .trim('/')

        if (!clean.contains("/")) {
            return@withContext GitHubConnectionState.Error("Repository format 'username/repository-name' hona chahiye.")
        }

        try {
            val endpoint = URL("https://api.github.com/repos/$clean")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PersonalAIAssistant-Android")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val code = conn.responseCode
            if (code == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                val item = GitHubRepoItem(
                    fullName = obj.optString("full_name", clean),
                    name = obj.optString("name", clean.substringAfter("/")),
                    htmlUrl = obj.optString("html_url", "https://github.com/$clean"),
                    isPrivate = obj.optBoolean("private", false),
                    defaultBranch = obj.optString("default_branch", "main"),
                    description = obj.optString("description", "")
                )
                GitHubConnectionState.RepoVerified(
                    repo = item,
                    statusNote = "Verified live on GitHub (${if (item.isPrivate) "Private" else "Public"}, branch: ${item.defaultBranch}). Use AI Studio's top-bar GitHub push or upload the verified Source ZIP."
                )
            } else if (code == 404 && preferPrivate) {
                GitHubConnectionState.PrivateRepoSelected(
                    fullName = clean,
                    htmlUrl = "https://github.com/$clean",
                    explanation = "Target set to '$clean' (Private Repository). Because this app never asks for your secret GitHub password or Personal Access Token, private repositories are hidden from unauthenticated checks. To push directly: tap the GitHub icon in the AI Studio top toolbar, or tap 'Create/Open on GitHub' below and upload the Verified Source ZIP."
                )
            } else {
                GitHubConnectionState.Error("Repository '$clean' public GitHub par nahi mila (HTTP $code). Agar yeh Private repo hai toh 'Private Repo' select karein.")
            }
        } catch (e: Exception) {
            GitHubConnectionState.Error("GitHub check failed: ${e.localizedMessage}")
        }
    }
}
