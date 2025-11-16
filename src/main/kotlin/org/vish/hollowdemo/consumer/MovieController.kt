package org.vish.hollowdemo.consumer

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.vish.hollowdemo.model.Movie

@RestController
@RequestMapping("/movies")
@Profile("consumer")
class MovieController(
    private val movieConsumer: MovieConsumer
) {

    @GetMapping
    fun getAllMovies(): List<Movie> {
        return movieConsumer.getAllMovies()
    }

    @GetMapping("/{id}")
    fun getMovieById(@PathVariable id: Long): ResponseEntity<Movie> {
        val movie = movieConsumer.getMovieById(id)
        return if (movie != null) {
            ResponseEntity.ok(movie)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search")
    fun searchMovies(@RequestParam title: String): List<Movie> {
        return movieConsumer.searchByTitle(title)
    }

    @GetMapping("/genre/{genre}")
    fun getMoviesByGenre(@PathVariable genre: String): List<Movie> {
        return movieConsumer.getMoviesByGenre(genre)
    }

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        return movieConsumer.getStats()
    }
}