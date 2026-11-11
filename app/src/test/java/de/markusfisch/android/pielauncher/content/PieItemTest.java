package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PieItemTest {
	private Context context;
	private Map<LauncherItemKey, Apps.AppIcon> allApps;
	private Apps.AppIcon testApp;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		allApps = new HashMap<>();
		ComponentName cn = new ComponentName("com.example.test", "com.example.test.MainActivity");
		testApp = new Apps.AppIcon(cn, "Test App", (Bitmap) null, null);
		allApps.put(new LauncherItemKey(cn, null), testApp);
	}

	@After
	public void tearDown() {
		PieItemRegistry.unregisterResolver("custom");
	}

	@Test
	public void testAppIconIsPieItem() {
		assertTrue(testApp instanceof PieItem);
		assertEquals("Test App", testApp.getLabel());
		assertEquals("app:com.example.test/com.example.test.MainActivity", testApp.getItemKey(context));
	}

	@Test
	public void testGhostPieItem() {
		PieItem ghost = PieItem.createGhost();
		assertNotNull(ghost);
		assertTrue(ghost.isGhost);
		assertEquals("", ghost.getLabel());
		assertEquals("", ghost.getItemKey(context));
		assertFalse(ghost.launch(context));
	}

	@Test
	public void testPieItemRegistryResolvesLegacyAppKey() {
		String legacyKey = "com.example.test/com.example.test.MainActivity";
		PieItem item = PieItemRegistry.restoreItem(context, legacyKey, allApps);
		assertNotNull(item);
		assertEquals(testApp, item);
	}

	@Test
	public void testPieItemRegistryResolvesAppPrefix() {
		String prefixedKey = "app:com.example.test/com.example.test.MainActivity";
		PieItem item = PieItemRegistry.restoreItem(context, prefixedKey, allApps);
		assertNotNull(item);
		assertEquals(testApp, item);
	}

	@Test
	public void testPieItemRegistryCustomResolver() {
		PieItemRegistry.registerResolver("custom", (ctx, subKey, apps) -> new PieItem(null) {
			@Override
			public String getItemKey(Context c) {
				return "custom:" + subKey;
			}

			@Override
			public String getLabel() {
				return "Custom " + subKey;
			}

			@Override
			public boolean launch(Context c) {
				return true;
			}
		});

		PieItem item = PieItemRegistry.restoreItem(context, "custom:sample_action", allApps);
		assertNotNull(item);
		assertEquals("Custom sample_action", item.getLabel());
		assertEquals("custom:sample_action", item.getItemKey(context));
		assertTrue(item.launch(context));
	}

	@Test
	public void testPieItemRegistryHandlesNullAndUnknownKeys() {
		assertNull(PieItemRegistry.restoreItem(context, null, allApps));
		assertNull(PieItemRegistry.restoreItem(context, "", allApps));
		assertNull(PieItemRegistry.restoreItem(context, "unknown:key", allApps));
		assertNull(PieItemRegistry.restoreItem(context, "com.nonexistent/Activity", allApps));
	}

	@Test
	public void testDefaultPieItemEditActions() {
		PieItem genericItem = new PieItem(null) {
			@Override
			public String getItemKey(Context c) { return "generic"; }
			@Override
			public String getLabel() { return "Generic"; }
			@Override
			public boolean launch(Context c) { return false; }
		};
		java.util.List<PieItemAction> actions = genericItem.getEditActions(context, false);
		assertEquals(1, actions.size());
		assertEquals(RemoveFromPieAction.ID, actions.get(0).getId());
	}

	@Test
	public void testRegularAppEditActions() {
		java.util.List<PieItemAction> actionsNoPacks = testApp.getEditActions(context, false);
		assertEquals(3, actionsNoPacks.size());
		assertEquals(RemoveFromPieAction.ID, actionsNoPacks.get(0).getId());
		assertEquals(ChangeIconOrHideAppAction.ID_HIDE_APP, actionsNoPacks.get(1).getId());
		assertEquals(AppInfoAction.ID_UNINSTALL_APP, actionsNoPacks.get(2).getId());

		java.util.List<PieItemAction> actionsWithPacks = testApp.getEditActions(context, true);
		assertEquals(3, actionsWithPacks.size());
		assertEquals(ChangeIconOrHideAppAction.ID_CHANGE_ICON, actionsWithPacks.get(1).getId());
	}

	@Test
	public void testShortcutEditActions() {
		testApp.shortcutId = "test_shortcut";
		testApp.shortcutPackage = "com.example.test";
		java.util.List<PieItemAction> actions = testApp.getEditActions(context, true);
		assertEquals(2, actions.size());
		assertEquals(RemoveFromPieAction.ID, actions.get(0).getId());
		assertEquals(DeleteShortcutAction.ID, actions.get(1).getId());
		assertEquals("Delete shortcut", actions.get(1).getTip(context));
	}

	@Test
	public void testAppIconEqualsAndHashCode() {
		ComponentName cn1 = new ComponentName("com.pkg.a", "com.pkg.a.MainActivity");
		ComponentName cn2 = new ComponentName("com.pkg.b", "com.pkg.b.MainActivity");
		Apps.AppIcon icon1 = new Apps.AppIcon(cn1, "App A", (Bitmap) null, null);
		Apps.AppIcon icon1Duplicate = new Apps.AppIcon(cn1, "App A", (Bitmap) null, null);
		Apps.AppIcon icon2 = new Apps.AppIcon(cn2, "App B", (Bitmap) null, null);

		assertEquals(icon1, icon1Duplicate);
		assertEquals(icon1.hashCode(), icon1Duplicate.hashCode());
		assertFalse(icon1.equals(icon2));

		// Pinned shortcut comparison
		icon1.shortcutId = "sc1";
		icon1.shortcutPackage = "com.pkg.a";
		assertFalse(icon1.equals(icon1Duplicate));
		icon1Duplicate.shortcutId = "sc1";
		icon1Duplicate.shortcutPackage = "com.pkg.a";
		assertEquals(icon1, icon1Duplicate);

		// Ghosts must not be equal across distinct instances
		Apps.AppIcon ghost1 = Apps.AppIcon.createGhost();
		Apps.AppIcon ghost2 = Apps.AppIcon.createGhost();
		assertFalse(ghost1.equals(ghost2));
		assertEquals(ghost1, ghost1);
	}

	@Test
	public void testFolderItemEqualsAndHashCode() {
		FolderItem folder1 = new FolderItem("folder123", "Folder 1", null);
		FolderItem folder2 = new FolderItem("folder123", "Folder 1 Renamed", null);
		FolderItem folder3 = new FolderItem("folder456", "Folder 2", null);

		assertEquals(folder1, folder2);
		assertEquals(folder1.hashCode(), folder2.hashCode());
		assertFalse(folder1.equals(folder3));
	}
}
