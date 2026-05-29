package com.deeplink.app.data.model

data class PaginatedResponse<T>(
    val count: Int,
    val results: List<T>
)
