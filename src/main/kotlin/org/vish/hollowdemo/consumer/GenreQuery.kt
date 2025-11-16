package org.vish.hollowdemo.consumer

import com.netflix.hollow.api.consumer.index.FieldPath

data class GenreQuery(
    @FieldPath("genre.value")
    val genre: String
)