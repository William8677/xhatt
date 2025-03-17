package com.williamfq.data.repository

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.williamfq.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val sharedPreferences: SharedPreferences
) : UserRepository {

    override fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: sharedPreferences.getString(KEY_USER_ID, "default_user_id") ?: "default_user_id"
    }

    override suspend fun saveUserId(userId: String) {
        sharedPreferences.edit { putString(KEY_USER_ID, userId) }
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null || getCurrentUserId().isNotEmpty()
    }

    override suspend fun clearUserData() {
        sharedPreferences.edit { clear() }
        auth.signOut()
    }

    override suspend fun getCurrentUserName(): String {
        return auth.currentUser?.displayName ?: sharedPreferences.getString(KEY_USER_NAME, "Usuario Default") ?: "Usuario Default"
    }

    override suspend fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: sharedPreferences.getString(KEY_USER_EMAIL, "default@example.com") ?: "default@example.com"
    }

    override suspend fun getCurrentUserPhotoUrl(): String {
        return auth.currentUser?.photoUrl?.toString() ?: sharedPreferences.getString(KEY_USER_PHOTO_URL, "https://example.com/default-avatar.jpg") ?: "https://example.com/default-avatar.jpg"
    }

    override suspend fun getCurrentUserFollowersCount(): Int {
        return sharedPreferences.getInt("followers_count", 0)
    }

    override suspend fun getCurrentUserFollowingCount(): Int {
        return sharedPreferences.getInt("following_count", 0)
    }

    override suspend fun getCurrentUserPostsCount(): Int {
        return sharedPreferences.getInt("posts_count", 0)
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO_URL = "user_photo_url"
    }
}