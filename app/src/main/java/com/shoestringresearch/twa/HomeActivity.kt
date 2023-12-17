package com.shoestringresearch.twa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class HomeActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)
        text.text = getString(android.R.string.untitled)
        setContentView(text)
    }

    override fun onResume() {
        super.onResume()

        // Open the TWA.
        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
    }
}
