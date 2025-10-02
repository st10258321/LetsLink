package com.example.letslink.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.local_database.UserDao
import com.example.letslink.R
import com.example.letslink.SessionManager
import com.example.letslink.activities.RegisterPage
import com.example.letslink.activities.ResetPasswordPage
import com.example.letslink.model.LoginEvent
import com.example.letslink.nav.HorizontalCoordinator
import com.example.letslink.viewmodels.LoginViewModel
import com.example.letslink.viewmodels.UserViewModel
import com.example.letslink.viewmodels.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginPage : AppCompatActivity() {
    private lateinit var loginViewModel : LoginViewModel
    private lateinit var userViewModel : UserViewModel
    //google sign in
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)

        //configure google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<SignInButton>(R.id.google_sign_in_btn).setOnClickListener {
            val siginInIntent = googleSignInClient.signInIntent
            startActivityForResult(siginInIntent, RC_SIGN_IN)
        }

        var dao : UserDao = LetsLinkDB.getDatabase(applicationContext).userDao()
        val factory = ViewModelFactory(dao)
        loginViewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)
        val hyperLinkForgotPassword: TextView = findViewById(R.id.forgot_password_link)
        val signInButton: MaterialButton = findViewById(R.id.sign_in_button)
        val signUpLink: TextView = findViewById(R.id.sign_up_link)

        hyperLinkForgotPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordPage::class.java)
            startActivity(intent)
        }
        var searchEmail = ""
        signInButton.setOnClickListener {
            val email = findViewById<TextView>(R.id.email_edit_text).text.toString()
            val password = findViewById<TextView>(R.id.password_edit_text).text.toString()
            searchEmail = email
            loginViewModel.onEvent(LoginEvent.checkEmail(email))
            loginViewModel.onEvent(LoginEvent.checkPassword(password))
            loginViewModel.onEvent(LoginEvent.Login)

        }
        lifecycleScope.launch{
            loginViewModel.loginState.collect{ state ->
                if(state.isSuccess) {
                    val user = userViewModel.getUserByEmail(state.email)
                    if(user != null){
                        sessionManager.saveUserSession(user.userId, user.email, user.firstName)
                        Log.d("LoginPage", "User session saved: ${user.userId}, ${user.email}, ${user.firstName}")
                        val intent = Intent(this@LoginPage, HorizontalCoordinator::class.java)
                        startActivity(intent)
                    }else{
                        Toast.makeText(this@LoginPage, "User not found", Toast.LENGTH_SHORT).show()
                        Log.d("LoginPage", "User not found")
                    }

                    //save session data on current user data

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val account = task.getResult(Exception::class.java)

                loginViewModel.onEvent(LoginEvent.GoogleLogin(account.idToken!!))

            }catch (e: Exception){
                loginViewModel.onEvent(LoginEvent.LoginFailed("Google sign in failed"))
            }
        }
    }
}