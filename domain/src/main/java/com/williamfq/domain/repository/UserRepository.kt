
package com.williamfq.domain.repository

interface UserRepository {

    fun getCurrentUserId(): String
    suspend fun saveUserId(userId: String)
    fun isUserLoggedIn(): Boolean
    suspend fun clearUserData()
    suspend fun getCurrentUserName(): String
    suspend fun getCurrentUserEmail(): String
    suspend fun getCurrentUserPhotoUrl(): String
    suspend fun getCurrentUserFollowersCount(): Int
    suspend fun getCurrentUserFollowingCount(): Int
    suspend fun getCurrentUserPostsCount(): Int
    
}
