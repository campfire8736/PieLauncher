package de.markusfisch.android.pielauncher.content;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;

public class DrawerSectionRegistry {

	public static class SectionGroup {
		public final DrawerSection section;
		public final List<Apps.AppIcon> items = new ArrayList<>();

		public SectionGroup(DrawerSection section) {
			this.section = section;
		}
	}

	public static final String SECTION_ID_APPS = "android_apps";
	public static final String SECTION_ID_APP_SHORTCUTS = "app_shortcuts";

	private static final List<DrawerSection> sections = new ArrayList<>();

	static {
		resetToDefaults();
	}

	public static synchronized void resetToDefaults() {
		sections.clear();
		sections.add(new DrawerSection() {
			@Override
			public String getId() {
				return SECTION_ID_APPS;
			}

			@Override
			public CharSequence getTitle(Context context) {
				return context != null ? context.getString(R.string.drawer_section_apps) : "Android Apps";
			}

			@Override
			public boolean matches(Apps.AppIcon item) {
				return item != null && !Apps.isShortcut(item);
			}
		});

		sections.add(new DrawerSection() {
			@Override
			public String getId() {
				return SECTION_ID_APP_SHORTCUTS;
			}

			@Override
			public CharSequence getTitle(Context context) {
				return context != null ? context.getString(R.string.drawer_section_app_shortcuts) : "App shortcuts";
			}

			@Override
			public boolean matches(Apps.AppIcon item) {
				return Apps.isShortcut(item);
			}
		});
	}

	public static synchronized void registerSection(DrawerSection section) {
		if (section == null) {
			return;
		}
		unregisterSection(section.getId());
		sections.add(section);
	}

	public static synchronized void registerSection(int index, DrawerSection section) {
		if (section == null) {
			return;
		}
		unregisterSection(section.getId());
		int pos = Math.max(0, Math.min(index, sections.size()));
		sections.add(pos, section);
	}

	public static synchronized void unregisterSection(String id) {
		if (id == null) {
			return;
		}
		for (int i = sections.size() - 1; i >= 0; --i) {
			if (id.equals(sections.get(i).getId())) {
				sections.remove(i);
			}
		}
	}

	public static synchronized List<DrawerSection> getSections() {
		return new ArrayList<>(sections);
	}

	/**
	 * Groups the given items into non-empty section groups in the order of registered sections.
	 * If an item matches a section, it is added to that section group and not considered for subsequent sections.
	 * Any item that doesn't match any registered section is placed in the first section as fallback.
	 */
	public static List<SectionGroup> group(List<Apps.AppIcon> items) {
		List<DrawerSection> registered;
		synchronized (DrawerSectionRegistry.class) {
			registered = new ArrayList<>(sections);
		}

		List<SectionGroup> groups = new ArrayList<>(registered.size());
		for (DrawerSection section : registered) {
			groups.add(new SectionGroup(section));
		}

		if (items != null) {
			for (Apps.AppIcon item : items) {
				if (item == null) {
					continue;
				}
				boolean matched = false;
				for (SectionGroup group : groups) {
					if (group.section.matches(item)) {
						group.items.add(item);
						matched = true;
						break;
					}
				}
				if (!matched && !groups.isEmpty()) {
					groups.get(0).items.add(item);
				}
			}
		}

		List<SectionGroup> nonEmptyGroups = new ArrayList<>();
		for (SectionGroup group : groups) {
			if (!group.items.isEmpty()) {
				nonEmptyGroups.add(group);
			}
		}
		return nonEmptyGroups;
	}

	/**
	 * Flattens the section groups into a single contiguous list in section order.
	 */
	public static List<Apps.AppIcon> flatten(List<SectionGroup> groups) {
		List<Apps.AppIcon> flat = new ArrayList<>();
		if (groups != null) {
			for (SectionGroup group : groups) {
				flat.addAll(group.items);
			}
		}
		return flat;
	}
}
