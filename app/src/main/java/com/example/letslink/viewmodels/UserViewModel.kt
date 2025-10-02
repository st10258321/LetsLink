package com.example.letslink.viewmodels

import com.example.letslink.local_database.UserDao
import com.example.letslink.model.UserState


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.letslink.model.User
import com.example.letslink.model.UserEvent
import java.security.MessageDigest


class UserViewModel(private val dao: UserDao) : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState = _userState.asStateFlow()

    fun hasPass(hashPassword : String): String
    {
        val bytes = hashPassword.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }
    fun onEvent(event: UserEvent)
    {
        when(event){
            is UserEvent.deleteUser -> {
                viewModelScope.launch{
                    dao.deleteUser(event.user)
                }
            }
            UserEvent.createUser -> {
                val fullName = userState.value.firstName
                val password = userState.value.password
                val email = userState.value.email
                val dateOfBirth = userState.value.dateOfBirth
                val checkedEmail = _userState.equals(UserState::isValid)
                val hashedPassword = hasPass(password)
                if(fullName.isBlank() || password.isBlank() || email.isBlank())
                {
                    _userState.update { it.copy(errorMessage = "All fields are required.") }
                    return
                }
                if (checkedEmail) {
                    _userState.update { it.copy(errorMessage = "Invalid email address.") }
                    return
                }
                if (dateOfBirth.isBlank()) {
                    _userState.update { it.copy(errorMessage = "Date of birth is required.") }
                    return
                }


                val user = User(
                    firstName = fullName,
                    email = email,
                    password = hashedPassword,
                    dateOfBirth = dateOfBirth,
                    )


                viewModelScope.launch {
                    try {
                        dao.upsertUser(user)
                        _userState.update { it.copy(
                            firstName = "",
                            password = "",
                            email = "",
                            dateOfBirth = "",
                            isSuccess = true,
                            errorMessage = null
                        ) }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        _userState.update { it.copy(
                            errorMessage = "Account creation failed: ${e.message}",
                            isSuccess = false
                        ) }
                    }
                }
            }
            is UserEvent.setEmail -> {

                _userState.update { it.copy(
                    email = event.email,
                    errorMessage = null
                ) }
            }
            is UserEvent.setFirstName -> {
                _userState.update { it.copy(
                    firstName = event.firstName,
                    errorMessage = null
                ) }
            }
            is UserEvent.setPassword -> {
                _userState.update { it.copy(
                    password= event.password,
                    errorMessage = null
                ) }
            }

            is UserEvent.setDateOfBirth -> {
                _userState.update { it.copy(
                    dateOfBirth = event.dateOfBirth,
                    errorMessage = null
                ) }
            }
        }
    }
    suspend fun getUserByEmail(email: String): User? {
        return dao.getUserByEmail(email)
    }
    /*
    *  companion object {
         fun provideFactory(dao: UserDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
             @Suppress("UNCHECKED_CAST")
             override fun <T : ViewModel> create(modelClass: Class<T>): T {
                 return UserViewModel(dao) as T
             }
         }
     }
    * */
}