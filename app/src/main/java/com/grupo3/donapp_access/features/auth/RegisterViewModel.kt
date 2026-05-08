package com.grupo3.donapp_access.features.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
): ViewModel(){
    var selectedRole: String = "Cliente"
}