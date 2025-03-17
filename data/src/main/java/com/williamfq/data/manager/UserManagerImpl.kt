package com.williamfq.data.manager

import com.williamfq.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManagerImpl @Inject constructor() : UserManager {
    // Ejemplo de datos simulados; en una aplicación real podrías obtenerlos de SharedPreferences, una API, etc.
    private val currentUser = User(
        id = "user_123",
        username = "usuarioEjemplo",
        avatarUrl = "https://example.com/avatar.png" // Se agrega el valor para avatarUrl
    )

    override fun getCurrentUserId(): String = currentUser.id

    override fun getCurrentUser(): User = currentUser
}
