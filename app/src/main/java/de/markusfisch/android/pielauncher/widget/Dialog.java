package de.markusfisch.android.pielauncher.widget;

import android.app.AlertDialog;
import android.content.Context;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class Dialog {
	public static AlertDialog.Builder newDialog(Context context) {
		Preferences prefs = PieLauncherApp.getPrefs(context);
		boolean isLight = prefs.isLight(context);
		if (prefs.useLightDialogs()) {
			isLight = !isLight;
		}
		int theme = isLight
				? R.style.LightDialog
				: R.style.DarkDialog;
		return new AlertDialog.Builder(context, theme);
	}
}
