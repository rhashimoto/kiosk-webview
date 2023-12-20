package com.shoestringresearch.kiosk.webview

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

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
