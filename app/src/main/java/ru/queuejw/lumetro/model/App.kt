package ru.queuejw.lumetro.model

data class App(
    val mName: String,
    val mPackage: String?,
    val viewType: Int // -1 - letter placeholder, 0 - app holder
)