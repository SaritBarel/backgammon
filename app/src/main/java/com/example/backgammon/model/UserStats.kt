package com.example.backgammon.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Represents a user's game statistics
 */
data class UserStats(
    @DocumentId
    val userId: String = "",
    
    @PropertyName("displayName")
    val displayName: String = "",
    
    @PropertyName("gamesPlayed")
    val gamesPlayed: Int = 0,
    
    @PropertyName("gamesWon")
    val gamesWon: Int = 0,
    
    @PropertyName("gamesLost")
    val gamesLost: Int = 0,
    
    @PropertyName("rating")
    val rating: Int = 1200,
    
    @PropertyName("winStreak")
    val winStreak: Int = 0,
    
    @PropertyName("bestWinStreak")
    val bestWinStreak: Int = 0,
    
    @PropertyName("photoUrl")
    val photoUrl: String = ""
) {
    fun getWinRate(): Float {
        return if (gamesPlayed > 0) {
            gamesWon.toFloat() / gamesPlayed.toFloat() * 100
        } else {
            0f
        }
    }
    
    /**
     * Calculate updated stats after a game
     */
    fun afterGameResult(won: Boolean, opponentRating: Int): UserStats {
        val newGamesPlayed = gamesPlayed + 1
        val newGamesWon = if (won) gamesWon + 1 else gamesWon
        val newGamesLost = if (!won) gamesLost + 1 else gamesLost
        
        // ELO rating calculation (simplified)
        val expectedOutcome = 1f / (1f + Math.pow(10.0, ((opponentRating - rating) / 400.0).toDouble()).toFloat())
        val actualOutcome = if (won) 1f else 0f
        val ratingChange = (32 * (actualOutcome - expectedOutcome)).toInt()
        val newRating = rating + ratingChange
        
        // Update win streak
        val newWinStreak = if (won) winStreak + 1 else 0
        val newBestWinStreak = Math.max(bestWinStreak, newWinStreak)
        
        return this.copy(
            gamesPlayed = newGamesPlayed,
            gamesWon = newGamesWon,
            gamesLost = newGamesLost,
            rating = newRating,
            winStreak = newWinStreak,
            bestWinStreak = newBestWinStreak
        )
    }
} 