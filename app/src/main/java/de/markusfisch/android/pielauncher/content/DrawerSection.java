package de.markusfisch.android.pielauncher.content;

import android.content.Context;

public interface DrawerSection {
	/**
	 * Unique identifier for this section (e.g. "android_apps", "app_shortcuts").
	 */
	String getId();

	/**
	 * Localized display title for the section header.
	 */
	CharSequence getTitle(Context context);

	/**
	 * Determines if a given app icon belongs to this section.
	 */
	boolean matches(Apps.AppIcon item);
}
