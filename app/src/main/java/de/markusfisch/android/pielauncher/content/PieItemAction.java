package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

public interface PieItemAction {
	String getId();

	Bitmap getIcon(Context context);

	String getTip(Context context);

	void perform(Context context, PieItem item, ActionListener listener);

	interface ActionListener {
		void removeFromPie(PieItem item);

		void changeIcon(PieItem item);

		void hideApp(Apps.AppIcon item);

		void requestAppInfo(Apps.AppIcon item);

		void endEditMode();
	}
}
