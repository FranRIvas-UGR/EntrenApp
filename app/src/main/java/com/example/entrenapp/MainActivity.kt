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
import android.widget.LinearLayout
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

        val intent = intent
        var id_user = ""
        if (intent.hasExtra("id_user")) {
           id_user = intent.getStringExtra("id_user").toString()
           setGrupo(id_user)
        }

        // Inicializar el RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        adapter = ActivityAdapter(activities["today"] ?: mutableListOf(), id_user)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Inicializar el calendario
        calendarView = findViewById(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
        val selectedDate = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", dayOfMonth)}"
        
        if (activities.containsKey(selectedDate)) {
            adapter.updateActivities(activities[selectedDate] ?: mutableListOf())
            findViewById<Button>(R.id.addButton).isEnabled = true
            findViewById<Button>(R.id.addButton).visibility = View.VISIBLE
            findViewById<Button>(R.id.clearButton).visibility = View.VISIBLE
        } else {
            getActividadesServer(selectedDate, id_user)
            findViewById<Button>(R.id.addButton).isEnabled = false
            findViewById<Button>(R.id.addButton).visibility = View.INVISIBLE
            findViewById<Button>(R.id.clearButton).visibility = View.INVISIBLE
        }
    }


        

        // Botón para agregar actividad
        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            val today = getTodayDate().let { "${it.first}-${it.second}-${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}" }
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("date", today)
            intent.putExtra("id_user", id_user)
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
        getActividadesServer(today_string, id_user)

        // Conexión WebSocket para recibir actualizaciones de actividades
        connectToWebSocket()
    }

    // Método para obtener actividades desde el servidor
    private fun getActividadesServer(date: String = "", id_user: String = "") {
        val request = Request.Builder().url("http://192.168.1.116:8000/actividades/usuario?id_user=$id_user").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val json = response.body?.string()
                val jsonArray = JSONObject(json).getJSONArray("activities")

                // Limpiar actividades existentes para la fecha
                if (!activities.containsKey(date)) {
                    activities[date] = mutableListOf()
                } else {
                    activities[date]?.clear()
                }

                for (i in 0 until jsonArray.length()) {
                    val activity = jsonArray.getJSONObject(i).getString("activity")
                    val duration = jsonArray.getJSONObject(i).getString("duration")
                    val activityDate = jsonArray.getJSONObject(i).getString("date")
                    val activityUser = jsonArray.getJSONObject(i).getString("id_user")
                    var activityUserName = jsonArray.getJSONObject(i).getString("username")

                    if (activityDate == date) {
                        activities[activityDate]?.add("$activity - $duration min -> Usuario $activityUser: $activityUserName")
                    }
                }

                println("Lista de actividades: $activities")
                runOnUiThread {
                    adapter.updateActivities(activities[date] ?: mutableListOf())
                }
            }
        })
    }


    // Método para borrar actividades desde el servidor
    private fun borrarActividades(date: String, id_user: String = "") {
        val request = Request.Builder().url("http://192.168.1.116:8000/borrar_actividades?date=$date&&id_user=$id_user").build()
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
        val request = Request.Builder().url("ws://192.168.1.116:8080").build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected to WebSocket", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setGrupo(id_user: String?) {
        println("ID_USER: $id_user")
        val request = Request.Builder().url("http://192.168.1.116:8000/get_grupo?id_user=$id_user").build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val json = response.body?.string()
                println("JSON: $json")
                val grupo = JSONObject(json).getJSONObject("grupo").getString("name")
                runOnUiThread {
                    findViewById<TextView>(R.id.userGroupTextView).text = grupo
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

class ActivityAdapter(private var activities: List<String>, private val id_user: String) : RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    // ViewHolder que hace referencia al TextView dentro de item_activity.xml
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textViewActivity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflar el layout de item_activity
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return ViewHolder(itemView)
    }

    fun updateActivities(newActivities: List<String>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = activities[position]
        val container = holder.itemView.findViewById<LinearLayout>(R.id.linearLayoutContainer)
        println("ACTIVIDAD: ${activities[position]}")
        if (activities[position].contains("Fran")) {
            container.setBackgroundColor(holder.itemView.context.getColor(R.color.holo_green_light))
        } else {
            container.setBackgroundColor(holder.itemView.context.getColor(R.color.holo_blue_light))
        }
    }

    override fun getItemCount() = activities.size
}
