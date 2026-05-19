package org.vish.hollowdemo.consumer

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.vish.hollowdemo.model.Movie

@RestController
@RequestMapping("/movies")
@Profile("consumer")
class CatalogController(
    private val catalogConsumer: CatalogConsumer
) {

    @GetMapping
    fun getAllMovies(): List<Movie> = catalogConsumer.getAllMovies()

    @GetMapping("/{id}")
    fun getMovieById(@PathVariable id: Long): ResponseEntity<Movie> {
        val movie = catalogConsumer.getMovieById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(movie)
    }

    @GetMapping("/search")
    fun searchMovies(@RequestParam title: String): List<Movie> =
        catalogConsumer.searchByTitle(title)

    @GetMapping("/genre/{genre}")
    fun getMoviesByGenre(@PathVariable genre: String): List<Movie> =
        catalogConsumer.getMoviesByGenre(genre)

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> = catalogConsumer.getStats()
}
