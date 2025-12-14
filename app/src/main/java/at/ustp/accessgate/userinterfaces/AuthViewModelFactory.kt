package at.ustp.accessgate.userinterfaces

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.ustp.accessgate.data.AuthRepository
import at.ustp.accessgate.db.AuthDatabase

class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AuthDatabase.getDatabase(context)
        val repo = AuthRepository(db.authDao())
        return AuthViewModel(repo) as T
    }
}