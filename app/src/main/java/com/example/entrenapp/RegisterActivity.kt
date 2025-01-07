package com.example.entrenapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.content.Intent

class RegisterActivity : Activity() {
    private lateinit var webSocket: okhttp3.WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val client = OkHttpClient()
        val request = Request.Builder().url("ws://10.0.2.2:8080").build()
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Conectado al servidor", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Error al conectar al servidor: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val activityNameEditText: EditText = findViewById(R.id.etActivityName)
        val durationEditText: EditText = findViewById(R.id.etDuration)
        val registerButton: Button = findViewById(R.id.btnSave)
        registerButton.setOnClickListener {
            val activityName = activityNameEditText.text.toString()
            val duration = durationEditText.text.toString().toInt()
            sendActivityToServer(activityName, duration)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("newActivity", activityName)
            startActivity(intent)
        }
    }

    private fun sendActivityToServer(activityName: String, duration: Int) {
        val json = JSONObject()
        json.put("activity", activityName)
        json.put("duration", duration)
        webSocket.send(json.toString())
        Toast.makeText(this, "Actividad enviada", Toast.LENGTH_SHORT).show()
    }
}
