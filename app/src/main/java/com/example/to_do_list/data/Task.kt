package com.example.to_do_list.data

data class Task(
    val id: Long,
    val title: String,
    val description: String = "",
    val done: Boolean = false
)
