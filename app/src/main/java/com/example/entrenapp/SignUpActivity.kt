package com.example.entrenapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Spinner
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import android.widget.ArrayAdapter

class SignUpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val usernameField = findViewById<EditText>(R.id.etUsername)
        val passwordField = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginLink = findViewById<TextView>(R.id.tvLoginLink)
        val spinnerGroups = findViewById<Spinner>(R.id.spinnerGroups)

        // Fetch groups from the server and populate the spinner
        val client = OkHttpClient()
        val request = Request.Builder().url("http://192.168.1.116:8000/grupos").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
            runOnUiThread {
                Toast.makeText(this@SignUpActivity, "Error al obtener los grupos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            response.body?.string()?.let { responseBody ->
                val groups = JSONObject(responseBody).getJSONArray("grupos")
                val groupList = mutableListOf<String>()
                for (i in 0 until groups.length()) {
                    groupList.add(groups.getJSONObject(i).getString("name"))
                }
                runOnUiThread {
                val adapter = ArrayAdapter(this@SignUpActivity, android.R.layout.simple_spinner_item, groupList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerGroups.adapter = adapter
                }
            }
            }
        })

        registerButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Connect to WebSocket server and send registration data
                val wsUrl = "ws://192.168.1.116:8080"
                val client = OkHttpClient()
                val request = Request.Builder().url(wsUrl).build()
                val webSocketListener =
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            var group = spinnerGroups.selectedItemPosition + 1
                            println("Group: $group")
                            val registerData =
                                "{\"username\":\"$username\", \"password\":\"$password\", \"type\":\"register\", \"id_group\":\"$group\"}"
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
