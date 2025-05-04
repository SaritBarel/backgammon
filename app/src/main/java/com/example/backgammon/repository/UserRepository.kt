package com.example.backgammon.repository

import android.util.Log
import com.example.backgammon.model.UserStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")
    private val tag = "UserRepository"
    
    /**
     * Creates or updates a user profile
     */
    suspend fun createOrUpdateUser() {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        // Check if user exists
        val userDoc = usersCollection.document(currentUser.uid).get().await()
        
        if (!userDoc.exists()) {
            // Create new user stats
            val userStats = UserStats(
                userId = currentUser.uid,
                displayName = currentUser.displayName ?: "Player",
                photoUrl = currentUser.photoUrl?.toString() ?: ""
            )
            
            usersCollection.document(currentUser.uid).set(userStats).await()
            Log.d(tag, "Created new user profile for ${currentUser.uid}")
        } else {
            // Update user display name and photo if needed
            val updates = mutableMapOf<String, Any>()
            
            currentUser.displayName?.let {
                updates["displayName"] = it
            }
            
            currentUser.photoUrl?.let {
                updates["photoUrl"] = it.toString()
            }
            
            if (updates.isNotEmpty()) {
                usersCollection.document(currentUser.uid).update(updates).await()
                Log.d(tag, "Updated user profile for ${currentUser.uid}")
            }
        }
    }
    
    /**
     * Gets the current user's stats
     */
    suspend fun getUserStats(): UserStats {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        val userDoc = usersCollection.document(currentUser.uid).get().await()
        
        return if (userDoc.exists()) {
            userDoc.toObject<UserStats>() ?: UserStats(userId = currentUser.uid)
        } else {
            // Create user profile if it doesn't exist
            createOrUpdateUser()
            UserStats(
                userId = currentUser.uid,
                displayName = currentUser.displayName ?: "Player",
                photoUrl = currentUser.photoUrl?.toString() ?: ""
            )
        }
    }
    
    /**
     * Gets another user's stats by their ID
     */
    suspend fun getUserStatsById(userId: String): UserStats {
        val userDoc = usersCollection.document(userId).get().await()
        
        return userDoc.toObject() ?: UserStats(userId = userId)
    }
    
    /**
     * Updates user stats after a game
     */
    suspend fun updateStatsAfterGame(won: Boolean, opponentId: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        // Get current stats
        val currentStats = getUserStats()
        
        // Get opponent stats
        val opponentStats = getUserStatsById(opponentId)
        
        // Calculate new stats
        val newStats = currentStats.afterGameResult(won, opponentStats.rating)
        
        // Update in Firestore
        usersCollection.document(currentUser.uid).set(newStats).await()
        
        Log.d(tag, "Updated stats for ${currentUser.uid} after game")
    }
    
    /**
     * Gets top players by rating
     */
    suspend fun getTopPlayers(limit: Int = 10): List<UserStats> {
        val topPlayers = usersCollection
            .orderBy("rating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        
        return topPlayers.documents.mapNotNull { it.toObject<UserStats>() }
    }
} 