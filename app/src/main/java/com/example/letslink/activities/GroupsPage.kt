package com.example.letslink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.API_related.AppContainer
import com.example.letslink.API_related.MyApp
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.activities.GroupChatActivity
import com.example.letslink.activities.GroupDetailsF
import com.example.letslink.adapter.GroupAdapter
import com.example.letslink.local_database.GroupEvent
import com.example.letslink.viewmodels.GroupViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class GroupsFragment : Fragment() {
    private lateinit var viewModel: GroupViewModel
    private lateinit var appContainer: AppContainer
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: GroupAdapter
    private lateinit var groupsRecyclerView: RecyclerView

    private lateinit var floatingButton: FloatingActionButton
    private lateinit var inviteLinkEditText: EditText
    private lateinit var joinGroupButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupsRecyclerView = view.findViewById(R.id.groups_recycler_view)

        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val application = requireActivity().application

        appContainer = (application as MyApp).container
        inviteLinkEditText = view.findViewById(R.id.inviteLinkEditText) // Use your actual ID
        joinGroupButton = view.findViewById(R.id.joinGroupButton)      // Use your actual ID
        sessionManager = appContainer.sessionManager

        viewModel = ViewModelProvider(
            this,
            GroupViewModel.provideFactory(appContainer.groupRepository, sessionManager)
        )[GroupViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeGroups()
        observeViewModelState()
    }
    private fun observeViewModelState() {
        lifecycleScope.launch {
            viewModel.noteState.collect { state ->
                state.errorMessage?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }

                // Show success message
                if (state.isSuccess) {
                    Toast.makeText(requireContext(), "Group operation successful!", Toast.LENGTH_SHORT).show()

                }
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
    private fun setupRecyclerView() {
        adapter = GroupAdapter(
            onGroupClick = { note ->
                val fragment = GroupDetailsF().apply {
                    arguments = Bundle().apply {
                        putString("groupId", note.groupId.toString())
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // use your container id
                    .addToBackStack(null) // so you can go back
                    .commit()
            },
            onGroupDelete = { note ->
                viewModel.onEvent(GroupEvent.deleteNotes(note))
            }
        )

        groupsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GroupsFragment.adapter
        }
    }
    private fun setupClickListeners() {
        val fab: FloatingActionButton = requireView().findViewById(R.id.floatingId)
        fab.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateGroupFragment())
                .addToBackStack("GroupsFragment")
                .commit()
        }
        joinGroupButton.setOnClickListener {
            val link = inviteLinkEditText.text.toString().trim()
            if (link.isBlank()) {
                Toast.makeText(requireContext(), "Please paste an invite link.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val groupId = extractGroupIdFromLink(link)

            if (groupId != null) {
                viewModel.joinGroupFromInvite(groupId)
                inviteLinkEditText.setText("")
            } else {
                Toast.makeText(requireContext(), "Invalid invite link format. Please check the URL.", Toast.LENGTH_LONG).show()
            }
        }
    }
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