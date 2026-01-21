package com.darkrockstudios.apps.fasttrack.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.coroutineScope
import com.darkrockstudios.apps.fasttrack.FastingNotificationManager
import com.darkrockstudios.apps.fasttrack.R
import com.darkrockstudios.apps.fasttrack.data.activefast.ActiveFastRepository
import com.darkrockstudios.apps.fasttrack.data.log.FastingLogRepository
import com.darkrockstudios.apps.fasttrack.data.settings.SettingsDatasource
import com.darkrockstudios.apps.fasttrack.ui.theme.FastTrackTheme
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

class SettingsActivity : AppCompatActivity() {
	private val settings by inject<SettingsDatasource>()
	private val activeFastRepository by inject<ActiveFastRepository>()
	private val logRepository by inject<FastingLogRepository>()
	private lateinit var requestNotificationPermission: ActivityResultLauncher<String>
	private lateinit var getContent: ActivityResultLauncher<String>
	private lateinit var createDocument: ActivityResultLauncher<String>
	private lateinit var openDocument: ActivityResultLauncher<Array<String>>
	private var pendingNotificationToggle = false
	private var notificationSettingState by mutableStateOf(false)
	private var stageAlertsSettingState by mutableStateOf(false)
	private var autoExportEnabled by mutableStateOf(false)
	private var autoExportPath by mutableStateOf<String?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		WindowCompat.getInsetsController(window, window.decorView)
			.isAppearanceLightStatusBars = false

		notificationSettingState = settings.getShowFastingNotification()
		stageAlertsSettingState = settings.getFastingAlerts()
		autoExportEnabled = settings.getAutoExportEnabled()
		autoExportPath = getExportPathDisplay()
		registerNotificationPermissionCallback()
		registerImportCallback()
		registerCreateDocumentCallback()
		registerOpenDocumentCallback()

		setContent {
			FastTrackTheme {
				SettingsScreen(
					onBack = { finish() },
					settings = settings,
					notificationSettingState = notificationSettingState,
					onNotificationSettingChanged = { enabled -> handleNotificationSettingChange(enabled) },
					stageAlertsSettingState = stageAlertsSettingState,
					onStageAlertsSettingChanged = { enabled -> handleStageAlertsSettingChange(enabled) },
					autoExportEnabled = autoExportEnabled,
					autoExportPath = autoExportPath,
					onAutoExportToggleChanged = { enabled -> handleAutoExportToggleChange(enabled) },
					onChangeExportLocation = { handleChangeExportLocation() },
					onExportClick = { onExportLogBook() },
					onImportClick = { onImportLogBook() }
				)
			}
		}
	}

	private fun registerNotificationPermissionCallback() {
		requestNotificationPermission = registerForActivityResult(
			ActivityResultContracts.RequestPermission()
		) { isGranted: Boolean ->
			if (isGranted) {
				Napier.d("Notification permission granted")
				if (pendingNotificationToggle) {
					settings.setShowFastingNotification(true)
					notificationSettingState = true
					pendingNotificationToggle = false

					// Show the notification if there's an active fast
					if (activeFastRepository.isFasting()) {
						val elapsedTime = activeFastRepository.getElapsedFastTime()
						FastingNotificationManager.postFastingNotification(this, elapsedTime)
					}
				}
			} else {
				Napier.w("Notification permission denied")
				// Reset the toggle since permission was denied
				settings.setShowFastingNotification(false)
				notificationSettingState = false
				pendingNotificationToggle = false
			}
		}
	}

	private fun handleNotificationSettingChange(enabled: Boolean) {
		if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			when {
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS
				) == PackageManager.PERMISSION_GRANTED -> {
					Napier.d("Notification permission already granted")
					settings.setShowFastingNotification(true)
					notificationSettingState = true

					// Show the notification if there's an active fast
					if (activeFastRepository.isFasting()) {
						val elapsedTime = activeFastRepository.getElapsedFastTime()
						FastingNotificationManager.postFastingNotification(this, elapsedTime)
					}
				}

				else -> {
					Napier.d("Requesting notification permission")
					pendingNotificationToggle = true
					requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		} else {
			// Either disabled or Android < 13 (no permission needed)
			settings.setShowFastingNotification(enabled)
			notificationSettingState = enabled

			if (enabled) {
				// Show the notification if there's an active fast
				if (activeFastRepository.isFasting()) {
					val elapsedTime = activeFastRepository.getElapsedFastTime()
					FastingNotificationManager.postFastingNotification(this, elapsedTime)
				}
			} else {
				// Dismiss the notification if it's currently displayed
				FastingNotificationManager.cancelFastingNotification(this)
			}
		}
	}

	private fun handleStageAlertsSettingChange(enabled: Boolean) {
		settings.setFastingAlerts(enabled)
		stageAlertsSettingState = enabled
	}

	private fun handleAutoExportToggleChange(enabled: Boolean) {
		if (enabled) {
			val existingUri = settings.getAutoExportUri()
			if (existingUri != null) {
				// Verify permission is still valid
				val uri = Uri.parse(existingUri)
				if (hasUriPermission(uri)) {
					settings.setAutoExportEnabled(true)
					autoExportEnabled = true
				} else {
					// Permission lost, need to re-select location
					settings.setAutoExportUri(null)
					autoExportPath = null
					createDocument.launch("fastingLogbook.csv")
				}
			} else {
				// No URI saved, launch picker
				createDocument.launch("fastingLogbook.csv")
			}
		} else {
			settings.setAutoExportEnabled(false)
			autoExportEnabled = false
		}
	}

	private fun handleChangeExportLocation() {
		openDocument.launch(arrayOf("text/csv", "text/comma-separated-values"))
	}

	private fun getExportPathDisplay(): String? {
		val uriString = settings.getAutoExportUri() ?: return null
		val uri = Uri.parse(uriString)
		// Try to get a user-friendly display name
		return try {
			val cursor = contentResolver.query(uri, null, null, null, null)
			cursor?.use {
				if (it.moveToFirst()) {
					val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
					if (displayNameIndex >= 0) {
						it.getString(displayNameIndex)
					} else {
						uri.lastPathSegment
					}
				} else {
					uri.lastPathSegment
				}
			} ?: uri.lastPathSegment
		} catch (e: Exception) {
			uri.lastPathSegment
		}
	}

	private fun hasUriPermission(uri: Uri): Boolean {
		val persistedUris = contentResolver.persistedUriPermissions
		return persistedUris.any { it.uri == uri && it.isWritePermission }
	}

	private fun registerCreateDocumentCallback() {
		createDocument = registerForActivityResult(
			ActivityResultContracts.CreateDocument("text/csv")
		) { uri: Uri? ->
			if (uri != null) {
				// Release old permission if exists
				val oldUriString = settings.getAutoExportUri()
				if (oldUriString != null) {
					try {
						val oldUri = Uri.parse(oldUriString)
						contentResolver.releasePersistableUriPermission(
							oldUri,
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION
						)
					} catch (e: Exception) {
						Napier.w("Failed to release old permission", e)
					}
				}

				// Take persistent permission for new URI
				try {
					contentResolver.takePersistableUriPermission(
						uri,
						Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					)
					settings.setAutoExportUri(uri.toString())
					settings.setAutoExportEnabled(true)
					autoExportEnabled = true
					autoExportPath = getExportPathDisplay()

					// Perform initial export
					performAutoExport(uri)
				} catch (e: Exception) {
					Napier.w("Failed to take persistable permission", e)
					Toast.makeText(
						this,
						getString(R.string.auto_export_failed),
						Toast.LENGTH_SHORT
					).show()
				}
			}
			// If uri is null, user cancelled - toggle stays off
		}
	}

	private fun registerOpenDocumentCallback() {
		openDocument = registerForActivityResult(
			ActivityResultContracts.OpenDocument()
		) { uri: Uri? ->
			if (uri != null) {
				// Release old permission if exists
				val oldUriString = settings.getAutoExportUri()
				if (oldUriString != null) {
					try {
						val oldUri = Uri.parse(oldUriString)
						contentResolver.releasePersistableUriPermission(
							oldUri,
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION
						)
					} catch (e: Exception) {
						Napier.w("Failed to release old permission", e)
					}
				}

				// Take persistent permission for new URI
				try {
					contentResolver.takePersistableUriPermission(
						uri,
						Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					)
					settings.setAutoExportUri(uri.toString())
					autoExportPath = getExportPathDisplay()

					// Perform export to verify it works
					performAutoExport(uri)
				} catch (e: Exception) {
					Napier.w("Failed to take persistable permission", e)
					Toast.makeText(
						this,
						getString(R.string.auto_export_failed),
						Toast.LENGTH_SHORT
					).show()
				}
			}
		}
	}

	private fun performAutoExport(uri: Uri) {
		lifecycle.coroutineScope.launch(Dispatchers.IO) {
			try {
				val csvLog = logRepository.exportLog()
				contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
					outputStream.write(csvLog.toByteArray())
				}
				withContext(Dispatchers.Main) {
					Toast.makeText(
						this@SettingsActivity,
						getString(R.string.auto_export_success),
						Toast.LENGTH_SHORT
					).show()
				}
			} catch (e: Exception) {
				Napier.w("Auto-export failed", e)
				withContext(Dispatchers.Main) {
					Toast.makeText(
						this@SettingsActivity,
						getString(R.string.auto_export_failed),
						Toast.LENGTH_SHORT
					).show()
				}
			}
		}
	}

	private fun registerImportCallback() {
		getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
			uri?.let {
				lifecycle.coroutineScope.launch(Dispatchers.Default) {
					try {
						val inputStream = contentResolver.openInputStream(it)
						val csvContent =
							inputStream?.bufferedReader()?.use { reader -> reader.readText() }

						if (csvContent != null) {
							val success = logRepository.importLog(csvContent)
							val messageResId =
								if (success) R.string.import_success else R.string.import_failed
							withContext(Dispatchers.Main) {
								Toast.makeText(
									this@SettingsActivity,
									getString(messageResId),
									Toast.LENGTH_SHORT
								).show()
							}
						} else {
							withContext(Dispatchers.Main) {
								Toast.makeText(
									this@SettingsActivity,
									getString(R.string.import_failed),
									Toast.LENGTH_SHORT
								)
									.show()
							}
						}
					} catch (e: Exception) {
						Napier.w("Failed to import Log", e)
						withContext(Dispatchers.Main) {
							Toast.makeText(
								this@SettingsActivity,
								getString(R.string.import_failed),
								Toast.LENGTH_SHORT
							).show()
						}
					}
				}
			}
		}
	}

	private fun onExportLogBook() {
		lifecycle.coroutineScope.launch {
			val csvLog = logRepository.exportLog()

			val csvFile = File(cacheDir, "fastingLogbook.csv")

			try {
				csvFile.writeText(csvLog)

				val fileUri = FileProvider.getUriForFile(
					this@SettingsActivity,
					"${packageName}.fileprovider",
					csvFile
				)

				val sendIntent: Intent = Intent().apply {
					action = Intent.ACTION_SEND
					putExtra(Intent.EXTRA_STREAM, fileUri)
					type = "text/csv"
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				}

				val shareIntent = Intent.createChooser(sendIntent, getString(R.string.app_name))
				startActivity(shareIntent)
			} catch (_: Exception) {
				Toast.makeText(
					this@SettingsActivity,
					getString(R.string.export_failed),
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	private fun onImportLogBook() {
		getContent.launch("text/*")
	}
}
