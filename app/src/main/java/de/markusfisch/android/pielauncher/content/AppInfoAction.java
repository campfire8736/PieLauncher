package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;

public class AppInfoAction implements PieItemAction {
	public static final String ID_APP_INFO = "app_info";
	public static final String ID_UNINSTALL_APP = "uninstall_app";

	private final boolean isShortcut;
	private Bitmap cachedIcon;

	public AppInfoAction(boolean isShortcut) {
		this.isShortcut = isShortcut;
	}

	public boolean isShortcut() {
		return isShortcut;
	}

	@Override
	public String getId() {
		return isShortcut ? ID_APP_INFO : ID_UNINSTALL_APP;
	}

	@Override
	public Bitmap getIcon(Context context) {
		if (cachedIcon == null && context != null) {
			cachedIcon = EditorRenderer.getWhiteBitmapFromDrawable(context, R.drawable.ic_details);
		}
		return cachedIcon;
	}

	@Override
	public String getTip(Context context) {
		if (context == null) {
			return isShortcut ? "Show app info" : "Uninstall app";
		}
		return context.getString(isShortcut ? R.string.show_app_info : R.string.tip_remove_app);
	}

	@Override
	public void perform(Context context, PieItem item, ActionListener listener) {
		if (listener != null && item instanceof Apps.AppIcon) {
			listener.requestAppInfo((Apps.AppIcon) item);
		}
	}
}
