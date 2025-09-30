package com.example.letslink.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.API_related.LetsLinkDB
import com.example.API_related.UserDao
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.activities.RegisterPage
import com.example.letslink.activities.ResetPasswordPage
import com.example.letslink.model.LoginEvent
import com.example.letslink.nav.HorizontalCoordinator
import com.example.letslink.viewmodels.LoginViewModel
import com.example.letslink.viewmodels.ViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginPage : AppCompatActivity() {
    private lateinit var loginViewModel : LoginViewModel

    private val sessionManager: SessionManager = SessionManager(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)
        var dao : UserDao = LetsLinkDB.getDatabase(applicationContext).userDao()
        val factory = ViewModelFactory(dao)
        loginViewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)

        val hyperLinkForgotPassword: TextView = findViewById(R.id.forgot_password_link)
        val signInButton: MaterialButton = findViewById(R.id.sign_in_button)
        val signUpLink: TextView = findViewById(R.id.sign_up_link)

        hyperLinkForgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordPage::class.java)
            startActivity(intent)
        }

        signInButton.setOnClickListener {
            val email = findViewById<TextView>(R.id.email_edit_text).text.toString()
            val password = findViewById<TextView>(R.id.password_edit_text).text.toString()

            loginViewModel.onEvent(LoginEvent.checkEmail(email))
            loginViewModel.onEvent(LoginEvent.checkPassword(password))
            loginViewModel.onEvent(LoginEvent.Login)

        }
        lifecycleScope.launch{
            loginViewModel.loginState.collect{ state ->
                if(state.isSuccess){
                    //save session data on current user data
                    sessionManager.saveUserSession(loginViewModel._loggedInUser!!.userId.toString(), loginViewModel._loggedInUser!!.email, loginViewModel._loggedInUser!!.firstName)
                    Toast.makeText(this@LoginPage, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginPage, HorizontalCoordinator::class.java)
                    startActivity(intent)
                }else if(state.errorMessage != null){
                    Toast.makeText(this@LoginPage, state.errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        signUpLink.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java)
            startActivity(intent)
        }






        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}