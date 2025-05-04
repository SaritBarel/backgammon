package com.example.backgammon.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.backgammon.R
import com.example.backgammon.databinding.ActivityGameListBinding
import com.example.backgammon.model.Game
import com.example.backgammon.repository.GameRepository
import com.example.backgammon.repository.UserRepository
import com.example.backgammon.ui.adapters.GameAdapter
import kotlinx.coroutines.launch

class GameListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameListBinding
    private val gameRepository = GameRepository()
    private val userRepository = UserRepository()
    private lateinit var gameAdapter: GameAdapter
    private val tag = "GameListActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupRecyclerView()
        setupListeners()
        
        // Create user profile if it doesn't exist
        lifecycleScope.launch {
            try {
                userRepository.createOrUpdateUser()
            } catch (e: Exception) {
                Log.e(tag, "Error creating user profile: ${e.message}")
                Toast.makeText(this@GameListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        loadGames()
    }
    
    private fun setupRecyclerView() {
        gameAdapter = GameAdapter(
            onJoinClick = { game -> joinGame(game) },
            onContinueClick = { game -> continueGame(game) }
        )
        
        binding.recyclerGames.apply {
            adapter = gameAdapter
            layoutManager = LinearLayoutManager(this@GameListActivity)
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadGames()
        }
        
        binding.btnNewGame.setOnClickListener {
            createNewGame()
        }
        
        binding.btnFindGame.setOnClickListener {
            findGame()
        }
    }
    
    private fun loadGames() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val games = gameRepository.getUserGames()
                gameAdapter.submitList(games)
                
                // Show empty state if no games
                binding.txtEmptyState.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
                
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error loading games: ${e.message}")
                Toast.makeText(this@GameListActivity, "Error loading games: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }
    
    private fun createNewGame() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val gameId = gameRepository.createGame()
                Toast.makeText(this@GameListActivity, "Game created successfully", Toast.LENGTH_SHORT).show()
                
                // Refresh the list
                loadGames()
            } catch (e: Exception) {
                Log.e(tag, "Error creating game: ${e.message}")
                Toast.makeText(this@GameListActivity, "Error creating game: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }
    
    private fun findGame() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val gameId = gameRepository.findOrCreateGame()
                
                // Open the game screen
                startOnlineGame(gameId)
                
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error finding game: ${e.message}")
                Toast.makeText(this@GameListActivity, "Error finding game: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }
    
    private fun joinGame(game: Game) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                gameRepository.joinGame(game.id)
                
                // Open the game screen
                startOnlineGame(game.id)
                
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error joining game: ${e.message}")
                Toast.makeText(this@GameListActivity, "Error joining game: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }
    
    private fun continueGame(game: Game) {
        // Open the game screen
        startOnlineGame(game.id)
    }
    
    private fun startOnlineGame(gameId: String) {
        val intent = Intent(this, OnlineGameActivity::class.java).apply {
            putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId)
        }
        startActivity(intent)
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.swipeRefresh.isRefreshing = isLoading
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the list when returning to this screen
        loadGames()
    }
} 