package com.darkrockstudios.apps.fasttrack.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.darkrockstudios.apps.fasttrack.R
import com.darkrockstudios.apps.fasttrack.data.settings.SettingsDatasource
import com.darkrockstudios.apps.fasttrack.utils.MAX_COLUMN_WIDTH


@Composable
fun SettingsScreen(
	onBack: () -> Unit,
	settings: SettingsDatasource,
	notificationSettingState: Boolean,
	onNotificationSettingChanged: (Boolean) -> Unit,
	stageAlertsSettingState: Boolean,
	onStageAlertsSettingChanged: (Boolean) -> Unit,
	autoExportEnabled: Boolean,
	autoExportPath: String?,
	onAutoExportToggleChanged: (Boolean) -> Unit,
	onChangeExportLocation: () -> Unit,
	onExportClick: () -> Unit,
	onImportClick: () -> Unit
) {
	Scaffold(
		topBar = {
			TopAppBar(
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer,
					titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
					navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
				),
				title = { Text(text = stringResource(id = R.string.title_activity_settings)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.Filled.ArrowBack,
							contentDescription = stringResource(id = R.string.back)
						)
					}
				},
			)
		}
	) { paddingValues ->
		SettingsList(
			paddingValues = paddingValues,
			settings = settings,
			notificationSettingState = notificationSettingState,
			onNotificationSettingChanged = onNotificationSettingChanged,
			stageAlertsSettingState = stageAlertsSettingState,
			onStageAlertsSettingChanged = onStageAlertsSettingChanged,
			autoExportEnabled = autoExportEnabled,
			autoExportPath = autoExportPath,
			onAutoExportToggleChanged = onAutoExportToggleChanged,
			onChangeExportLocation = onChangeExportLocation,
			onExportClick = onExportClick,
			onImportClick = onImportClick
		)
	}
}

@Composable
private fun SettingsList(
	paddingValues: PaddingValues,
	settings: SettingsDatasource,
	notificationSettingState: Boolean,
	onNotificationSettingChanged: (Boolean) -> Unit,
	stageAlertsSettingState: Boolean,
	onStageAlertsSettingChanged: (Boolean) -> Unit,
	autoExportEnabled: Boolean,
	autoExportPath: String?,
	onAutoExportToggleChanged: (Boolean) -> Unit,
	onChangeExportLocation: () -> Unit,
	onExportClick: () -> Unit,
	onImportClick: () -> Unit
) {
	var fancyBackground by remember { mutableStateOf(settings.getShowFancyBackground()) }

	Box(modifier = Modifier.fillMaxSize()) {
		LazyColumn(
			modifier = Modifier
				.widthIn(max = MAX_COLUMN_WIDTH)
				.fillMaxHeight()
				.align(Alignment.Center),
			contentPadding = paddingValues
		) {
			item(key = "notifications_header") {
				SettingsSectionHeader(title = R.string.settings_section_notifications)
			}
			item(key = "fasting_notification") {
				SettingsItem(
					headline = R.string.settings_fasting_notification_title,
					details = R.string.settings_fasting_notification_subtitle,
					value = notificationSettingState,
					onChange = { checked ->
						onNotificationSettingChanged(checked)
					}
				)
			}
			item(key = "stage_alerts") {
				SettingsItem(
					headline = R.string.settings_stage_alerts_title,
					details = R.string.settings_stage_alerts_subtitle,
					value = stageAlertsSettingState,
					onChange = { checked ->
						onStageAlertsSettingChanged(checked)
					}
				)
			}
			item(key = "ui_header") {
				SettingsSectionHeader(title = R.string.settings_section_ui)
			}
			item(key = "fancy_background") {
				SettingsItem(
					headline = R.string.settings_fancy_background_title,
					details = R.string.settings_fancy_background_subtitle,
					value = fancyBackground,
					onChange = { checked ->
						fancyBackground = checked
						settings.setShowFancyBackground(checked)
					}
				)
			}
			item(key = "logbook_header") {
				SettingsSectionHeader(title = R.string.settings_section_logbook)
			}
			item(key = "auto_export") {
				AutoExportSettingsItem(
					enabled = autoExportEnabled,
					path = autoExportPath,
					onToggleChanged = onAutoExportToggleChanged,
					onChangeLocation = onChangeExportLocation
				)
			}
			item(key = "export_logbook") {
				SettingsActionItem(
					headline = R.string.action_export,
					details = R.string.action_export_description,
					onClick = onExportClick
				)
			}
			item(key = "import_logbook") {
				SettingsActionItem(
					headline = R.string.action_import,
					details = R.string.action_import_description,
					onClick = onImportClick
				)
			}
		}
	}
}

@Composable
private fun SettingsSectionHeader(
	@StringRes title: Int
) {
	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary
			)
		}
	)
}

@Composable
private fun SettingsItem(
	@StringRes headline: Int,
	@StringRes details: Int,
	value: Boolean,
	onChange: (enabled: Boolean) -> Unit
) {
	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = headline),
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold
			)
		},
		supportingContent = {
			Text(
				text = stringResource(id = details),
				style = MaterialTheme.typography.bodySmall
			)
		},
		trailingContent = {
			Switch(
				checked = value,
				onCheckedChange = onChange
			)
		}
	)
}

@Composable
private fun SettingsActionItem(
	@StringRes headline: Int,
	@StringRes details: Int,
	onClick: () -> Unit
) {
	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = headline),
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold
			)
		},
		supportingContent = {
			Text(
				text = stringResource(id = details),
				style = MaterialTheme.typography.bodySmall
			)
		},
		modifier = Modifier.clickable { onClick() }
	)
}

@Composable
private fun AutoExportSettingsItem(
	enabled: Boolean,
	path: String?,
	onToggleChanged: (Boolean) -> Unit,
	onChangeLocation: () -> Unit
) {
	ListItem(
		headlineContent = {
			Text(
				text = stringResource(id = R.string.settings_auto_export_title),
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold
			)
		},
		supportingContent = {
			Column {
				Text(
					text = stringResource(id = R.string.settings_auto_export_subtitle),
					style = MaterialTheme.typography.bodySmall
				)
				if (enabled && path != null) {
					Spacer(modifier = Modifier.height(4.dp))
					Row(
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = stringResource(id = R.string.settings_auto_export_path, path),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.primary,
							modifier = Modifier.weight(1f)
						)
						TextButton(
							onClick = onChangeLocation,
							contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
						) {
							Text(
								text = stringResource(id = R.string.settings_auto_export_change),
								style = MaterialTheme.typography.bodySmall
							)
						}
					}
				}
			}
		},
		trailingContent = {
			Switch(
				checked = enabled,
				onCheckedChange = onToggleChanged
			)
		}
	)
}