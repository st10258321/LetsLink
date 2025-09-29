package com.example.api_test

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import android.app.Dialog

class GroupAdd: AppCompatActivity() {

    private lateinit var viewModel: GroupViewModel
    private lateinit var addButton: Button
    private lateinit var edTitle: EditText
    private lateinit var edDesc: EditText
    private var inviteLinkDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notes_add)

        // Initialize UI components
        addButton = findViewById(R.id.addButton)
        edTitle = findViewById(R.id.edTitle)
        edDesc = findViewById(R.id.edDesc)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val appContainer = (application as MyApp).container

        viewModel = ViewModelProvider(
            this,
            GroupViewModel.provideFactory(appContainer.groupRepository, appContainer.sessionManager)
        )[GroupViewModel::class.java]

        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        inviteLinkDialog?.dismiss()
    }

    private fun setupClickListeners() {
        addButton.setOnClickListener {
            addNote()
        }
    }

    private fun addNote() {
        val title = edTitle.text.toString()
        val description = edDesc.text.toString()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(this,"Title or description must not be empty", Toast.LENGTH_LONG).show()
            return
        }

        viewModel.onEvent(GroupEvent.setTitle(title))
        viewModel.onEvent(GroupEvent.setDecription(description))
        //event to create group
        viewModel.onEvent(GroupEvent.createNote)
  }

    /**
     * Checks the GroupState to handle loading, errors, and the resulting invite link.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.noteState.collectLatest { state ->
                // Disables add button button while the API call is running
                addButton.isEnabled = !state.isLoading

                // Handle Errors
                state.errorMessage?.let { error ->
                    Toast.makeText(this@GroupAdd, error, Toast.LENGTH_LONG).show()
                     }

                // Handle Success and Invite Link: Show the dialog for the link
                if (state.inviteLink != null && state.isSuccess) {
                    showInviteLinkDialog(state.inviteLink)
                }
            }
        }
    }

    /**
     * Displays a dialog with the invitation link and a copy button.
     */
    private fun showInviteLinkDialog(link: String) {
        // Prevent showing the dialog multiple times
        if (inviteLinkDialog != null && inviteLinkDialog!!.isShowing) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Group Created!")
        builder.setMessage("Share this unique invite link:\n\n$link")
            .setPositiveButton("Copy Link") { dialog, _ ->
                copyToClipboard(link)
                Toast.makeText(this, "Invite link copied to clipboard.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish() // Exit
            }
            .setNegativeButton("Done") { dialog, _ ->
                dialog.dismiss()
                finish() // Exit if the user closes it
            }

        inviteLinkDialog = builder.create()
        inviteLinkDialog!!.show()
    }

    /**
     * metjod to copy text to the system clipboard.
     */
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Group Invite Link", text)
        clipboard.setPrimaryClip(clip)
    }
}
