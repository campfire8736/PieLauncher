package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer;

public class FolderManager {
	private static final Map<String, FolderItem> folders = new HashMap<>();

	static {
		PieItemRegistry.registerResolver("folder", (context, subKey, allApps) ->
				getOrCreateFolder(context, subKey, allApps));
	}

	public static synchronized void init() {
		// Ensures static block is executed
	}

	public static synchronized FolderItem getOrCreateFolder(
			Context context,
			String id,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		if (folders.containsKey(id)) {
			return folders.get(id);
		}
		Database.FolderRecord record = PieLauncherApp.getDatabase(context).getFolder(id);
		String name = record != null && record.name != null ? record.name : context.getString(R.string.folder);
		int color = record != null ? record.color : FolderItem.DEFAULT_COLOR;
		if (record == null) {
			PieLauncherApp.getDatabase(context).insertOrUpdateFolder(id, name, color);
		}
		List<PieItem> items = PieLauncherApp.getDatabase(context).restoreMenu("folder:" + id, allApps);
		if (items.isEmpty()) {
			if (PieLauncherApp.getPrefs(context).isRigidPie()) {
				Apps.convertToRigid(items);
			}
		}
		FolderItem folder = new FolderItem(
				id,
				name,
				color,
				createFolderIcon(context, color),
				items);
		folders.put(id, folder);
		return folder;
	}

	public static synchronized FolderItem createNewFolder(
			Context context,
			String id,
			String name,
			int color,
			List<PieItem> items) {
		if (id == null) {
			id = String.valueOf(System.currentTimeMillis());
		}
		if (name == null || name.trim().isEmpty()) {
			name = context.getString(R.string.folder);
		}
		if (color == 0) {
			color = FolderItem.DEFAULT_COLOR;
		}
		if (items == null) {
			items = new ArrayList<>();
		}
		if (items.isEmpty()) {
			if (PieLauncherApp.getPrefs(context).isRigidPie()) {
				Apps.convertToRigid(items);
			}
		}
		FolderItem folder = new FolderItem(
				id,
				name,
				color,
				createFolderIcon(context, color),
				items);
		folders.put(id, folder);
		Database db = PieLauncherApp.getDatabase(context);
		db.insertOrUpdateFolder(id, name, color);
		db.storeMenu("folder:" + id, folder.items);
		return folder;
	}

	public static synchronized FolderItem createNewFolder(
			Context context,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		return createNewFolder(context, null, context.getString(R.string.folder),
				FolderItem.DEFAULT_COLOR, null);
	}

	public static synchronized void updateFolderMetadata(
			Context context,
			FolderItem folder,
			String name,
			int color) {
		if (folder == null) {
			return;
		}
		if (name == null || name.trim().isEmpty()) {
			name = context.getString(R.string.folder);
		}
		if (color == 0) {
			color = FolderItem.DEFAULT_COLOR;
		}
		folder.updateMetadata(context, name, color);
		PieLauncherApp.getDatabase(context).insertOrUpdateFolder(folder.id, name, color);
	}

	public static synchronized void deleteFolder(Context context, String id) {
		folders.remove(id);
		PieLauncherApp.getDatabase(context).deleteMenuAsync("folder:" + id);
		PieLauncherApp.getDatabase(context).deleteFolderAsync(id);
	}

	public static synchronized Collection<FolderItem> getAllFolders() {
		return new ArrayList<>(folders.values());
	}

	public static synchronized void updateFolders(Map<LauncherItemKey, Apps.AppIcon> allApps) {
		if (allApps == null) {
			return;
		}
		for (FolderItem folder : folders.values()) {
			for (int i = 0; i < folder.items.size(); i++) {
				PieItem item = folder.items.get(i);
				if (item instanceof Apps.AppIcon && !item.isGhost) {
					Apps.AppIcon icon = (Apps.AppIcon) item;
					LauncherItemKey key = new LauncherItemKey(icon.componentName, icon.userHandle);
					Apps.AppIcon updated = allApps.get(key);
					if (updated != null) {
						folder.items.set(i, updated);
					}
				}
			}
		}
	}

	public static synchronized void clearCache() {
		folders.clear();
	}

	public static Bitmap createFolderIcon(Context context, int color) {
		if (context == null) {
			return null;
		}
		Drawable d = context.getDrawable(R.drawable.ic_folder);
		if (d != null) {
			d = d.mutate();
			d.setTint(color != 0 ? color : 0xffffffff);
			return Converter.getBitmapFromDrawable(d);
		}
		return null;
	}

	public static Bitmap createFolderIcon(Context context) {
		return createFolderIcon(context, 0xffffffff);
	}
}
