package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Process;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DatabaseTest {
	private Context context;
	private Database database;
	private Map<LauncherItemKey, Apps.AppIcon> allApps;
	private Apps.AppIcon app1;
	private Apps.AppIcon app2;
	private Apps.AppIcon app3;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		database = PieLauncherApp.getDatabase(context);

		allApps = new HashMap<>();
		app1 = new Apps.AppIcon(
				new ComponentName("com.pkg.one", "com.pkg.one.Main"),
				"App One", (Bitmap) null, Process.myUserHandle());
		app2 = new Apps.AppIcon(
				new ComponentName("com.pkg.two", "com.pkg.two.Main"),
				"App Two", (Bitmap) null, Process.myUserHandle());
		app3 = new Apps.AppIcon(
				new ComponentName("com.pkg.three", "com.pkg.three.Main"),
				"App Three", (Bitmap) null, Process.myUserHandle());

		allApps.put(new LauncherItemKey(app1.componentName, app1.userHandle), app1);
		allApps.put(new LauncherItemKey(app2.componentName, app2.userHandle), app2);
		allApps.put(new LauncherItemKey(app3.componentName, app3.userHandle), app3);
	}

	@After
	public void tearDown() {
		database.deleteMenu("menu");
		database.deleteMenu("menu_alt");
	}

	@Test
	public void testStoreAndRestorePrimaryAndSecondaryMenus() {
		List<PieItem> primary = new ArrayList<>();
		primary.add(app1);
		primary.add(app2);

		List<PieItem> secondary = new ArrayList<>();
		secondary.add(app3);

		database.storeMenuSync("menu", primary);
		database.storeMenuSync("menu_alt", secondary);

		List<PieItem> restoredPrimary = database.restoreMenu("menu", allApps);
		assertEquals(2, restoredPrimary.size());
		assertEquals(app1, restoredPrimary.get(0));
		assertEquals(app2, restoredPrimary.get(1));

		List<PieItem> restoredSecondary = database.restoreMenu("menu_alt", allApps);
		assertEquals(1, restoredSecondary.size());
		assertEquals(app3, restoredSecondary.get(0));
	}

	@Test
	public void testStoreAndRestoreFolderMenu() {
		List<PieItem> folderItems = new ArrayList<>();
		folderItems.add(app1);
		folderItems.add(app3);

		database.storeMenuSync("folder:my_folder", folderItems);

		List<PieItem> restored = database.restoreMenu("folder:my_folder", allApps);
		assertEquals(2, restored.size());
		assertEquals(app1, restored.get(0));
		assertEquals(app3, restored.get(1));
	}

	@Test
	public void testUniquenessConstraintInPrimaryMenuAcrossFolders() {
		// App 1 is added to Main Menu ("menu")
		List<PieItem> primary = new ArrayList<>();
		primary.add(app1);
		database.storeMenuSync("menu", primary);

		Map<String, String> assignments = database.getPrimaryMenuFolderAssignments();
		assertEquals("root", assignments.get(app1.getItemKey(context)));

		// App 1 is now added to folder:test_f
		List<PieItem> folderItems = new ArrayList<>();
		folderItems.add(app1);
		database.storeMenuSync("folder:test_f", folderItems);

		assignments = database.getPrimaryMenuFolderAssignments();
		// In the database table pie_menu_primary, app1's folder_id is now test_f (not duplicated!)
		assertEquals("test_f", assignments.get(app1.getItemKey(context)));
	}

	@Test
	public void testDeleteFolderRemovesFolderItemsAndFolderEntry() {
		FolderItem folder = new FolderItem("f_to_del", "Folder To Del", null, new ArrayList<>());
		List<PieItem> root = new ArrayList<>();
		root.add(folder);
		database.storeMenuSync("menu", root);

		List<PieItem> folderContent = new ArrayList<>();
		folderContent.add(app2);
		database.storeMenuSync("folder:f_to_del", folderContent);

		Map<String, String> assignments = database.getPrimaryMenuFolderAssignments();
		assertTrue(assignments.containsKey("folder:f_to_del"));
		assertTrue(assignments.containsKey(app2.getItemKey(context)));

		database.deleteMenu("folder:f_to_del");

		assignments = database.getPrimaryMenuFolderAssignments();
		assertFalse(assignments.containsKey("folder:f_to_del"));
		assertFalse(assignments.containsKey(app2.getItemKey(context)));
	}

	@Test
	public void testFolderRecordCrud() {
		database.insertOrUpdateFolder("folder_1", "Social", 0xffe91e63);
		Database.FolderRecord record = database.getFolder("folder_1");
		assertNotNull(record);
		assertEquals("folder_1", record.id);
		assertEquals("Social", record.name);
		assertEquals(0xffe91e63, record.color);

		database.insertOrUpdateFolder("folder_1", "Social Media", 0xff9c27b0);
		record = database.getFolder("folder_1");
		assertNotNull(record);
		assertEquals("Social Media", record.name);
		assertEquals(0xff9c27b0, record.color);

		database.deleteFolder("folder_1");
		assertNull(database.getFolder("folder_1"));
	}
}
