package com.example.entrenapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class SignUpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val usernameField = findViewById<EditText>(R.id.etUsername)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginLink = findViewById<TextView>(R.id.tvLoginLink)

        registerButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Connect to WebSocket server and send registration data
                val wsUrl = "ws://10.0.2.2:8080"
                val client = OkHttpClient()
                val request = Request.Builder().url(wsUrl).build()
                val webSocketListener =
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            val registerData =
                                "{\"username\":\"$username\", \"password\":\"$password\", \"type\":\"register\"}"
                            webSocket.send(registerData)
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            runOnUiThread {
                                val jsonResponse = JSONObject(text)
                                val message = jsonResponse.getString("message")
                                val success = jsonResponse.getBoolean("success")
                                if (success) {
                                    Toast.makeText(
                                        this@SignUpActivity,
                                        message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?
                        ) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@SignUpActivity,
                                    "Error al conectar al servidor: ${t.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                client.newWebSocket(request, webSocketListener)
                client.dispatcher.executorService.shutdown()
            }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
