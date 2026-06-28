package com.aura.app.data.update

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("html_url") val htmlUrl: String
)
