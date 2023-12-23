package com.shoestringresearch.kiosk.webview

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
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
        (application as Application).authorizationHelper.createAuthIntent { intent ->
            authIntentLauncher.launch(intent)
        }
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
