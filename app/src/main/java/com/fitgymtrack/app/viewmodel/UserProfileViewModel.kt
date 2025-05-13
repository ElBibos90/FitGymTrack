package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.UserProfile
import com.fitgymtrack.app.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    fun loadUserProfile() {
        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getUserProfile()

                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        _profileState.value = ProfileState.Success
                    },
                    onFailure = { e ->
                        _profileState.value = ProfileState.Error(e.message ?: "Si è verificato un errore")
                    }
                )
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    fun updateUserProfile(updatedProfile: UserProfile) {
        _profileState.value = ProfileState.Saving

        viewModelScope.launch {
            try {
                val result = repository.updateUserProfile(updatedProfile)

                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        _profileState.value = ProfileState.Success
                    },
                    onFailure = { e ->
                        _profileState.value = ProfileState.Error(e.message ?: "Errore durante il salvataggio")
                    }
                )
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Errore durante il salvataggio")
            }
        }
    }

    sealed class ProfileState {
        object Loading : ProfileState()
        object Saving : ProfileState()
        object Success : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}