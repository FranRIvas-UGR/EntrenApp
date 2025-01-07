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


class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameField = findViewById<EditText>(R.id.etUsername)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signupLink = findViewById<TextView>(R.id.tvSignup)
        var hayJson = false

        if (getExternalFilesDir(null)?.resolve("login.json")?.exists() == true) {
            val file = getExternalFilesDir(null)?.resolve("login.json")
            val json = file?.readText()
            val jsonObject = JSONObject(json)
            val savedUsername = jsonObject.getString("username")
            val savedPassword = jsonObject.getString("password")
            usernameField.setText(savedUsername)
            passwordField.setText(savedPassword)
            hayJson = true
        }

        loginButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Connect to WebSocket server and send login credentials
                val wsUrl = "ws://192.168.1.116:8080"
                val client = OkHttpClient()
                val request = Request.Builder().url(wsUrl).build()
                val webSocketListener =
                    object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val loginData = "{\"username\":\"$username\", \"password\":\"$password\", \"type\":\"login\"}"
                        webSocket.send(loginData)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runOnUiThread {
                        val jsonResponse = JSONObject(text)
                        val message = jsonResponse.getString("message")
                        val success = jsonResponse.getBoolean("success")
                        if (success) {
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                            var id_user = ""
                            if (jsonResponse.has("id_user")) {
                                id_user = jsonResponse.getString("id_user")
                            }
                            // Guardamos los datos en un JSON
                            if (!hayJson) {
                                val file = getExternalFilesDir(null)?.resolve("login.json")
                                val json = JSONObject()
                                json.put("username", username)
                                json.put("password", password)
                                json.put("id_user", id_user)
                                file?.writeText(json.toString())
                            }               
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("id_user", id_user)
                            startActivity(intent)
                            finish()
                        }
                        else {
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
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
                            this@LoginActivity,
                            "Error al conectar al servidor: ${t.message}",
                            Toast.LENGTH_SHORT
                            )
                            .show()
                        }
                    }
                    }
                client.newWebSocket(request, webSocketListener)
                client.dispatcher.executorService.shutdown()
            }
        }

        signupLink.setOnClickListener {
            Toast.makeText(this, "Página de registro", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
