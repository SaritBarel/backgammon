package com.example.backgammon.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Represents a move in a Backgammon game
 */
data class Move(
    @PropertyName("playerId")
    val playerId: String = "",
    
    @PropertyName("fromPosition")
    val fromPosition: Int = 0,
    
    @PropertyName("toPosition")
    val toPosition: Int = 0,
    
    @PropertyName("diceValue")
    val diceValue: Int = 0,
    
    @PropertyName("capturedPiece")
    val capturedPiece: Boolean = false,
    
    @PropertyName("timestamp")
    val timestamp: Timestamp = Timestamp.now()
) {
    fun getMoveDistance(): Int {
        return Math.abs(toPosition - fromPosition)
    }
    
    fun isValid(boardState: Map<String, Any>, dice: List<Int>): Boolean {
        // Basic validation - check if dice value is available
        if (!dice.contains(getMoveDistance())) {
            return false
        }
        
        // More validation logic would be implemented here
        return true
    }
} 