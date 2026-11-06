package com.example.to_do_list.data

data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val done: Boolean = false
)
