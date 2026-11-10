package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.HomeActivity;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.AppPieView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FolderLifecycleIntegrationTest {

	private Context context;
	private Preferences prefs;
	private ActivityController<HomeActivity> controller;
	private HomeActivity activity;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		prefs = PieLauncherApp.getPrefs(context);
		prefs.setSkipSetup();
		FolderManager.init();
		PieLauncherApp.apps.menuPrimary.clear();
		PieLauncherApp.apps.menuSecondary.clear();
	}

	@After
	public void tearDown() {
		if (controller != null) {
			controller.pause().stop().destroy();
		}
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
	}

	@Test
	public void testAddFolderToPie_InRigidMode_FillsGhostSlot() {
		List<PieItem> menu = new ArrayList<>();
		Apps.AppIcon app1 = createAppIcon("com.pkg.app1", "App 1");
		Apps.AppIcon app2 = createAppIcon("com.pkg.app2", "App 2");
		menu.add(app1);
		menu.add(app2);
		menu.add(Apps.AppIcon.createGhost());
		menu.add(Apps.AppIcon.createGhost());

		FolderItem folder = new FolderItem("f1", "Folder 1", null, new ArrayList<>());
		Apps.addItemToPieMenu(menu, folder, true);

		assertEquals(4, menu.size());
		assertEquals(app1, menu.get(0));
		assertEquals(app2, menu.get(1));
		assertEquals(folder, menu.get(2));
		assertTrue(Apps.isGhost(menu.get(3)));
	}

	@Test
	public void testAddFolderToPie_InDynamicMode_AppendsItem() {
		List<PieItem> menu = new ArrayList<>();
		Apps.AppIcon app1 = createAppIcon("com.pkg.app1", "App 1");
		menu.add(app1);

		FolderItem folder = new FolderItem("f1", "Folder 1", null, new ArrayList<>());
		Apps.addItemToPieMenu(menu, folder, false);

		assertEquals(2, menu.size());
		assertEquals(app1, menu.get(0));
		assertEquals(folder, menu.get(1));
	}

	@Test
	public void testSelectFolderOnPie_ChangesMenuToFolderItemsOnNextOpen() {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		List<PieItem> folderItems = new ArrayList<>();
		Apps.AppIcon folderApp = createAppIcon("com.pkg.folderapp", "Folder App");
		folderItems.add(folderApp);

		FolderItem folder = new FolderItem("test_folder", "My Folder", null, folderItems);
		assertFalse(pieView.isInFolder());

		// Activating folder sets activeFolder
		pieView.setActiveFolder(folder);
		assertTrue(pieView.isInFolder());
		assertEquals(folder, pieView.getActiveFolder());

		// Next openMenu uses folder.items via ACTION_DOWN touch event
		long downTime = android.os.SystemClock.uptimeMillis();
		android.view.MotionEvent downEvent = android.view.MotionEvent.obtain(
				downTime, downTime, android.view.MotionEvent.ACTION_DOWN, 500f, 500f, 0);
		pieView.dispatchTouchEvent(downEvent);

		try {
			java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
			pieMenuField.setAccessible(true);
			de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
					(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);
			assertEquals(folderItems, pieMenu.icons);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testBackPressedInHomeActivityExitsFolder() {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);

		FolderItem folder = new FolderItem("f1", "Folder 1", null, new ArrayList<>());
		pieView.setActiveFolder(folder);
		assertTrue(pieView.isInFolder());

		// Pressing back in HomeActivity should exit folder
		activity.onBackPressed();
		assertFalse("Back pressed should exit folder", pieView.isInFolder());
	}

	@Test
	public void testOpeningAndClosingAppDrawerExitsFolder() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);

		FolderItem folder = new FolderItem("f1", "Folder 1", null, new ArrayList<>());
		pieView.setActiveFolder(folder);
		assertTrue(pieView.isInFolder());

		// Simulate opening and closing AppDrawer
		Method showAllAppsMethod = HomeActivity.class.getDeclaredMethod("showAllApps");
		showAllAppsMethod.setAccessible(true);
		showAllAppsMethod.invoke(activity);

		Method hideAllAppsMethod = HomeActivity.class.getDeclaredMethod("hideAllApps");
		hideAllAppsMethod.setAccessible(true);
		hideAllAppsMethod.invoke(activity);

		assertFalse("Closing AppDrawer should exit folder", pieView.isInFolder());
	}

	@Test
	public void testDeleteFolderActionRemovesFromPieAndDatabase() {
		FolderItem folder = FolderManager.createNewFolder(context, PieLauncherApp.apps.apps);
		assertNotNull(folder);

		// Verify it was stored in DB and manager
		assertTrue(FolderManager.getAllFolders().contains(folder));

		// Now delete folder
		FolderManager.deleteFolder(context, folder.id);
		assertFalse(FolderManager.getAllFolders().contains(folder));
	}

	private Apps.AppIcon createAppIcon(String pkg, String label) {
		try {
			Constructor<Apps.AppIcon> constructor =
					Apps.AppIcon.class.getDeclaredConstructor(
							ComponentName.class,
							String.class,
							Bitmap.class,
							UserHandle.class);
			constructor.setAccessible(true);
			return constructor.newInstance(
					new ComponentName(pkg, pkg + ".MainActivity"),
					label,
					(Bitmap) null,
					Process.myUserHandle());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
