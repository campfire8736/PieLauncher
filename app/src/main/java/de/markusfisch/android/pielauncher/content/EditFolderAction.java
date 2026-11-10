package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;

public class EditFolderAction implements PieItemAction {
	public static final String ID = "edit_folder";

	private Bitmap cachedIcon;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Bitmap getIcon(Context context) {
		if (cachedIcon == null && context != null) {
			cachedIcon = EditorRenderer.getWhiteBitmapFromDrawable(context, R.drawable.ic_edit);
		}
		return cachedIcon;
	}

	@Override
	public String getTip(Context context) {
		return context != null ? context.getString(R.string.edit_folder) : "Edit folder";
	}

	@Override
	public void perform(Context context, PieItem item, ActionListener listener) {
		if (listener != null && item instanceof FolderItem) {
			listener.editFolder((FolderItem) item);
		}
	}
}
