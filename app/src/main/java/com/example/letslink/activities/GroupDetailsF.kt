package com.example.letslink.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.fragments.CreateTaskFragment
import com.example.letslink.model.EventVoting_m
import com.google.firebase.database.FirebaseDatabase

class GroupDetailsF : Fragment() {

    companion object{
        fun newInstance(): GroupDetailsF{

            return GroupDetailsF()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_group_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val votingButton: LinearLayout = view.findViewById(R.id.btn_vote_on_events)
        val startChatButton: LinearLayout = view.findViewById(R.id.btn_start_chat)
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString(SessionManager.KEY_USER_ID, null)
        val groupId = arguments?.getString("groupId")


        votingButton.setOnClickListener {
            val eventRef = FirebaseDatabase.getInstance().getReference("events").child(groupId!!)

            eventRef.get().addOnSuccessListener {  snapshot ->
                 val events = snapshot.children.mapNotNull { snap ->
                     snap.getValue(EventVoting_m::class.java)?.copy(eventId = snap.key!!)
                    val event = snap.getValue(EventVoting_m::class.java)
                    event?.copy(eventId = snap.key!!) ?: ""
                }

                val intent = Intent(requireContext(), EventVoting::class.java)
                intent.putParcelableArrayListExtra("events", ArrayList(events))
                intent.putExtra("groupId", groupId)
                intent.putExtra("userId", userId)
                startActivity(intent)

            }
        }
        votingButton?.setOnClickListener {
            val intent = Intent(requireContext(), EventVoting::class.java)
            requireContext().startActivity(intent)
        }

        startChatButton.setOnClickListener {
            val intent = Intent(requireContext(), GroupChatActivity::class.java)
            requireContext().startActivity(intent)
        }


    }

}