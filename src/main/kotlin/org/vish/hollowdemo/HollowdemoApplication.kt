package org.vish.hollowdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class HollowdemoApplication

fun main(args: Array<String>) {
    runApplication<HollowdemoApplication>(*args)
}
