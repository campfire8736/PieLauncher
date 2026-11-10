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
	public void testAddFolderToPie_ToastFormatsLabelCorrectly() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);

		Method addFolderMethod = AppPieView.class.getDeclaredMethod("addFolderToPie", Context.class);
		addFolderMethod.setAccessible(true);
		addFolderMethod.invoke(pieView, activity);

		String latestToast = org.robolectric.shadows.ShadowToast.getTextOfLatestToast();
		assertNotNull(latestToast);
		assertFalse("Toast should format %1$s placeholder", latestToast.contains("%1$s"));
		assertTrue("Toast should include folder name", latestToast.contains(activity.getString(R.string.folder)));
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

	@Test
	public void testSplitPieMenu_OverFolders_OpensAlternateOrFolderContent() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		// Enable split top/bottom pie menu with secondary menu
		prefs.setSplitPieMenu(Preferences.ALTERNATE_MENU_SECONDARY);
		List<PieItem> secondaryItems = new ArrayList<>();
		Apps.AppIcon secondaryApp = createAppIcon("com.pkg.secondary", "Secondary App");
		secondaryItems.add(secondaryApp);
		PieLauncherApp.apps.menuSecondary.addAll(secondaryItems);

		List<PieItem> folderItems = new ArrayList<>();
		Apps.AppIcon folderApp = createAppIcon("com.pkg.folderapp", "Folder App");
		folderItems.add(folderApp);

		FolderItem folder = new FolderItem("folder1", "Folder 1", null, folderItems);
		pieView.setActiveFolder(folder);
		assertTrue(pieView.isInFolder());

		java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
		pieMenuField.setAccessible(true);
		de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
				(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);

		// 1. Touch bottom half (e.g. y = 1600f) -> should open folder's items
		long time = android.os.SystemClock.uptimeMillis();
		pieView.dispatchTouchEvent(android.view.MotionEvent.obtain(
				time, time, android.view.MotionEvent.ACTION_DOWN, 540f, 1600f, 0));
		assertEquals(folderItems, pieMenu.icons);

		// Cancel touch 1
		pieView.dispatchTouchEvent(android.view.MotionEvent.obtain(
				time, time + 50, android.view.MotionEvent.ACTION_CANCEL, 540f, 1600f, 0));

		// 2. Touch top half (e.g. y = 500f) -> should open secondary menu
		time += 100;
		pieView.dispatchTouchEvent(android.view.MotionEvent.obtain(
				time, time, android.view.MotionEvent.ACTION_DOWN, 540f, 500f, 0));
		assertEquals(PieLauncherApp.apps.menuSecondary, pieMenu.icons);

		// Cancel touch 2
		pieView.dispatchTouchEvent(android.view.MotionEvent.obtain(
				time, time + 50, android.view.MotionEvent.ACTION_CANCEL, 540f, 500f, 0));

		// 3. Folder remains active, so touching bottom half again still opens folder items
		time += 100;
		pieView.dispatchTouchEvent(android.view.MotionEvent.obtain(
				time, time, android.view.MotionEvent.ACTION_DOWN, 540f, 1600f, 0));
		assertEquals(folderItems, pieMenu.icons);
	}

	@Test
	public void testEditMode_SecondaryMenu_DisablesAddFolder() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		// Enable split menu so secondary menu is available
		prefs.setSplitPieMenu(Preferences.ALTERNATE_MENU_SECONDARY);

		// Enter edit mode
		Method editIconMethod = AppPieView.class.getDeclaredMethod("editIcon", PieItem.class);
		editIconMethod.setAccessible(true);
		editIconMethod.invoke(pieView, (PieItem) null);

		java.lang.reflect.Field rendererField = AppPieView.class.getDeclaredField("editorRenderer");
		rendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer editorRenderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) rendererField.get(pieView);

		// Primary menu initially -> addFolder is enabled
		assertTrue(editorRenderer.isAddFolderEnabled());

		java.lang.reflect.Field touchField = AppPieView.class.getDeclaredField("touch");
		touchField.setAccessible(true);
		android.graphics.Point touchPoint = (android.graphics.Point) touchField.get(pieView);

		Method performEditActionMethod = AppPieView.class.getDeclaredMethod("performEditAction", Context.class);
		performEditActionMethod.setAccessible(true);

		// Switch to secondary menu via EDIT_SETTINGS_OR_UNDO action
		touchPoint.set(editorRenderer.iconCenterRect.centerX(), editorRenderer.iconCenterRect.centerY());
		performEditActionMethod.invoke(pieView, activity);

		// Now on secondary menu -> addFolder is disabled
		assertFalse(editorRenderer.isAddFolderEnabled());

		// Attempting ADD_FOLDER while on secondary menu must NOT add any items
		int secondarySizeBefore = PieLauncherApp.apps.menuSecondary.size();
		touchPoint.set(editorRenderer.iconFolderRect.centerX(), editorRenderer.iconFolderRect.centerY());
		performEditActionMethod.invoke(pieView, activity);
		assertEquals(secondarySizeBefore, PieLauncherApp.apps.menuSecondary.size());

		// Switch back to primary menu -> addFolder is enabled again
		touchPoint.set(editorRenderer.iconCenterRect.centerX(), editorRenderer.iconCenterRect.centerY());
		performEditActionMethod.invoke(pieView, activity);
		assertTrue(editorRenderer.isAddFolderEnabled());
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
