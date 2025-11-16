package org.vish.hollowdemo.producer

import org.springframework.stereotype.Component
import org.vish.hollowdemo.model.Movie
import kotlin.random.Random

@Component
class MovieDataGenerator {

    private val genres = listOf(
        "Action", "Comedy", "Drama", "Thriller", "Sci-Fi",
        "Romance", "Horror", "Documentary", "Animation", "Mystery"
    )

    private val directors = listOf(
        "Martin Scorsese", "Christopher Nolan", "Greta Gerwig", "Denis Villeneuve",
        "Taika Waititi", "Wes Anderson", "Rian Johnson", "Bong Joon-ho",
        "Jordan Peele", "Guillermo del Toro", "Edgar Wright", "Chloe Zhao",
        "Ryan Coogler", "James Gunn", "Sam Raimi", "Robert Eggers",
        "Ari Aster", "Alex Garland", "David Fincher", "Quentin Tarantino"
    )

    private val actors = listOf(
        "Tom Holland", "Zendaya", "Ryan Gosling", "Margot Robbie",
        "Florence Pugh", "Timothée Chalamet", "Saoirse Ronan", "Oscar Isaac",
        "Adam Driver", "Scarlett Johansson", "Chris Evans", "Robert Downey Jr.",
        "Chadwick Boseman", "Lupita Nyong'o", "Michael B. Jordan", "Daniel Kaluuya",
        "Tessa Thompson", "John Boyega", "Letitia Wright", "Winston Duke",
        "Angela Bassett", "Viola Davis", "Idris Elba", "Mahershala Ali",
        "Sandra Oh", "Awkwafina", "Simu Liu", "Michelle Yeoh",
        "Ke Huy Quan", "Stephanie Hsu", "Jamie Lee Curtis", "Ana de Armas",
        "Pedro Pascal", "Bella Ramsey", "Jeffrey Wright", "Emma Stone"
    )

    private val writers = listOf(
        "Aaron Sorkin", "Charlie Kaufman", "Phoebe Waller-Bridge", "Noah Baumbach",
        "Greta Gerwig", "Jordan Peele", "Rian Johnson", "Taika Waititi",
        "Bong Joon-ho", "Han Jin-won", "Tony Gilroy", "Alex Garland"
    )

    private val productionCompanies = listOf(
        "Netflix", "A24", "Blumhouse Productions", "Legendary Pictures",
        "Plan B Entertainment", "Scott Free Productions", "Amblin Entertainment",
        "Bad Robot", "Lucasfilm", "Marvel Studios", "Pixar Animation Studios"
    )

    private val countries = listOf(
        "United States", "United Kingdom", "South Korea", "Japan",
        "France", "Germany", "Spain", "Mexico", "Canada", "Australia"
    )

    private val languages = listOf(
        "English", "Spanish", "Korean", "Japanese", "French",
        "German", "Mandarin", "Italian", "Portuguese", "Hindi"
    )

    private val ageRatings = listOf("G", "PG", "PG-13", "R", "NC-17", "TV-MA", "TV-14")

    private val tags = listOf(
        "award-winner", "critic's choice", "audience favorite", "binge-worthy",
        "visually stunning", "thought-provoking", "heartwarming", "edge-of-your-seat",
        "mind-bending", "emotional", "inspiring", "hilarious", "terrifying",
        "action-packed", "romantic", "dark comedy", "psychological thriller",
        "family-friendly", "cult classic", "based on true story", "book adaptation"
    )

    private val awards = listOf(
        "Academy Award Winner - Best Picture",
        "Academy Award Nominee - Best Director",
        "Golden Globe Winner",
        "BAFTA Award Winner",
        "Critics Choice Award",
        "SAG Award Winner",
        "Cannes Film Festival Winner",
        "Sundance Film Festival Award",
        "Emmy Award Winner",
        "Independent Spirit Award"
    )

    // Real Netflix hits mixed with humorous variations
    private val famousNetflixMovies = listOf(
        "The Irishman" to "Drama",
        "Bird Box" to "Horror",
        "Don't Look Up" to "Comedy",
        "Glass Onion: A Knives Out Mystery" to "Mystery",
        "Red Notice" to "Action",
        "The Gray Man" to "Action",
        "Extraction" to "Action",
        "Extraction 2: Still Extracting" to "Action",
        "Purple Hearts" to "Romance",
        "The Kissing Booth" to "Romance",
        "The Kissing Booth 2: More Kissing" to "Romance",
        "The Kissing Booth 3: Ultimate Kissing Championship" to "Romance",
        "To All The Boys I've Loved Before" to "Romance",
        "To All The Boys: P.S. I Still Love You" to "Romance",
        "To All The Boys: Always and Forever" to "Romance",
        "Roma" to "Drama",
        "The Trial of the Chicago 7" to "Drama",
        "Marriage Story" to "Drama",
        "Marriage Story 2: The Divorce Papers" to "Comedy",
        "The Adam Project" to "Sci-Fi",
        "The Old Guard" to "Action",
        "Army of the Dead" to "Horror",
        "Army of Thieves" to "Action",
        "Army of Slightly Annoyed People" to "Comedy",
        "Enola Holmes" to "Mystery",
        "Enola Holmes 2: Sherlock's Revenge" to "Mystery",
        "Murder Mystery" to "Comedy",
        "Murder Mystery 2: Definitely Murder This Time" to "Comedy",
        "The Sea Beast" to "Animation",
        "Guillermo del Toro's Pinocchio" to "Animation",
        "Klaus" to "Animation",
        "Wendell & Wild" to "Animation",
        "The Mitchells vs. The Machines" to "Animation",
        "Over the Moon" to "Animation",
        "6 Underground" to "Action",
        "6 Underground 2: Now Actually Underground" to "Action",
        "Spenser Confidential" to "Action",
        "Tyler Rake Chronicles" to "Action",
        "The Harder They Fall" to "Action",
        "The Power of the Dog" to "Drama",
        "The Power of the Cat" to "Comedy",
        "All Quiet on the Western Front" to "Drama",
        "Slumberland" to "Adventure",
        "The School for Good and Evil" to "Fantasy",
        "Matilda The Musical" to "Family",
        "Glass Onion 2: The Avocado" to "Mystery",
        "Knives Out 3: Spoons Out" to "Mystery",
        "Leave the World Behind" to "Thriller",
        "Society of the Snow" to "Drama",
        "Rebel Moon" to "Sci-Fi",
        "Rebel Moon - Part Two: Still Rebelling" to "Sci-Fi",
        "The Mother" to "Action",
        "Heart of Stone" to "Action",
        "Heart of Slightly Hardened Stone" to "Comedy",
        "Nimona" to "Animation",
        "Leo" to "Animation"
    )

    private var currentId = 1L
    private val usedMovies = mutableSetOf<String>()

    fun generateInitialDataset(count: Int = 50000): List<Movie> {
        usedMovies.clear()
        currentId = 1L

        // Use all famous movies first
        val movies = mutableListOf<Movie>()
        famousNetflixMovies.forEach { (title, genre) ->
            movies.add(createMovie(title, genre))
            usedMovies.add(title)
        }

        // Fill remaining with numbered variations for unique titles at scale
        var attemptCount = 0
        while (movies.size < count && attemptCount < count * 2) {
            val (baseTitle, genre) = famousNetflixMovies.random()
            val title = if (Random.nextDouble() < 0.5) {
                // Create variation with suffix
                generateVariation(baseTitle, genre).title
            } else {
                // Create numbered title for guaranteed uniqueness
                "$baseTitle ${movies.size + 1}"
            }

            if (title !in usedMovies) {
                movies.add(createMovie(title, genre))
                usedMovies.add(title)
                attemptCount = 0  // Reset counter on success
            } else {
                attemptCount++
            }
        }

        return movies.take(count)
    }

    fun generateUpdatedDataset(existingMovies: List<Movie>): List<Movie> {
        val movies = existingMovies.toMutableList()
        val random = Random.Default

        // Remove 1-2% of movies (some Netflix originals don't make it...)
        val removeCount = (movies.size * 0.015).toInt().coerceAtLeast(1)
        repeat(removeCount) {
            if (movies.isNotEmpty()) {
                val removed = movies.removeAt(random.nextInt(movies.size))
                usedMovies.remove(removed.title)
            }
        }

        // Update 3-5% of movies (ratings change as people watch)
        val updateCount = (movies.size * 0.04).toInt()
        repeat(updateCount) {
            if (movies.isNotEmpty()) {
                val index = random.nextInt(movies.size)
                val movie = movies[index]
                // Rating changes by +/- 0.5
                val ratingChange = random.nextDouble() - 0.5
                movies[index] = movie.copy(
                    rating = (movie.rating + ratingChange).coerceIn(5.0, 10.0)
                )
            }
        }

        // Add MORE movies than we removed (dataset grows over time!)
        // Add 4-6% new movies to ensure net growth (Netflix keeps shipping!)
        val addCount = (movies.size * 0.05).toInt().coerceAtLeast(removeCount + 50)
        var addedCount = 0
        var attemptCount = 0
        while (addedCount < addCount && attemptCount < addCount * 2) {
            val (baseTitle, genre) = famousNetflixMovies.random()
            val title = if (Random.nextDouble() < 0.3) {
                generateVariation(baseTitle, genre).title
            } else {
                "$baseTitle ${currentId + 1000000}"
            }

            if (title !in usedMovies) {
                movies.add(createMovie(title, genre))
                usedMovies.add(title)
                addedCount++
                attemptCount = 0
            } else {
                attemptCount++
            }
        }

        return movies
    }

    private fun createMovie(title: String, genre: String): Movie {
        val id = currentId++
        val rating = (Random.nextDouble() * 3 + 6).coerceIn(5.0, 10.0) // 6-9 rating (Netflix quality!)
        val releaseYear = Random.nextInt(2016, 2025)
        val duration = when (genre) {
            "Animation" -> Random.nextInt(90, 120)
            "Drama" -> Random.nextInt(120, 180)
            "Action" -> Random.nextInt(100, 150)
            else -> Random.nextInt(90, 130)
        }

        // Generate rich metadata
        val description = generateDescription(title, genre)
        val director = directors.random()
        val cast = actors.shuffled().take(Random.nextInt(3, 8))
        val writerList = writers.shuffled().take(Random.nextInt(1, 3))
        val productionCompany = productionCompanies.random()
        val country = countries.random()
        val language = languages.random()
        val budget = Random.nextLong(5_000_000, 200_000_000)
        val boxOffice = (budget * Random.nextDouble(0.5, 5.0)).toLong()
        val movieTags = tags.shuffled().take(Random.nextInt(2, 5))
        val ageRating = ageRatings.random()
        val movieAwards = if (Random.nextDouble() < 0.3) awards.random() else "None"

        return Movie(
            id = id,
            title = title,
            genre = genre,
            rating = rating,
            releaseYear = releaseYear,
            duration = duration,
            description = description,
            director = director,
            cast = cast,
            writers = writerList,
            productionCompany = productionCompany,
            country = country,
            language = language,
            budget = budget,
            boxOffice = boxOffice,
            tags = movieTags,
            ageRating = ageRating,
            awards = movieAwards
        )
    }

    private fun generateDescription(title: String, genre: String): String {
        val templates = mapOf(
            "Action" to listOf(
                "An explosive adventure featuring $title that keeps audiences on the edge of their seats with intense sequences and spectacular stunts.",
                "A high-octane thriller where $title delivers non-stop action, breathtaking set pieces, and unforgettable moments of heroism.",
                "Experience the adrenaline rush as $title brings unprecedented action and excitement to the screen in this blockbuster hit."
            ),
            "Comedy" to listOf(
                "A hilarious comedy that follows the misadventures in $title, bringing laughs and heartwarming moments throughout.",
                "$title offers a fresh take on comedy with witty dialogue, memorable characters, and laugh-out-loud moments.",
                "Prepare to laugh until your sides hurt with $title, a comedy that perfectly balances humor with heart."
            ),
            "Drama" to listOf(
                "A powerful drama exploring complex human emotions through the lens of $title, featuring stellar performances and compelling storytelling.",
                "$title is an emotionally charged drama that delves deep into the human experience with nuanced performances.",
                "Experience the profound narrative of $title, a drama that will move you with its authentic portrayal of life's challenges."
            ),
            "Thriller" to listOf(
                "A gripping psychological thriller that keeps you guessing until the very end. $title masterfully builds tension and suspense.",
                "$title delivers edge-of-your-seat thrills with unexpected twists and turns that will leave you breathless.",
                "Dive into the dark world of $title, a thriller that expertly weaves mystery, suspense, and shocking revelations."
            ),
            "Sci-Fi" to listOf(
                "A visually stunning science fiction epic that explores futuristic concepts in $title with groundbreaking special effects.",
                "$title pushes the boundaries of imagination with its innovative take on science fiction and spectacular world-building.",
                "Journey to new dimensions with $title, a sci-fi masterpiece that combines cutting-edge visuals with thought-provoking themes."
            ),
            "Romance" to listOf(
                "A heartwarming love story that captures the magic of romance in $title with chemistry that lights up the screen.",
                "$title is a touching romantic tale that explores the complexities of love with genuine emotion and beautiful cinematography.",
                "Fall in love with $title, a romance that celebrates the power of human connection with warmth and authenticity."
            ),
            "Horror" to listOf(
                "A terrifying horror experience that will haunt your nightmares. $title delivers genuine scares and atmospheric dread.",
                "$title is a masterclass in horror, creating an unsettling atmosphere that builds to shocking and memorable moments.",
                "Brace yourself for $title, a horror film that expertly crafts fear through suspenseful storytelling and chilling visuals."
            ),
            "Animation" to listOf(
                "A beautifully animated adventure for all ages. $title combines stunning visuals with heartfelt storytelling.",
                "$title showcases the art of animation at its finest with breathtaking imagery and universal themes that resonate.",
                "Experience the magic of $title, an animated masterpiece that captivates with its artistry and emotional depth."
            ),
            "Mystery" to listOf(
                "An intricate mystery that challenges viewers to piece together clues. $title is a puzzle worth solving.",
                "$title weaves a complex web of intrigue and deception, keeping audiences guessing with clever plot twists.",
                "Unravel the secrets in $title, a mystery that rewards careful attention with satisfying revelations."
            ),
            "Documentary" to listOf(
                "An enlightening documentary that explores fascinating subjects in $title with compelling research and interviews.",
                "$title offers unprecedented access and insights into its subject matter with powerful storytelling.",
                "Discover the truth behind $title, a documentary that educates and inspires with its thorough investigation."
            )
        )

        val genreTemplates = templates[genre] ?: templates["Drama"]!!
        return genreTemplates.random()
    }

    private fun generateVariation(baseTitle: String, baseGenre: String): Movie {
        val sequelNumbers = listOf("The Beginning", "Returns", "Reloaded", "Resurrection",
            "Evolution", "The Final Chapter", "Origins", "Legacy", "Unleashed")
        val variation = "$baseTitle: ${sequelNumbers.random()}"
        return createMovie(variation, baseGenre)
    }
}
