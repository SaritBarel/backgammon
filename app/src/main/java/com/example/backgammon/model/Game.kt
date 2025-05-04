package com.example.backgammon.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * Represents an online Backgammon game
 */
data class Game(
    @DocumentId
    val id: String = "",
    
    @PropertyName("player1Id")
    val player1Id: String = "",
    
    @PropertyName("player1Name")
    val player1Name: String = "",
    
    @PropertyName("player2Id")
    val player2Id: String = "",
    
    @PropertyName("player2Name")
    val player2Name: String = "",
    
    @PropertyName("currentPlayerId")
    val currentPlayerId: String = "",
    
    @PropertyName("boardState")
    val boardState: Map<String, Any> = mapOf(),
    
    @PropertyName("moves")
    val moves: List<Move> = listOf(),
    
    @PropertyName("dice")
    val dice: List<Int> = listOf(),
    
    @PropertyName("status")
    val status: GameStatus = GameStatus.WAITING,
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now(),
    
    @PropertyName("winner")
    val winner: String = ""
) {
    fun isPlayerTurn(userId: String): Boolean {
        return currentPlayerId == userId
    }
    
    fun getOpponentId(userId: String): String {
        return if (userId == player1Id) player2Id else player1Id
    }
    
    fun getOpponentName(userId: String): String {
        return if (userId == player1Id) player2Name else player1Name
    }
    
    fun isActive(): Boolean {
        return status == GameStatus.ACTIVE
    }
    
    fun isWaiting(): Boolean {
        return status == GameStatus.WAITING
    }
    
    fun isFinished(): Boolean {
        return status == GameStatus.FINISHED
    }
}

enum class GameStatus {
    WAITING,    // Waiting for a second player
    ACTIVE,     // Game is in progress
    FINISHED    // Game has ended
} 