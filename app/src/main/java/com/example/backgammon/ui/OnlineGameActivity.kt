package com.example.backgammon.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.backgammon.BackgammonBoardView
import com.example.backgammon.R
import com.example.backgammon.databinding.ActivityOnlineGameBinding
import com.example.backgammon.model.Game
import com.example.backgammon.model.GameStatus
import com.example.backgammon.model.Move
import com.example.backgammon.repository.GameRepository
import com.example.backgammon.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OnlineGameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnlineGameBinding
    private val gameRepository = GameRepository()
    private val userRepository = UserRepository()
    private lateinit var backgammonBoardView: BackgammonBoardView
    private val auth = FirebaseAuth.getInstance()
    
    private var gameId: String = ""
    private var currentGame: Game? = null
    private val tag = "OnlineGameActivity"
    
    companion object {
        const val EXTRA_GAME_ID = "extra_game_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Get game ID from intent
        gameId = intent.getStringExtra(EXTRA_GAME_ID) ?: ""
        if (gameId.isEmpty()) {
            Toast.makeText(this, "Error: Game ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize board view
        backgammonBoardView = findViewById(R.id.backgammonBoard)
        
        setupListeners()
        loadGame()
    }
    
    private fun setupListeners() {
        binding.btnMakeMove.setOnClickListener {
            makeMove()
        }
        
        binding.btnResign.setOnClickListener {
            resignGame()
        }
    }
    
    private fun loadGame() {
        showLoading(true)
        
        // Load initial game state
        lifecycleScope.launch {
            try {
                val game = gameRepository.getGame(gameId)
                updateGameUI(game)
                
                // Start listening for real-time updates
                gameRepository.listenToGame(gameId) { updatedGame ->
                    updateGameUI(updatedGame)
                }
                
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error loading game: ${e.message}")
                Toast.makeText(this@OnlineGameActivity, "Error loading game: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
                finish()
            }
        }
    }
    
    private fun updateGameUI(game: Game) {
        currentGame = game
        
        // Update player names
        binding.txtPlayer1.text = game.player1Name
        binding.txtPlayer2.text = game.player2Name
        
        // Update game status
        binding.txtGameStatus.text = game.status.name
        
        // Update turn indicator
        val currentUserId = auth.currentUser?.uid ?: ""
        val isMyTurn = game.currentPlayerId == currentUserId
        
        binding.txtTurnIndicator.text = if (isMyTurn) "Your turn" else "Opponent's turn"
        binding.txtTurnIndicator.setTextColor(
            if (isMyTurn) getColor(R.color.green) else getColor(R.color.red)
        )
        
        // Update dice
        val diceText = game.dice.joinToString(" and ") { it.toString() }
        binding.txtDice.text = "Dice: $diceText"
        
        // Enable/disable move button based on turn
        binding.btnMakeMove.isEnabled = isMyTurn
        
        // TODO: Update board state based on game.boardState
        // This would require integration with your BackgammonBoardView
        // backgammonBoardView.updateBoardState(game.boardState)
    }
    
    private fun makeMove() {
        // TODO: Implement move logic
        // This should get the current board state and create a Move object
        // Then update the game in Firebase
        
        // Example (placeholder):
        /*
        val currentUserId = auth.currentUser?.uid ?: return
        val move = Move(
            playerId = currentUserId,
            fromPosition = 1,
            toPosition = 3,
            diceValue = 2
        )
        
        lifecycleScope.launch {
            try {
                gameRepository.makeMove(gameId, move)
            } catch (e: Exception) {
                Toast.makeText(this@OnlineGameActivity, "Error making move: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        */
    }
    
    private fun resignGame() {
        // TODO: Implement resign logic
        // This should update the game status to FINISHED and set the winner as the opponent
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop listening to game updates
        gameRepository.stopListeningToGame()
    }
} 