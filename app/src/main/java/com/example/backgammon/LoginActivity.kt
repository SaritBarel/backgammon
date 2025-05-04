package com.example.backgammon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.backgammon.databinding.ActivityLoginBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Firebase Auth
        try {
            auth = Firebase.auth
            Log.d(TAG, "Firebase Auth initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Auth: ${e.message}")
            Toast.makeText(this, "Failed to initialize Firebase: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Set up ViewPager adapter
        val viewPagerAdapter = AuthViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        
        // Connect TabLayout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.login)
                1 -> getString(R.string.register)
                else -> null
            }
        }.attach()
    }
    
    override fun onStart() {
        super.onStart()
        
        try {
            // Check if user is already signed in
            val currentUser = auth.currentUser
            Log.d(TAG, "Current user: ${currentUser?.email}")
            
            if (currentUser != null && false) { // Temporarily disable auto-redirect
                // If user is already signed in, redirect to main screen
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking current user: ${e.message}")
        }
    }
} 