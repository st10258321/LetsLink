package com.example.api_test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class GroupListActivity : AppCompatActivity() {

    private lateinit var adapter: GroupAdapter
    private lateinit var viewModel: GroupViewModel
    private lateinit var sessionManager: SessionManager


    private lateinit var inviteLinkEditText: EditText
    private lateinit var joinGroupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_front)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

       val appContainer = (application as MyApp).container

        sessionManager = appContainer.sessionManager

        viewModel = ViewModelProvider(
            this,
            GroupViewModel.provideFactory(appContainer.groupRepository, sessionManager)
        )[GroupViewModel::class.java]

        inviteLinkEditText = findViewById(R.id.inviteLinkEditText)
        joinGroupButton = findViewById(R.id.joinGroupButton)

        setupRecyclerView()
        setupClickListeners()
        observeGroups()
        observeViewModelState()
    }

    private fun setupRecyclerView() {
        adapter = GroupAdapter(
            onGroupClick =  { note ->
                val intent = Intent(this, NotesUpdate::class.java).apply {
                    putExtra("NOTE_ID", note.groupId.toString()) // Use toString() for UUID
                }
                startActivity(intent)
            },
            onGroupDelete = { note ->
                viewModel.onEvent(GroupEvent.deleteNotes(note))
            }
        )

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@GroupListActivity)
            adapter = this@GroupListActivity.adapter
        }
    }

    private fun setupClickListeners() {
        findViewById<FloatingActionButton>(R.id.floatingId).setOnClickListener {
            startActivity(Intent(this, GroupAdd::class.java))
        }

        joinGroupButton.setOnClickListener {
            val link = inviteLinkEditText.text.toString().trim()
            if (link.isBlank()) {
                Toast.makeText(this, "Please paste an invite link.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val groupId = extractGroupIdFromLink(link)

            if (groupId != null) {
                viewModel.joinGroupFromInvite(groupId)
                inviteLinkEditText.setText("")
            } else {
                Toast.makeText(this, "Invalid invite link format. Please check the URL.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeGroups() {
        lifecycleScope.launch {
            viewModel.groups.collect { groups ->
                adapter.submitList(groups)
                println("Loaded ${groups.size} groups (via ViewModel and Repository)")
            }
        }
    }

    // this will display for errors and success messages
    private fun observeViewModelState() {
        lifecycleScope.launch {
            viewModel.noteState.collect { state ->
                state.errorMessage?.let { msg ->
                    Toast.makeText(this@GroupListActivity, msg, Toast.LENGTH_LONG).show()
                                    }

                // Show success message
                if (state.isSuccess) {
                    Toast.makeText(this@GroupListActivity, "Group operation successful!", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }

    /**
     * method that extracts the Group ID from URL format.
     * */
    private fun extractGroupIdFromLink(link: String): String? {

        val path = link.substringAfterLast("://").substringAfter('/')
        val parts = path.split('/')

        return if (parts.size >= 2 && parts[parts.size - 2] == "invite") {
            parts.last()
        } else {
            null
        }
    }
}
