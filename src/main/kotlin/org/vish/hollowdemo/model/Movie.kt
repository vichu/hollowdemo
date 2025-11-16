package org.vish.hollowdemo.model

import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey

@HollowPrimaryKey(fields = ["id"])
data class Movie(
    val id: Long,
    val title: String,
    val genre: String,
    val rating: Double,
    val releaseYear: Int,
    val duration: Int, // in minutes
    val description: String,
    val director: String,
    val cast: List<String>,
    val writers: List<String>,
    val productionCompany: String,
    val country: String,
    val language: String,
    val budget: Long, // in USD
    val boxOffice: Long, // in USD
    val tags: List<String>,
    val ageRating: String,
    val awards: String
)