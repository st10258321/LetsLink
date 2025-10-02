package com.example.letslink.fragments

import android.accessibilityservice.GestureDescription

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.letslink.API_related.MyApp
import com.example.letslink.R
import com.example.letslink.viewmodels.GroupViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import com.example.letslink.local_database.GroupEvent


/**
 * Fragment responsible for creating new groups.
 */

class CreateGroupFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_create_group, container, false)
    }

    private var inviteLinkDialog: Dialog? = null
    private lateinit var viewModel: GroupViewModel
    private lateinit var addButton: Button
    private lateinit var etGroupName: EditText
    private lateinit var edDesc: EditText
    private lateinit var context: Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back Button Logic
        val backArrow: ImageView = view.findViewById(R.id.backArrow)
        backArrow.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        context = requireContext()
        // 2. Setup the Create Group Button Logic (Example)

        addButton = view.findViewById(R.id.btnCreateGroup)
        etGroupName = view.findViewById(R.id.etGroupName)
        edDesc = view.findViewById(R.id.etGroupDescription)
        val application = requireActivity().application
        val appContainer = (application as MyApp).container
        viewModel = ViewModelProvider(
            this,
            GroupViewModel.provideFactory(appContainer.groupRepository, appContainer.sessionManager)
        )[GroupViewModel::class.java]
        setupClickListeners()

    }

    override fun onDestroy() {
        super.onDestroy()
        inviteLinkDialog?.dismiss()
    }

    private fun setupClickListeners() {
        addButton.setOnClickListener{
            addNote(etGroupName,edDesc,context,viewModel)
        }
    }
    private fun addNote(edTitle: EditText, edDesc: EditText, context : Context,viewModel:GroupViewModel) {
        val title = edTitle.text.toString()
        val description = edDesc.text.toString()

        if (title.isBlank() || description.isBlank()) {
            Toast.makeText(context, "Title or description must not be empty", Toast.LENGTH_LONG).show()
            return
        }

        viewModel.onEvent(GroupEvent.setTitle(title))
        viewModel.onEvent(GroupEvent.setDecription(description))
        //event to create group
        viewModel.onEvent(GroupEvent.createNote)
    }
    private fun showInviteLinkDialog(link: String,inviteLinkDialog : Dialog,context: Context) {
        // Prevent showing the dialog multiple times
        if (inviteLinkDialog != null && inviteLinkDialog!!.isShowing) {
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Group Created!")
        builder.setMessage("Share this unique invite link:\n\n$link")
            .setPositiveButton("Copy Link") { dialog, _ ->
                copyToClipboard(link)
                Toast.makeText(context, "Invite link copied to clipboard.", Toast.LENGTH_LONG).show()
                dialog.dismiss()

            }
            .setNegativeButton("Done") { dialog, _ ->
                dialog.dismiss()

            }
        inviteLinkDialog!!.show()
    }

    /**
     * metjod to copy text to the system clipboard.
     */
    private fun copyToClipboard(text: String) {

        // Call getSystemService() on the Fragment's associated Activity

        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Group Invite Link", text)
        clipboard.setPrimaryClip(clip)
    }

}


