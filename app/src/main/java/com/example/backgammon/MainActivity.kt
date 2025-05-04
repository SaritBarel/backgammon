package com.example.backgammon

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.backgammon.ui.GameListActivity

class MainActivity : AppCompatActivity() {
    private lateinit var backgammonBoardView: BackgammonBoardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize game board
        backgammonBoardView = findViewById(R.id.backgammonBoard)
        
        // Connect BACK button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            backgammonBoardView.undoLastMove()
        }
        
        // Connect DONE button
        val doneButton = findViewById<ImageButton>(R.id.doneButton)
        doneButton.setOnClickListener {
            backgammonBoardView.finishTurn()
        }
        
        // Connect ONLINE button
        val onlineButton = findViewById<Button>(R.id.btnOnline)
        onlineButton?.setOnClickListener {
            startActivity(Intent(this, GameListActivity::class.java))
        }
    }
}