package com.modooshuttle.ps_phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
          SettingsScreen(
            context = this@MainActivity,
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
fun SettingsScreen(context: MainActivity, modifier: Modifier = Modifier) {
  val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

  var isKakaoEnabled by remember {
    mutableStateOf(prefs.getBoolean("kakao_enabled", false))
  }
  var isSmsEnabled by remember {
    mutableStateOf(prefs.getBoolean("sms_enabled", false))
  }
  var isCallRecordingEnabled by remember {
    mutableStateOf(prefs.getBoolean("call_recording_enabled", false))
  }
  var handlerName by remember {
    mutableStateOf(prefs.getString("handler_name", "") ?: "")
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text("메시지 수신 설정", modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
      Text("카카오톡", modifier = Modifier.weight(1f))
      Switch(
        checked = isKakaoEnabled,
        onCheckedChange = { isKakaoEnabled = it }
      )
    }

    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
      Text("문자메시지", modifier = Modifier.weight(1f))
      Switch(
        checked = isSmsEnabled,
        onCheckedChange = { isSmsEnabled = it }
      )
    }

    // TODO: WorkManager 기반 S3 업로드 시스템 구현 후 활성화
    // Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
    //   Text("통화 녹음", modifier = Modifier.weight(1f))
    //   Switch(
    //     checked = isCallRecordingEnabled,
    //     onCheckedChange = { isCallRecordingEnabled = it }
    //   )
    // }

    Text("담당자 이름", modifier = Modifier.padding(bottom = 8.dp))
    TextField(
      value = handlerName,
      onValueChange = { handlerName = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("담당자 이름 입력") }
    )

    Button(
      onClick = {
        prefs.edit()
          .putBoolean("kakao_enabled", isKakaoEnabled)
          .putBoolean("sms_enabled", isSmsEnabled)
          // .putBoolean("call_recording_enabled", isCallRecordingEnabled) // TODO: WorkManager 구현 후 활성화
          .putString("handler_name", handlerName)
          .apply()
        Toast.makeText(context, "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        Log.d("SettingsScreen", "설정 저장됨: kakao=$isKakaoEnabled, sms=$isSmsEnabled, handler=$handlerName")
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp)
    ) {
      Text("저장")
    }
  }
}