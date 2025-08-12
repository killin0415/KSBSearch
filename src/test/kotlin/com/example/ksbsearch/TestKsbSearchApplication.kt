package com.example.ksbsearch

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<KsbSearchApplication>().with(TestcontainersConfiguration::class).run(*args)
}
