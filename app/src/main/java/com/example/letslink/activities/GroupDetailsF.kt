package com.example.letslink.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.adapter.GroupEventAdapter
import com.example.letslink.fragments.CreateTaskFragment
import com.example.letslink.model.Event
import com.example.letslink.model.EventVoting_m
import com.google.firebase.database.FirebaseDatabase
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.letslink.adapter.EventResultsAdapter
import com.example.letslink.local_database.GroupDao
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.model.EventVotingResults
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class GroupDetailsF : Fragment() {
    private lateinit var groupDao : GroupDao

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

        groupDao = LetsLinkDB.getDatabase(requireContext()).groupDao()

        val votingButton: LinearLayout = view.findViewById(R.id.btn_vote_on_events)
        val startChatButton: LinearLayout = view.findViewById(R.id.btn_start_chat)
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString(SessionManager.KEY_USER_ID, null)
        val groupId = arguments?.getString("groupId")
        if(groupId != null){
          lifecycleScope.launch{
              val group = groupDao.getGroupById(groupId)
              if(group != null){
                  view.findViewById<TextView>(R.id.txt_group_name).text = group.groupName
                  view.findViewById<TextView>(R.id.txt_group_description).text = group.description
              }

          }

        }



        votingButton.setOnClickListener {
            val eventRef = FirebaseDatabase.getInstance().getReference("events")

            eventRef.get().addOnSuccessListener {  snapshot ->
                 val events = snapshot.children.mapNotNull { snap ->
                     val event = snap.getValue(EventVoting_m::class.java)?.copy(eventId = snap.key!!)

                    if(event != null && event.groups.contains(groupId)){
                        event
                    }else{
                        null
                    }
                }
                Log.d("--groupvoting--", "Event: ${events.size}")
                if(events.isNotEmpty()){

                    val intent = Intent(requireContext(), EventVoting::class.java)
                    intent.putExtra("events", ArrayList(events))
                    intent.putExtra("groupId", groupId)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                }else{
                    Toast.makeText(requireContext(), "No events found", Toast.LENGTH_SHORT).show()
                }

            }
        }

        val groupEvents = mutableListOf<EventVoting_m>()
        val votingResults = mutableListOf<EventVotingResults>()
        //results for group voting
        val groupVotingDb = FirebaseDatabase.getInstance().getReference("group_voting").child(groupId!!).child("events")
        val groupResultsRV : RecyclerView = view.findViewById(R.id.groupResultsRV)
        groupResultsRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        var groupEventAdapter = GroupEventAdapter(requireContext(), groupEvents)

        var resultsAdapter = EventResultsAdapter(requireContext(), votingResults)
        groupResultsRV.adapter = resultsAdapter

        //most recent events for groups
        val groupEventsRV : RecyclerView = view.findViewById(R.id.groupEventsRV)
        groupEventsRV.layoutManager = LinearLayoutManager(requireContext())
        groupEventsRV.adapter = groupEventAdapter

        val evtRef = FirebaseDatabase.getInstance().getReference("events")


        evtRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { snap ->
                val dataMap = snap.value as? Map<String, Any> ?: return@forEach
                val eventId = snap.key ?: return@forEach
                val title = dataMap["title"] as? String ?: ""
                val description = dataMap["description"] as? String ?: ""
                val groups = dataMap["groups"] as? List<String> ?: emptyList()

                val event = EventVoting_m(eventId, title, description, groups = groups)
                if (event.groups.contains(groupId)) {
                    groupEvents.add(event)
                    groupEventAdapter.notifyDataSetChanged()

                    groupVotingDb.child(event.eventId).child("votes").get()
                        .addOnSuccessListener { voteSnapshot ->
                            val voteMap =
                                voteSnapshot.value as? Map<String, String> ?: emptyMap()
                            val yesCount = voteMap.count { it.value == "dislike" }
                            val noCount = voteMap.count { it.value == "like" }

                            val existingIndex = votingResults.indexOfFirst { it.eventId == event.eventId }
                            if (existingIndex != -1) {
                                votingResults[existingIndex] =
                                    EventVotingResults(event.eventId, event.title, yesCount, noCount)
                            } else {
                                votingResults.add(
                                    EventVotingResults(event.eventId, event.title, yesCount, noCount)
                                )
                            }

                            resultsAdapter.notifyDataSetChanged()
                        }
                }
            }
        resultsAdapter  = EventResultsAdapter(requireContext(), votingResults)
        groupResultsRV.adapter = resultsAdapter

        groupEventAdapter = GroupEventAdapter(requireContext(), groupEvents)
        groupEventsRV.adapter = groupEventAdapter





        startChatButton.setOnClickListener {
            val intent = Intent(requireContext(), GroupChatActivity::class.java)
            requireContext().startActivity(intent)
        }


    }

}
}