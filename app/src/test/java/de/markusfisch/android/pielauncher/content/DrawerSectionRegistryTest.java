package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DrawerSectionRegistryTest {

	private Context context;
	private Bitmap mockBitmap;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		mockBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
		DrawerSectionRegistry.resetToDefaults();
	}

	@After
	public void tearDown() {
		DrawerSectionRegistry.resetToDefaults();
	}

	private Apps.AppIcon createAppIcon(String name) {
		try {
			Constructor<Apps.AppIcon> c = Apps.AppIcon.class.getDeclaredConstructor(
					ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
			c.setAccessible(true);
			return c.newInstance(
					new ComponentName("com.example", name),
					name,
					mockBitmap,
					Process.myUserHandle()
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Apps.AppIcon createShortcutIcon(String name, String shortcutId) {
		Apps.AppIcon icon = createAppIcon(name);
		icon.shortcutId = shortcutId;
		icon.shortcutPackage = "com.example";
		return icon;
	}

	@Test
	public void testDefaultSectionsRegistered() {
		List<DrawerSection> sections = DrawerSectionRegistry.getSections();
		assertEquals(2, sections.size());
		assertEquals(DrawerSectionRegistry.SECTION_ID_APPS, sections.get(0).getId());
		assertEquals("Android Apps", sections.get(0).getTitle(context));
		assertEquals(DrawerSectionRegistry.SECTION_ID_APP_SHORTCUTS, sections.get(1).getId());
		assertEquals("App shortcuts", sections.get(1).getTitle(context));
	}

	@Test
	public void testGroupSeparatesAppsAndShortcuts() {
		Apps.AppIcon app1 = createAppIcon("Calculator");
		Apps.AppIcon shortcut1 = createShortcutIcon("New Tab", "new_tab_id");
		Apps.AppIcon app2 = createAppIcon("Maps");
		Apps.AppIcon shortcut2 = createShortcutIcon("Directions", "directions_id");

		List<Apps.AppIcon> items = new ArrayList<>();
		items.add(app1);
		items.add(shortcut1);
		items.add(app2);
		items.add(shortcut2);

		List<DrawerSectionRegistry.SectionGroup> groups = DrawerSectionRegistry.group(items);
		assertEquals(2, groups.size());

		// Group 0: Android Apps
		DrawerSectionRegistry.SectionGroup appsGroup = groups.get(0);
		assertEquals(DrawerSectionRegistry.SECTION_ID_APPS, appsGroup.section.getId());
		assertEquals(2, appsGroup.items.size());
		assertEquals(app1, appsGroup.items.get(0));
		assertEquals(app2, appsGroup.items.get(1));

		// Group 1: App shortcuts
		DrawerSectionRegistry.SectionGroup shortcutsGroup = groups.get(1);
		assertEquals(DrawerSectionRegistry.SECTION_ID_APP_SHORTCUTS, shortcutsGroup.section.getId());
		assertEquals(2, shortcutsGroup.items.size());
		assertEquals(shortcut1, shortcutsGroup.items.get(0));
		assertEquals(shortcut2, shortcutsGroup.items.get(1));

		// Flattened preserves section separation
		List<Apps.AppIcon> flat = DrawerSectionRegistry.flatten(groups);
		assertEquals(4, flat.size());
		assertEquals(app1, flat.get(0));
		assertEquals(app2, flat.get(1));
		assertEquals(shortcut1, flat.get(2));
		assertEquals(shortcut2, flat.get(3));
	}

	@Test
	public void testGroupOmitsEmptySections() {
		Apps.AppIcon app1 = createAppIcon("Calculator");
		List<Apps.AppIcon> onlyApps = new ArrayList<>();
		onlyApps.add(app1);

		List<DrawerSectionRegistry.SectionGroup> groups = DrawerSectionRegistry.group(onlyApps);
		assertEquals(1, groups.size());
		assertEquals(DrawerSectionRegistry.SECTION_ID_APPS, groups.get(0).section.getId());
		assertEquals(1, groups.get(0).items.size());

		Apps.AppIcon shortcut1 = createShortcutIcon("New Tab", "new_tab_id");
		List<Apps.AppIcon> onlyShortcuts = new ArrayList<>();
		onlyShortcuts.add(shortcut1);

		List<DrawerSectionRegistry.SectionGroup> shortcutGroups = DrawerSectionRegistry.group(onlyShortcuts);
		assertEquals(1, shortcutGroups.size());
		assertEquals(DrawerSectionRegistry.SECTION_ID_APP_SHORTCUTS, shortcutGroups.get(0).section.getId());
		assertEquals(1, shortcutGroups.get(0).items.size());
	}

	@Test
	public void testModularCustomSectionRegistration() {
		DrawerSection contactsSection = new DrawerSection() {
			@Override
			public String getId() {
				return "contacts";
			}

			@Override
			public CharSequence getTitle(Context context) {
				return "Contacts";
			}

			@Override
			public boolean matches(Apps.AppIcon item) {
				return item != null && "ContactsApp".equals(item.label);
			}
		};

		// Register at index 0 (top priority)
		DrawerSectionRegistry.registerSection(0, contactsSection);

		Apps.AppIcon contactItem = createAppIcon("ContactsApp");
		Apps.AppIcon normalApp = createAppIcon("NormalApp");
		List<Apps.AppIcon> items = new ArrayList<>();
		items.add(normalApp);
		items.add(contactItem);

		List<DrawerSectionRegistry.SectionGroup> groups = DrawerSectionRegistry.group(items);
		assertEquals(2, groups.size());
		assertEquals("contacts", groups.get(0).section.getId());
		assertEquals(contactItem, groups.get(0).items.get(0));
		assertEquals(DrawerSectionRegistry.SECTION_ID_APPS, groups.get(1).section.getId());
		assertEquals(normalApp, groups.get(1).items.get(0));
	}
}
