package com.hyperos.blog.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class Post(
    val id: Long = 0,
    val slug: String = "",
    val titleZh: String = "",
    val titleEn: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val featured: Boolean = false,
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class PostListData(
    val posts: List<Post> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val pageSize: Int = 10,
    val totalPages: Int = 0,
)

@Serializable
data class Category(
    val id: Long = 0,
    val name_zh: String = "",
    val name_en: String = "",
    val count: Long = 0,
)

@Serializable
data class TagCount(
    val name: String = "",
    val count: Long = 0,
)

@Serializable
data class ArchiveGroup(
    val year: String = "",
    val month: String = "",
    val posts: List<Post> = emptyList(),
)

@Serializable
data class Comment(
    val id: Long = 0,
    val nickname: String = "",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class CommentInput(
    val nickname: String = "",
    val email: String = "",
    val content: String = "",
)

@Serializable
data class Message(
    val id: Long = 0,
    val nickname: String = "",
    val content: String = "",
    val createdAt: String = "",
)

@Serializable
data class Friend(
    val id: Long = 0,
    val name: String = "",
    val url: String = "",
    val avatar: String = "",
    val description: String = "",
    val sort: Int = 0,
)

@Serializable
data class SiteStats(
    val postCount: Long = 0,
    val commentCount: Long = 0,
    val messageCount: Long = 0,
    val viewCount: Long = 0,
    val totalVisits: Long = 0,
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val username: String = "",
)

@Serializable
data class PostInput(
    val slug: String = "",
    val titleZh: String = "",
    val titleEn: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "published",
    val pinned: Boolean = false,
    val featured: Boolean = false,
)