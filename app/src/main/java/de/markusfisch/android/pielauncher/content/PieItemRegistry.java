package de.markusfisch.android.pielauncher.content;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class PieItemRegistry {
	public interface ItemResolver {
		PieItem resolve(
				Context context,
				String subKey,
				Map<LauncherItemKey, Apps.AppIcon> allApps);
	}

	private static final Map<String, ItemResolver> resolvers = new HashMap<>();

	static {
		// Built-in resolver for "app:" prefix
		registerResolver("app", (context, subKey, allApps) -> {
			LauncherItemKey key = LauncherItemKey.unflattenFromString(context, subKey);
			return allApps != null ? allApps.get(key) : null;
		});
		// Built-in resolver for "folder:" prefix
		registerResolver("folder", (context, subKey, allApps) ->
				FolderManager.getOrCreateFolder(context, subKey, allApps));
	}

	public static synchronized void registerResolver(
			String prefix,
			ItemResolver resolver) {
		resolvers.put(prefix, resolver);
	}

	public static synchronized void unregisterResolver(String prefix) {
		resolvers.remove(prefix);
	}

	public static PieItem restoreItem(
			Context context,
			String itemKey,
			Map<LauncherItemKey, Apps.AppIcon> allApps) {
		if (itemKey == null || itemKey.isEmpty()) {
			return null;
		}
		int colon = itemKey.indexOf(':');
		if (colon > 0) {
			String prefix = itemKey.substring(0, colon);
			String subKey = itemKey.substring(colon + 1);
			ItemResolver resolver;
			synchronized (PieItemRegistry.class) {
				resolver = resolvers.get(prefix);
			}
			if (resolver != null) {
				return resolver.resolve(context, subKey, allApps);
			}
		}
		// Fallback for legacy keys without prefix (e.g. "package/activity#user")
		LauncherItemKey key = LauncherItemKey.unflattenFromString(context, itemKey);
		return allApps != null ? allApps.get(key) : null;
	}
}
