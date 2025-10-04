package com.example.letslink.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.letslink.R
import com.example.letslink.fragments.CreateTaskFragment

class GroupDetailsF : Fragment() {

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