package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
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
		List<PieItem> items = PieLauncherApp.getDatabase(context).restoreMenu("folder:" + id, allApps);
		if (items.isEmpty()) {
			MenuDefaults.createInitialMenu(items, allApps, context.getPackageManager());
			if (PieLauncherApp.getPrefs(context).isRigidPie()) {
				Apps.convertToRigid(items);
			}
		}
		FolderItem folder = new FolderItem(
				id,
				context.getString(R.string.folder),
				createFolderIcon(context),
				items);
		folders.put(id, folder);
		return folder;
	}

	public static synchronized FolderItem createNewFolder(
			Context context,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		String id = String.valueOf(System.currentTimeMillis());
		List<PieItem> items = new ArrayList<>();
		MenuDefaults.createInitialMenu(items, allApps, context.getPackageManager());
		if (PieLauncherApp.getPrefs(context).isRigidPie()) {
			Apps.convertToRigid(items);
		}
		FolderItem folder = new FolderItem(
				id,
				context.getString(R.string.folder),
				createFolderIcon(context),
				items);
		folders.put(id, folder);
		PieLauncherApp.getDatabase(context).storeMenu("folder:" + id, folder.items);
		return folder;
	}

	public static synchronized void deleteFolder(Context context, String id) {
		folders.remove(id);
		PieLauncherApp.getDatabase(context).deleteMenuAsync("folder:" + id);
	}

	public static synchronized Collection<FolderItem> getAllFolders() {
		return new ArrayList<>(folders.values());
	}

	public static synchronized void clearCache() {
		folders.clear();
	}

	public static Bitmap createFolderIcon(Context context) {
		return EditorRenderer.getWhiteBitmapFromDrawable(context, R.drawable.ic_folder);
	}
}
