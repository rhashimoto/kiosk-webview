package com.shoestringresearch.kiosk.webview

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


class SettingsActivity : AppCompatActivity() {
    private val fileIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.v("SettingsActivity", "file result $result")
            result.data?.let { intent ->
                startAuthorization(intent)
            }
        }

    private val authIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.v("SettingsActivity", "auth result $result")
            (application as Application).authorizationHelper.handleAuthResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.auth).setOnClickListener {
            Log.v("SettingsActivity", "onClickListener")
            onAuthButtonClick()
        }

        // Test token retrieval.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    try {
                        Log.v("SettingsActivity", "requesting AuthState...")
                        val authState =
                            (application as Application).authorizationHelper.getAuthState()
                        Log.v("SettingsActivity", "token ${authState?.accessTokenExpirationTime} ${authState?.accessToken}")
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "${e.message}")
                    }
                    delay(60_000)
                }
            }
        }
    }

    private fun onAuthButtonClick() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        fileIntentLauncher.launch(intent)
    }

    private fun startAuthorization(fileResultIntent: Intent) {
        try {
            val config = fileResultIntent.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    // Parse file contents as JSON and convert to data class.
                    val text = inputStreamToString(inputStream)
                    Log.v("SettingsActivity", text)
                    val gson = Gson()
                    gson.fromJson(text, AuthorizationHelper.Config::class.java)
                }
            } ?: throw Exception("Invalid auth config")

            (application as Application).authorizationHelper.createAuthIntent(config) { intent ->
                authIntentLauncher.launch(intent)
            }
        } catch (e: Throwable) {
            Log.e("SettingsActivity", "Auth config file error: ${e.message}")
            Toast.makeText(application, "${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun inputStreamToString(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
            stringBuilder.append(System.lineSeparator())
        }
        return stringBuilder.toString()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Allow only letters 'u' and 'd' in "code" preference.
            val codePref = findPreference<EditTextPreference>("code")
            requireNotNull(codePref)
            codePref.setOnBindEditTextListener {editText ->
                editText.filters = arrayOf(object: InputFilter {
                    override fun filter(
                        source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int
                    ): CharSequence {
                        // Remove invalid letters from incoming changes.
                        return source.replace(Regex("[^ud]*"), "")
                    }

                })
            }
        }
    }
}
