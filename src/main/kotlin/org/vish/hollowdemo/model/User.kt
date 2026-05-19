package org.vish.hollowdemo.model

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey

@HollowPrimaryKey(fields = ["userId"])
data class User(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val userSince: String,      // "YYYY-MM-DD"
    val planName: String,       // "AD_PLAN", "STANDARD", "PREMIUM"
    val recentlyWatchedMovieIds: List<Long>
)
