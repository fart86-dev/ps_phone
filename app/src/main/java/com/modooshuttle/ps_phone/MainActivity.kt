package com.modooshuttle.ps_phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.modooshuttle.ps_phone.ui.theme.Ps_phoneTheme

class MainActivity : ComponentActivity() {
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      Log.d("MainActivity", "READ_CONTACTS 권한 승인됨")
    } else {
      Log.w("MainActivity", "READ_CONTACTS 권한 거부됨")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      Ps_phoneTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Greeting(
            name = "Android",
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
    requestRequiredPermissions()
    if (!isNotificationListenerEnabled()) {
      this.requestNotificationPermission()
    }
  }

  private fun requestRequiredPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
      }
    }
  }

  private fun isNotificationListenerEnabled(): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
    return packageName in enabledListeners
  }

  private fun requestNotificationPermission() {
    try {
      val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
      startActivity(intent)
    } catch (e: Exception) {
      // 일부 기기에서 설정 화면이 다를 경우 대비
      val intent = Intent(Settings.ACTION_SETTINGS)
      startActivity(intent)
    }
  }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  Ps_phoneTheme {
    Greeting("Android")
  }
}