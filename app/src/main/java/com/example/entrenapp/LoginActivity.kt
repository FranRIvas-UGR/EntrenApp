package com.example.entrenapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.etEmail)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupLink = findViewById<TextView>(R.id.tvSignup)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            } else if (email == "test@example.com" && password == "123456") {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                // Navigate to the next screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }

        signupLink.setOnClickListener {
            Toast.makeText(this, "Navigate to Signup Screen", Toast.LENGTH_SHORT).show()
            // Here you could navigate to a signup activity
            // val intent = Intent(this, SignupActivity::class.java)
            // startActivity(intent)
        }
    }
}
