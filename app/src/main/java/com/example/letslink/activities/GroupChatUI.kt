package com.example.letslink.activities

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.adapters.MessagesAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupChatActivity : AppCompatActivity() {

    private var isFabOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_group_chat_u_i)

        // Setup RecyclerView

        val recyclerView = findViewById<RecyclerView>(R.id.messages_recycler)

        // Dummy messages (true = sent, false = received)
        val messages = listOf(
            true to "Hey, how are you?",
            false to "Chilling like a villain on the ceiling sipping penicillin, you?",
            true to "Working on this chat UI 😅",
            false to "Haha, looks like WhatsApp already 🔥"
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MessagesAdapter(messages)


        // FAB toggle logic

        val fabMain = findViewById<FloatingActionButton>(R.id.fab_main)
        val fabMenu = findViewById<LinearLayout>(R.id.fab_menu)

        fabMain.setOnClickListener {
            if (isFabOpen) {
                fabMenu.visibility = LinearLayout.GONE
                fabMain.setImageResource(R.drawable.ic_add)
            } else {
                fabMenu.visibility = LinearLayout.VISIBLE
                fabMain.setImageResource(R.drawable.ic_close)
            }
            isFabOpen = !isFabOpen
        }
    }
}
