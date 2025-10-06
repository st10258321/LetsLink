package com.example.letslink.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.R
import com.example.letslink.model.User
import com.example.letslink.model.UserEvent
import com.example.letslink.online_database.fb_userRepo
import com.example.letslink.viewmodels.UserViewModel
import com.google.android.material.button.MaterialButton
import com.example.letslink.viewmodels.ViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RegisterPage : AppCompatActivity() {
    private lateinit var viewModel: UserViewModel
    private val viewModelFactory by lazy {
        ViewModelFactory(
            dao = LetsLinkDB.getDatabase(this).userDao()
        )
    }

    private val fbUserRepo = fb_userRepo()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_page)
        val dao = LetsLinkDB.getDatabase(applicationContext).userDao()
        val factory = ViewModelFactory(dao)

        viewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)

        val SignInButton : TextView = findViewById(R.id.sign_in_link)
        val SignUpButton : MaterialButton = findViewById(R.id.sign_up_button)


        SignInButton.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }

        val txtDateOfBirth = findViewById<TextView>(R.id.txt_date_of_birth)
        txtDateOfBirth.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date of birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(supportFragmentManager, "DATE_PICKER")


            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) .format(calendar.time)
                txtDateOfBirth.text = formattedDate
            }
        }






        SignUpButton.setOnClickListener {
            val txtfullName : TextView = findViewById(R.id.txt_full_name)
            val txtemail : TextView = findViewById(R.id.txt_email)
            val txtpassword : TextView = findViewById(R.id.txt_password)
            val txtdateOfBirth : TextView = findViewById(R.id.txt_date_of_birth)
            val txtconfirmPassword : TextView = findViewById(R.id.txt_confirm_password)


            val fName = txtfullName.text.toString()
            val email = txtemail.text.toString()
            val password = txtpassword.text.toString().trim()
            val dateOfBirth = txtdateOfBirth.text.toString().trim()
            val confirmPassword = txtconfirmPassword.text.toString().trim()


            if(fName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && dateOfBirth.isNotEmpty() && confirmPassword.isNotEmpty()){
                try{
                    if(password == confirmPassword)
                    {
                        //saving the user locally on RoomDb
                        viewModel.onEvent(UserEvent.setFirstName(fName))
                        viewModel.onEvent(UserEvent.setEmail(email))
                        viewModel.onEvent(UserEvent.setPassword(password))
                        viewModel.onEvent(UserEvent.setDateOfBirth(dateOfBirth))
                        viewModel.onEvent(UserEvent.createUser)

                        lifecycleScope.launch {
                            viewModel.userState.collect { state ->
                                if(state.isSuccess){

                                    val user = viewModel.getUserByEmail(email)
                                    Log.d("RegisterPage", "Over here --- ${user?.userId} --- ${user?.firstName}")
                                    if(user!= null){
                                        Toast.makeText(this@RegisterPage,"Account created successfully (locally)",Toast.LENGTH_SHORT).show()
                                        val newUser = User(user.userId ,fName, password, dateOfBirth, email, null)
                                        //saving the user remotely on Firebase

                                        fbUserRepo.register(newUser){ success, errorMessage, user ->
                                            if(success){
                                                Toast.makeText(this@RegisterPage,"Account created successfully (remotely)",Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this@RegisterPage, LoginPage::class.java)
                                                startActivity(intent)
                                            }else{
                                                Toast.makeText(this@RegisterPage,"Account creation failed: $errorMessage",Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                }
                            }
                        }


                    }else{
                        Toast.makeText(this,"Passwords do not match",Toast.LENGTH_SHORT).show()
                    }
                }catch(e: Exception){
                    Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_SHORT).show()
                    Log.d("RegisterPage", "Over here --- Error: ${e.message}")
                }

            }else{
                Toast.makeText(this,"All fields are required",Toast.LENGTH_SHORT).show()
            }

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}