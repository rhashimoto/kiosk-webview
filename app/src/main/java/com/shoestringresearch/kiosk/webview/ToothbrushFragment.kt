package com.shoestringresearch.kiosk.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import java.util.Timer
import java.util.TimerTask

const val FLASH_PERIOD = 3000L

class ToothbrushFragment: Fragment() {
    private var backgroundColor by mutableStateOf(Color.White)
    private var brushTime by mutableStateOf(0L)
    private var brushDuration by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val timer = Timer()
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                backgroundColor = Color.Yellow
            }
        }, 0L, FLASH_PERIOD / 2L)
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                backgroundColor = Color.White
            }
        }, FLASH_PERIOD / 4L, FLASH_PERIOD / 2L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when viewLifecycleOwner is destroyed
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Brush your teeth!", fontSize = 120.sp, fontWeight = FontWeight.Bold)
                        Text("Use the electric toothbrush and count slowly to 100 to clear this message", fontSize = 64.sp, textAlign = TextAlign.Center)

                        val since = (System.currentTimeMillis() - brushTime + 1800000) / 3600000
                        Text("You brushed $since hours ago for $brushDuration seconds", fontSize = 64.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val activity = requireActivity() as HomeActivity
        val brushing = activity.getBrushing()
        brushTime = brushing.time
        brushDuration = brushing.duration
    }
}
