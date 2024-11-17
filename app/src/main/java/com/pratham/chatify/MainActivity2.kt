package com.pratham.chatify

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pratham.chatify.MessageAdapter
import com.pratham.chatify.Message
import com.google.firebase.auth.FirebaseAuth
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity2 : AppCompatActivity() {
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var messagesAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private lateinit var authToken: String
    private lateinit var socket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        enableEdgeToEdge()

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Use lifecycleScope to launch coroutine for getting Firebase ID token
        lifecycleScope.launch {
            val token = getFirebaseIdToken()
            if (token != null) {
                authToken = token
                connectSocket()
            } else {
                Log.e("MainActivity2", "Failed to get Firebase token")
            }
        }

        // Initialize UI components
        messagesRecyclerView = findViewById(R.id.messages_recycler_view)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_button)

        // Set up RecyclerView with standard layout (oldest at top)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true  // Always scroll to bottom on new messages
        messagesRecyclerView.layoutManager = layoutManager
        messagesAdapter = MessageAdapter(messageList, FirebaseAuth.getInstance().currentUser?.uid ?: "")
        messagesRecyclerView.adapter = messagesAdapter

        // Send message on button click
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                currentUser?.let {
                    val senderId = it.uid
                    sendMessage(messageText, senderId, it.displayName ?: "Anonymous")
                }
                messageEditText.text.clear()
            }
        }

        setupSidebarButton()
    }

    private suspend fun getFirebaseIdToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                Log.e("MainActivity2", "Error getting Firebase ID token", e)
                null
            }
        }
    }

    private fun sendMessage(messageText: String, senderId: String, senderName: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        socket.emit("newMessage", JSONObject().apply {
            put("text", messageText)
            put("senderName", senderName)
            put("timestamp", timestamp)
        })
    }

    private fun connectSocket() {
        try {
            val opts = IO.Options().apply {
                auth = mapOf("token" to authToken)
            }

            socket = IO.socket("https://chatify-backend-production-5d29.up.railway.app", opts)
            socket.connect()

            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("Socket.IO", "Connection Error: ${args[0]}")
            }

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("Socket.IO", "Connected")
                socket.emit("fetchMessages")
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("Socket.IO", "Disconnected")
            }

            socket.on("message") { args ->
                val messageJson = args[0] as JSONObject
                val message = Message(
                    text = messageJson.getString("text"),
                    senderId = messageJson.getString("senderId"),
                    senderName = messageJson.getString("senderName"),
                    timestamp = messageJson.getString("timestamp")
                )

                runOnUiThread {
                    // Add new message at the end (bottom)
                    messageList.add(message)
                    messagesAdapter.notifyItemInserted(messageList.size - 1)
                    // Scroll to the newest message
                    messagesRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }

            socket.on("previousMessages") { args ->
                val messagesJsonArray = args[0] as JSONArray
                val messages = mutableListOf<Message>()

                for (i in 0 until messagesJsonArray.length()) {
                    val messageJson = messagesJsonArray.getJSONObject(i)
                    val message = Message(
                        text = messageJson.getString("text"),
                        senderId = messageJson.getString("senderId"),
                        senderName = messageJson.getString("senderName"),
                        timestamp = messageJson.getString("timestamp")
                    )
                    messages.add(message)
                }

                runOnUiThread {
                    messageList.clear()
                    // Reverse the order to show oldest messages at top
                    messageList.addAll(messages.reversed())
                    messagesAdapter.notifyDataSetChanged()
                    // Scroll to bottom to show newest messages
                    messagesRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }

        } catch (e: Exception) {
            Log.e("MainActivity2", "Socket connection error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized) {
            socket.disconnect()
        }
    }

    private fun setupSidebarButton() {
        findViewById<ImageView>(R.id.sidebarButton).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showSignOutConfirmationDialog()
        }
    }

    private fun showSignOutConfirmationDialog() {
        val title = SpannableString("Sign Out").apply {
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this@MainActivity2, R.color.primary)),
                0, length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        MaterialAlertDialogBuilder(this, R.style.CustomRoundedDialogTheme)
            .setTitle(title)
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                performSignOut()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
            .apply {
                findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.tertiary))
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.primary))
            }
    }

    private fun performSignOut() {
        try {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("mainactivity2", "exception: ", e)
        }
    }
}