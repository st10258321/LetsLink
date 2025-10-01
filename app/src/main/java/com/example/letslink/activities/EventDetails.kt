package com.example.letslink.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.commit
import com.example.letslink.R

import com.example.letslink.fragments.CreateTaskFragment
import com.example.letslink.model.Event
import com.google.firebase.auth.FirebaseAuth

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EventDetails : Fragment() {
    companion object{
        private const val  ARG_EVENT_ID = "eventId"
        private const val ARG_TITLE = "title"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_LOCATION = "location"
        private const val ARG_START_TIME = "startTime"
        private const val ARG_END_TIME = "endTime"
        private const val ARG_DATE = "date"

        fun newInstace(event: Event) : EventDetails{
            val fragment = EventDetails()
            val bundke = Bundle().apply {
                putString(ARG_EVENT_ID, event.eventId)
                putString(ARG_TITLE, event.title)
                putString(ARG_DESCRIPTION, event.description)
                putString(ARG_LOCATION, event.location)
                putString(ARG_START_TIME, event.startTime)
                putString(ARG_END_TIME, event.endTime)
                putString(ARG_DATE, event.date)
            }
            fragment.arguments = bundke
            return fragment
            }
    }
    //private val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val name = auth.currentUser?.displayName
        // Back Button Logic
        val backArrow: ImageButton = view.findViewById(R.id.back_image_button)
        backArrow.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
        val addTaskButton: LinearLayout = view.findViewById(R.id.btn_add_task)
        //val userIdString = sharedPreferences.getString("KEY_USER_ID", null) ?: return

        val todoButton = view.findViewById<View>(R.id.btn_view_todo)
        todoButton.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragment_container, ToDoFragment()) // make sure this matches your container id
                addToBackStack(null) // so user can go back
            }
        }
        val eventId = arguments?.getString(ARG_EVENT_ID)
        addTaskButton?.setOnClickListener {
            val createTaskFragment = CreateTaskFragment.newInstance(eventId!!)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, createTaskFragment)
                .addToBackStack(null)
                .commit()
        }

        val title = arguments?.getString(ARG_TITLE)
        val description = arguments?.getString(ARG_DESCRIPTION)
        val location = arguments?.getString(ARG_LOCATION)
        val startTime = arguments?.getString(ARG_START_TIME)
        val endTime = arguments?.getString(ARG_END_TIME)
        val date = arguments?.getString(ARG_DATE)

        val eventTitle: TextView = view.findViewById(R.id.txt_event_title)
        val eventDescription: TextView = view.findViewById(R.id.txt_event_description)
        val eventLocation: TextView = view.findViewById(R.id.txt_event_location)
        val eventDate: TextView = view.findViewById(R.id.txt_event_date)
        val eventTime: TextView = view.findViewById(R.id.txt_event_time)
        val eventHost: TextView = view.findViewById(R.id.txt_event_host)

        eventTime.text = "${eventTime.text} $startTime"
        eventDate.text = "${eventDate.text} $date"
        eventLocation.text = "${eventLocation.text} $location"
        eventDescription.text = description
        eventTitle.text = title
        eventHost.text = name
    }

//    companion object {
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            EventDetails().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}
