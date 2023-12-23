package com.shoestringresearch.kiosk.webview

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
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
    }

    private fun onAuthButtonClick() {
        (application as Application).authorizationHelper.createAuthIntent { intent ->
            authIntentLauncher.launch(intent)

            // TODO: Remove this test.
            lifecycleScope.launch {
                val authState = (application as Application).authorizationHelper.getAuthState()
                Log.v("SettingsActivity", "token ${authState?.accessToken}")
            }
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
