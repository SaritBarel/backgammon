package com.example.backgammon.ui.adapters

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.backgammon.R
import com.example.backgammon.databinding.ItemGameBinding
import com.example.backgammon.model.Game
import com.example.backgammon.model.GameStatus
import com.google.firebase.auth.FirebaseAuth

class GameAdapter(
    private val onJoinClick: (Game) -> Unit,
    private val onContinueClick: (Game) -> Unit
) : ListAdapter<Game, GameAdapter.GameViewHolder>(GameDiffCallback()) {

    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GameViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = getItem(position)
        holder.bind(game)
    }
    
    inner class GameViewHolder(private val binding: ItemGameBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(game: Game) {
            val currentUser = auth.currentUser
            val currentUserId = currentUser?.uid ?: ""
            
            // Set player names
            binding.txtPlayer1.text = game.player1Name
            binding.txtPlayer2.text = if (game.player2Id.isNotEmpty()) game.player2Name else "Waiting..."
            
            // Set game status
            binding.txtGameStatus.text = game.status.name
            val statusColor = when (game.status) {
                GameStatus.WAITING -> Color.parseColor("#FFC107") // Yellow
                GameStatus.ACTIVE -> Color.parseColor("#4CAF50") // Green
                GameStatus.FINISHED -> Color.parseColor("#9E9E9E") // Gray
            }
            binding.txtGameStatus.setBackgroundColor(statusColor)
            
            // Set turn indicator
            val isCurrentUserTurn = game.currentPlayerId == currentUserId
            binding.txtTurnIndicator.text = if (isCurrentUserTurn) "Your turn" else "Opponent's turn"
            binding.txtTurnIndicator.visibility = if (game.status == GameStatus.ACTIVE) View.VISIBLE else View.GONE
            binding.txtTurnIndicator.setTextColor(
                if (isCurrentUserTurn) 
                    Color.parseColor("#4CAF50") // Green 
                else 
                    Color.parseColor("#F44336") // Red
            )
            
            // Set last update time
            val updateTime = game.updatedAt.toDate().time
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                updateTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.txtLastUpdate.text = "Updated $timeAgo"
            
            // Show appropriate action button
            if (game.status == GameStatus.WAITING && game.player1Id != currentUserId) {
                // Show join button for waiting games created by other users
                binding.btnJoinGame.visibility = View.VISIBLE
                binding.btnContinueGame.visibility = View.GONE
                
                binding.btnJoinGame.setOnClickListener {
                    onJoinClick(game)
                }
            } else if (game.status == GameStatus.ACTIVE) {
                // Show continue button for active games
                binding.btnJoinGame.visibility = View.GONE
                binding.btnContinueGame.visibility = View.VISIBLE
                
                binding.btnContinueGame.setOnClickListener {
                    onContinueClick(game)
                }
            } else {
                // No action buttons for finished games or waiting games created by current user
                binding.btnJoinGame.visibility = View.GONE
                binding.btnContinueGame.visibility = View.GONE
            }
        }
    }
    
    class GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }
} 