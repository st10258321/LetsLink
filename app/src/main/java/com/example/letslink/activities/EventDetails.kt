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
import androidx.fragment.app.commit
import com.example.letslink.R

import com.example.letslink.fragments.CreateTaskFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class EventDetails : Fragment() {
    private val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    private var param1: String? = null
    private var param2: String? = null

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

        // Back Button Logic
        val backArrow: ImageButton = view.findViewById(R.id.back_image_button)
        backArrow.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
        val addTaskButton: LinearLayout = view.findViewById(R.id.btn_add_task)
        val userIdString = sharedPreferences.getString("KEY_USER_ID", null) ?: return

        val todoButton = view.findViewById<View>(R.id.btn_view_todo)
        todoButton.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragment_container, ToDoFragment()) // make sure this matches your container id
                addToBackStack(null) // so user can go back
            }
        }
        addTaskButton?.setOnClickListener {
            val createTaskFragment = CreateTaskFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, createTaskFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EventDetails().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
