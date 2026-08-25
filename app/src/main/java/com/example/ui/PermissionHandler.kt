package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Phase 2.5: POST_NOTIFICATIONS runtime permission with rationale.
 * Shows a dismissible rationale card; requests permission on button tap.
 * No-op on SDK <33.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Auto-request on first launch if not yet decided and no rationale needed
        if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale && !dismissed) {
            // Don't auto-request immediately; wait for user interaction via rationale card
        }
    }

    if (permissionState.status.isGranted || dismissed) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تفعيل الإشعارات؟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (permissionState.status.shouldShowRationale)
                    "نستخدم الإشعارات لإعلامك عند اكتمال التنزيلات وتوفر التحديثات. يمكنك تغيير هذا لاحقاً من الإعدادات."
                else
                    "اسمح للتطبيق بإرسال إشعارات حول حالة التنزيلات والتحديثات.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { dismissed = true }) {
                    Text("لاحقاً")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { permissionState.launchPermissionRequest() }
                ) {
                    Text("السماح")
                }
            }
        }
    }
}
