package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class FlashcardItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String = "",
    val answer: String = "",
    val box: Int = 1, // Leitner Box 1 to 5
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewDate: Long = 0L,
    val sourceVideoUrl: String? = null,
    val sourceWikiUrl: String? = null
)
