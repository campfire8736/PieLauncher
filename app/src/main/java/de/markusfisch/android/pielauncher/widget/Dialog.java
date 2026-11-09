package de.markusfisch.android.pielauncher.widget;

import android.app.AlertDialog;
import android.content.Context;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;

public class Dialog {
	public static AlertDialog.Builder newDialog(Context context) {
		int theme = PieLauncherApp.getPrefs(context).useLightDialogs()
				? R.style.LightDialog
				: R.style.DarkDialog;
		return new AlertDialog.Builder(context, theme);
	}
}
