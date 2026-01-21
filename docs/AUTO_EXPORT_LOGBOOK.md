# Auto-Export Logbook

FastTrack can automatically export your fasting logbook as a CSV file after each fast ends.

## Enabling Auto-Export

1. Open **Settings** from the main screen
2. Scroll to the **Logbook** section
3. Enable **Auto-export Logbook**
4. A file picker will appear - choose where to save the CSV file (you can create a new file or select an existing one)
5. The logbook will be exported immediately to confirm the setup

From now on, your logbook will be automatically exported to this location whenever you complete a fast.

## File Format

The exported file is a standard CSV (Comma-Separated Values) with the following columns:

| Column | Description |
|--------|-------------|
| Start Date/Time | When the fast began |
| End Date/Time | When the fast ended |
| Duration | Length of the fast |

You can open this file with any spreadsheet application (Excel, Google Sheets, LibreOffice Calc) or import it into other tracking tools.

## How It Works

- The app uses Android's Storage Access Framework (SAF) to securely save to your chosen location
- The same file is overwritten each time, keeping your logbook up to date
- The app maintains persistent write permission to the file location

## Troubleshooting

### Auto-export stopped working

If the app can no longer write to the selected location (e.g., the file was deleted or permissions were revoked), the auto-export feature will be automatically disabled. You will see a notification when this happens.

**To fix:**
1. Go to Settings > Logbook
2. Re-enable Auto-export Logbook
3. Select a new save location

### File not updating

Make sure:
- The auto-export toggle is still enabled in Settings
- You have completed a fast (the export happens when a fast ends)
- The target location is accessible (not on a disconnected external storage)

### Want to change the save location?

Tap **Change** next to the current file path to select a new save location.
