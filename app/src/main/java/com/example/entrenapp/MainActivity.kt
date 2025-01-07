package com.example.entrenapp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import android.content.Intent
import org.json.JSONObject
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import androidx.core.util.Pair
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityAdapter
    private val activities = mutableMapOf<String, MutableList<String>>()  // Guardar actividades por fecha
    private lateinit var calendarView: CalendarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar el RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        adapter = ActivityAdapter(activities["today"] ?: mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Inicializar el calendario
        calendarView = findViewById(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Obtener las actividades para la fecha seleccionada
            val selectedDate = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", dayOfMonth)}"
            val activitiesForDate = activities[selectedDate] ?: mutableListOf()
            adapter.updateActivities(activitiesForDate)

            // Si el día seleccionado no es hoy, deshabilitar agregar actividad
            val today = getTodayDate()
            val selectedDay = Triple(year, month + 1, dayOfMonth)
            //Toast.makeText(this, "TargetDay: $selectedDay", Toast.LENGTH_SHORT).show()
            if (selectedDay != today) {
                // Aquí puedes deshabilitar el botón para agregar actividad si no es hoy
                findViewById<Button>(R.id.addButton).isEnabled = false
                findViewById<Button>(R.id.addButton).visibility = View.GONE
                findViewById<Button>(R.id.clearButton).visibility = View.GONE
            } else {
                findViewById<Button>(R.id.addButton).isEnabled = true
                findViewById<Button>(R.id.addButton).visibility = View.VISIBLE
                findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE
            }
            getActividadesServer(selectedDate)
        }

        // Botón para agregar actividad
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val today = getTodayDate().let { "${it.first}-${it.second}-${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}" }
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }

        // Botón para limpiar actividades
        val clearButton: Button = findViewById(R.id.clearButton)
        clearButton.setOnClickListener {
            val selectedDate = calendarView.date.toString()
            activities[selectedDate]?.clear()
            adapter.updateActivities(mutableListOf())
            borrarActividades(selectedDate)
        }

        // Llenar actividades al iniciar
        var today_string = getTodayDate().let {
            "${it.first}-${String.format("%02d", it.second)}-${String.format("%02d", it.third)}"
        }
        getActividadesServer(today_string)

        // Conexión WebSocket para recibir actualizaciones de actividades
        connectToWebSocket()
    }

    // Método para obtener actividades desde el servidor
    private fun getActividadesServer(date: String = "") {
        val request = Request.Builder().url("http://10.0.2.2:8000/actividades").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val json = response.body?.string()
                val jsonArray = JSONObject(json).getJSONArray("activities").apply {
                    for (i in 0 until length()) {
                        val activity = getJSONObject(i).getString("activity")
                        val activityDate = getJSONObject(i).getString("date")
                        if (!activities.containsKey(activityDate)) {
                            activities[activityDate] = mutableListOf()
                        }
                        else if (activities.contains(activityDate) && activities[activityDate]?.contains(activity) == true) {
                            activities[activityDate]?.remove(activity)
                        }
                        if (activityDate == date) {
                            activities[activityDate]?.add(activity)
                        }
                    }
                }
                println("Lista de actividades: $activities")
                runOnUiThread {
                    val today = getTodayDate().let {
                        "${it.first}-${String.format("%02d", it.second)}-${String.format("%02d", it.third)}"
                    }
                    Toast.makeText(this@MainActivity, "Today: $today", Toast.LENGTH_SHORT).show()
                    adapter.updateActivities(activities[today] ?: mutableListOf())
                }
            }
        })
    }

    // Método para borrar actividades desde el servidor
    private fun borrarActividades(date: String) {
        val request = Request.Builder().url("http://10.0.2.2:8000/borrar_actividades?date=$date").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Actividades eliminadas", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun connectToWebSocket() {
        val request = Request.Builder().url("ws://10.0.2.2:8080").build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected to WebSocket", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // Asegúrate de tener un cliente de OkHttp
    private val client = OkHttpClient()
    private fun getTodayDate(): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance()
        return Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    }
}

class ActivityAdapter(private var activities: List<String>) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    // ViewHolder que hace referencia al TextView dentro de item_activity.xml
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textViewActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflar el layout de item_activity
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Asignar el texto correspondiente al TextView
        holder.textView.text = activities[position]
    }

    fun updateActivities(newActivities: List<String>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    override fun getItemCount() = activities.size
}
