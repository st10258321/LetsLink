package com.example.api_test

interface GroupEvent {

    object createNote : GroupEvent
    data class setUserID(val userId: String):GroupEvent
    data class setTitle(val title: String):GroupEvent
    data class setDecription(val decription: String):GroupEvent
    data class deleteNotes(val groups: Group):GroupEvent
}