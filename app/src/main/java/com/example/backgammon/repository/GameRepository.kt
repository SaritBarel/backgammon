package com.example.backgammon.repository

import android.util.Log
import com.example.backgammon.model.Game
import com.example.backgammon.model.GameStatus
import com.example.backgammon.model.Move
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gamesCollection = db.collection("games")
    private val tag = "GameRepository"
    
    // For game state listeners
    private var gameListener: ListenerRegistration? = null
    
    /**
     * Creates a new game and returns the game ID
     */
    suspend fun createGame(): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        // Initialize empty board state - this will depend on your specific game rules
        val initialBoardState = createInitialBoardState()
        
        val newGame = Game(
            player1Id = currentUser.uid,
            player1Name = currentUser.displayName ?: "Player 1",
            currentPlayerId = currentUser.uid,
            status = GameStatus.WAITING,
            boardState = initialBoardState,
            dice = rollDice()
        )
        
        val gameRef = gamesCollection.document()
        gameRef.set(newGame).await()
        
        Log.d(tag, "Created new game with ID: ${gameRef.id}")
        return gameRef.id
    }
    
    /**
     * Creates initial board state for a backgammon game
     */
    private fun createInitialBoardState(): Map<String, Any> {
        val boardState = mutableMapOf<String, Any>()
        
        // סידור כלים לפי חוקי שש-בש
        boardState["position_1"] = 2  // שתי אבנים לבנות בעמדה 1
        boardState["position_12"] = 5 // 5 אבנים לבנות בעמדה 12
        boardState["position_17"] = 3 // 3 אבנים לבנות בעמדה 17
        boardState["position_19"] = 5 // 5 אבנים לבנות בעמדה 19
        
        boardState["position_6"] = -5  // 5 אבנים שחורות בעמדה 6
        boardState["position_8"] = -3  // 3 אבנים שחורות בעמדה 8
        boardState["position_13"] = -5 // 5 אבנים שחורות בעמדה 13
        boardState["position_24"] = -2 // 2 אבנים שחורות בעמדה 24
        
        // אזורי הבית והחוץ
        boardState["position_home_white"] = 0
        boardState["position_home_black"] = 0
        boardState["position_outside_white"] = 0
        boardState["position_outside_black"] = 0
        
        return boardState
    }
    
    /**
     * Finds an available game or creates a new one
     */
    suspend fun findOrCreateGame(): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        // Try to find a waiting game
        val waitingGames = gamesCollection
            .whereEqualTo("status", GameStatus.WAITING.name)
            .whereNotEqualTo("player1Id", currentUser.uid) // Not own game
            .limit(1)
            .get()
            .await()
        
        if (!waitingGames.isEmpty) {
            // Join existing game
            val gameId = waitingGames.documents[0].id
            joinGame(gameId)
            return gameId
        } else {
            // Create new game
            return createGame()
        }
    }
    
    /**
     * Joins an existing game
     */
    suspend fun joinGame(gameId: String): Game {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        val gameDoc = gamesCollection.document(gameId).get().await()
        val game = gameDoc.toObject<Game>() ?: throw Exception("Game not found")
        
        if (game.status != GameStatus.WAITING) {
            throw Exception("Game is not available for joining")
        }
        
        // Join the game
        gamesCollection.document(gameId).update(
            mapOf(
                "player2Id" to currentUser.uid,
                "player2Name" to (currentUser.displayName ?: "Player 2"),
                "status" to GameStatus.ACTIVE.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        
        Log.d(tag, "Joined game with ID: $gameId")
        return gamesCollection.document(gameId).get().await().toObject()!!
    }
    
    /**
     * Gets a game by ID
     */
    suspend fun getGame(gameId: String): Game {
        val gameDoc = gamesCollection.document(gameId).get().await()
        return gameDoc.toObject() ?: throw Exception("Game not found")
    }
    
    /**
     * Listens for changes to a specific game
     */
    fun listenToGame(gameId: String, onUpdate: (Game) -> Unit) {
        // Remove any existing listener
        gameListener?.remove()
        
        // Add new listener
        gameListener = gamesCollection.document(gameId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(tag, "Error listening to game changes", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val game = snapshot.toObject<Game>()
                game?.let { onUpdate(it) }
            }
        }
    }
    
    /**
     * Stops listening to game changes
     */
    fun stopListeningToGame() {
        gameListener?.remove()
        gameListener = null
    }
    
    /**
     * Makes a move in the game
     */
    suspend fun makeMove(gameId: String, move: Move) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        
        // Get current game state
        val game = getGame(gameId)
        
        if (game.currentPlayerId != currentUser.uid) {
            throw Exception("Not your turn")
        }
        
        if (game.status != GameStatus.ACTIVE) {
            throw Exception("Game is not active")
        }
        
        // Validate move (simplified)
        if (!move.isValid(game.boardState, game.dice)) {
            throw Exception("Invalid move")
        }
        
        // Calculate new board state
        val newBoardState = calculateNewBoardState(game.boardState, move)
        
        // Determine next player
        val nextPlayerId = if (currentUser.uid == game.player1Id) game.player2Id else game.player1Id
        
        // Roll new dice
        val newDice = rollDice()
        
        // Update game
        gamesCollection.document(gameId).update(
            mapOf(
                "currentPlayerId" to nextPlayerId,
                "boardState" to newBoardState,
                "moves" to FieldValue.arrayUnion(move),
                "dice" to newDice,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        
        // Check if game ended
        if (isGameOver(newBoardState, currentUser.uid)) {
            gamesCollection.document(gameId).update(
                mapOf(
                    "status" to GameStatus.FINISHED.name,
                    "winner" to currentUser.uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
    
    /**
     * Gets a list of active games for the current user
     */
    suspend fun getUserGames(): List<Game> {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        Log.d(tag, "Getting games for user: ${currentUser.uid}")
        
        // אין שדה players במודל שלנו, אז נבצע חיפוש לפי player1Id או player2Id ישירות
        val player1Games = gamesCollection
            .whereEqualTo("player1Id", currentUser.uid)
            .whereIn("status", listOf(GameStatus.WAITING.name, GameStatus.ACTIVE.name))
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        Log.d(tag, "Found ${player1Games.size()} games where user is player1")
        
        val player2Games = gamesCollection
            .whereEqualTo("player2Id", currentUser.uid)
            .whereIn("status", listOf(GameStatus.WAITING.name, GameStatus.ACTIVE.name))
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        Log.d(tag, "Found ${player2Games.size()} games where user is player2")
        
        // מיזוג התוצאות
        val allGames = mutableListOf<Game>()
        allGames.addAll(player1Games.documents.mapNotNull { it.toObject<Game>() })
        allGames.addAll(player2Games.documents.mapNotNull { it.toObject<Game>() })
        
        Log.d(tag, "Total games after merging: ${allGames.size}")
        
        // מיון לפי זמן עדכון במידה ויש תוצאות
        return allGames.sortedByDescending { it.updatedAt.seconds }
    }
    
    // Helper functions
    
    private fun rollDice(): List<Int> {
        return listOf(Random.nextInt(1, 7), Random.nextInt(1, 7))
    }
    
    private fun calculateNewBoardState(currentState: Map<String, Any>, move: Move): Map<String, Any> {
        // This would contain the logic to update the board based on a move
        // For example, moving pieces, capturing pieces, etc.
        // This is a placeholder implementation
        
        val newState = currentState.toMutableMap()
        
        // Update from position (remove piece)
        val fromKey = "position_${move.fromPosition}"
        val currentPieces = (currentState[fromKey] as? Long)?.toInt() ?: 0
        if (currentPieces > 0) {
            newState[fromKey] = currentPieces - 1
        }
        
        // Update to position (add piece)
        val toKey = "position_${move.toPosition}"
        val toPieces = (currentState[toKey] as? Long)?.toInt() ?: 0
        newState[toKey] = toPieces + 1
        
        return newState
    }
    
    private fun isGameOver(boardState: Map<String, Any>, playerId: String): Boolean {
        // Check if player has moved all pieces to their home
        // This is a simplified implementation
        
        // For example, checking if player has no pieces left on the board
        var totalPieces = 0
        for (key in boardState.keys) {
            if (key.startsWith("position_") && key != "position_home_$playerId") {
                val pieces = (boardState[key] as? Long)?.toInt() ?: 0
                totalPieces += pieces
            }
        }
        
        return totalPieces == 0
    }
} 