package com.ministerio.jovenes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ministerio.jovenes.ui.MinistryRoot
import com.ministerio.jovenes.ui.theme.MinistryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MinistryTheme { MinistryRoot() } }
    }
}
