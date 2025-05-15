package com.example.backgammon

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
        
        // Find Play button
        val playButton = findViewById<Button>(R.id.btnPlay)
        playButton.setOnClickListener {
            // Open the games list activity
            startActivity(Intent(this, GamesListActivity::class.java))
        }
        
        // Find Sign Out button
        val signOutButton = findViewById<Button>(R.id.btnSignOut)
        signOutButton.setOnClickListener {
            // Sign out the user and return to login screen
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}