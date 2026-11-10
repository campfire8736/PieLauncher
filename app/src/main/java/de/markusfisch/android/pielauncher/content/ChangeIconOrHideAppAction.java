package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;

public class ChangeIconOrHideAppAction implements PieItemAction {
	public static final String ID_CHANGE_ICON = "change_icon";
	public static final String ID_HIDE_APP = "hide_app";

	private final boolean hasPacks;
	private Bitmap cachedIcon;

	public ChangeIconOrHideAppAction(boolean hasPacks) {
		this.hasPacks = hasPacks;
	}

	public boolean hasPacks() {
		return hasPacks;
	}

	@Override
	public String getId() {
		return hasPacks ? ID_CHANGE_ICON : ID_HIDE_APP;
	}

	@Override
	public Bitmap getIcon(Context context) {
		if (cachedIcon == null && context != null) {
			int resId = hasPacks ? R.drawable.ic_edit : R.drawable.ic_hide;
			cachedIcon = EditorRenderer.getWhiteBitmapFromDrawable(context, resId);
		}
		return cachedIcon;
	}

	@Override
	public String getTip(Context context) {
		if (context == null) {
			return hasPacks ? "Change icon" : "Hide app";
		}
		return context.getString(hasPacks ? R.string.change_icon : R.string.hide_app);
	}

	@Override
	public void perform(Context context, PieItem item, ActionListener listener) {
		if (listener == null) {
			return;
		}
		if (hasPacks) {
			listener.changeIcon(item);
		} else if (item instanceof Apps.AppIcon) {
			listener.hideApp((Apps.AppIcon) item);
		}
	}
}
