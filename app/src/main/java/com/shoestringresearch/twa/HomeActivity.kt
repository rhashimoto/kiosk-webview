package com.shoestringresearch.twa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class HomeActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("HomeActivity", "onCreate")

        val text = TextView(this)
        text.text = "Hello, world!"
        setContentView(text)
    }

    override fun onResume() {
        super.onResume()
        Log.v("HomeActivity", "onResume")

        val intent = Intent(this, LauncherActivity::class.java)
        startActivity(intent)
    }
}
