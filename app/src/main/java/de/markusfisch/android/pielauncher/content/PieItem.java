package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;

public abstract class PieItem extends CanvasPieMenu.CanvasIcon {
	public PieItem(Bitmap bitmap) {
		super(bitmap);
	}

	/**
	 * Unique identifier / serialized key for this item in database.
	 * Examples:
	 * - App: "com.example.app/com.example.app.MainActivity#0"
	 * - Custom: "prefix:sub_key"
	 */
	public String getItemKey() {
		return getItemKey(null);
	}

	public abstract String getItemKey(Context context);

	/**
	 * User-visible label/name (e.g. for toast, accessibility, edit mode, search).
	 */
	public abstract String getLabel();

	/**
	 * Launch or execute the action for this item.
	 *
	 * @return true if launched successfully.
	 */
	public abstract boolean launch(Context context);

	/**
	 * Optional: secondary/long-press action or options (e.g. edit/rename/app info).
	 *
	 * @return true if handled.
	 */
	public boolean showOptions(Context context) {
		return false;
	}

	public static PieItem createGhost() {
		return new GhostPieItem();
	}

	private static final class GhostPieItem extends PieItem {
		GhostPieItem() {
			super(null);
			isGhost = true;
		}

		@Override
		public String getItemKey(Context context) {
			return "";
		}

		@Override
		public String getLabel() {
			return "";
		}

		@Override
		public boolean launch(Context context) {
			return false;
		}
	}
}
