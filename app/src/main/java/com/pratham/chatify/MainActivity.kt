package com.pratham.chatify

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var pinEditText: EditText  // PIN input field

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        pinEditText = findViewById(R.id.pinEditText)

        findViewById<ExtendedFloatingActionButton>(R.id.findMe).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val githubUrl = "https://github.com/pratham-developer" // Replace with your GitHub URL
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(githubUrl))
        }

        try {
            auth = FirebaseAuth.getInstance()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val currentUser = auth.currentUser
            if (currentUser != null) {
                startHomeActivity(currentUser.email, currentUser.displayName)
            }
        } catch (e: Exception) {
            Log.e("Main", "exception: ", e)
        }

        findViewById<Button>(R.id.login_button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val enteredPin = pinEditText.text.toString()

            // Call the backend to verify the PIN
            verifyPin(enteredPin)
        }
    }

    private fun verifyPin(enteredPin: String) {
        val apiService = RetrofitInstance.api
        val pinRequest = PinRequest(pin = enteredPin)

        apiService.verifyPin(pinRequest).enqueue(object : Callback<PinResponse> {
            override fun onResponse(call: Call<PinResponse>, response: Response<PinResponse>) {
                if (response.isSuccessful && response.body()?.valid == true) {
                    signInGoogle() // PIN is correct, proceed with Google Sign-In
                } else {
                    Toast.makeText(this@MainActivity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PinResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error verifying PIN", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "PIN verification error: ${t.message}")
            }
        })
    }

    private fun signInGoogle() {
        try {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e: ApiException) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    startHomeActivity(account.email, account.displayName)
                }
            }
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private fun startHomeActivity(email: String?, displayName: String?) {
        try {
            val intent = Intent(this, MainActivity2::class.java).apply {
                putExtra("USER_NAME", email)
                putExtra("USER_EMAIL", displayName)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }
}
