package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;
import android.widget.GridView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.activity.HomeActivity;
import de.markusfisch.android.pielauncher.activity.PickPieAppsActivity;
import de.markusfisch.android.pielauncher.adapter.PickPieAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.AppPieView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

	@Test
	public void testOpeningAppDrawerFromFolder_ThenEnteringEditMode_DisplaysFolderContent() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		List<PieItem> folderItems = new ArrayList<>();
		Apps.AppIcon folderApp = createAppIcon("com.pkg.folderapp", "Folder App");
		folderItems.add(folderApp);

		FolderItem folder = new FolderItem("folder1", "Folder 1", null, folderItems);
		pieView.setActiveFolder(folder);
		assertTrue(pieView.isInFolder());

		// Open AppDrawer from folder
		Method showAllAppsMethod = HomeActivity.class.getDeclaredMethod("showAllApps");
		showAllAppsMethod.setAccessible(true);
		showAllAppsMethod.invoke(activity);
		assertTrue(pieView.inListMode());
		assertTrue(pieView.isInFolder());

		// Go to EditMode from AppDrawer (e.g. clicking edit button)
		Method showEditorMethod = HomeActivity.class.getDeclaredMethod("showEditor");
		showEditorMethod.setAccessible(true);
		showEditorMethod.invoke(activity);
		assertTrue(pieView.inEditMode());

		java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
		pieMenuField.setAccessible(true);
		de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
				(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);

		// EditMode must display the folder's items, not the main pie menu
		assertEquals(folderItems, pieMenu.icons);

		java.lang.reflect.Field rendererField = AppPieView.class.getDeclaredField("editorRenderer");
		rendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer editorRenderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) rendererField.get(pieView);

		// Add Folder must be disabled when editing a folder
		assertFalse(editorRenderer.isAddFolderEnabled());
	}

	@Test
	public void testGrabbingFolderInEditMode_DroppingOnEditAction_DisplaysFolderContentInEditMode() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		List<PieItem> folderItems = new ArrayList<>();
		Apps.AppIcon folderApp = createAppIcon("com.pkg.folderapp", "Folder App");
		folderItems.add(folderApp);

		FolderItem folder = new FolderItem("folder1", "Folder 1", null, folderItems);
		PieLauncherApp.apps.menuPrimary.add(folder);

		// Enter EditMode on primary menu with the folder
		Method editIconMethod = AppPieView.class.getDeclaredMethod("editIcon", PieItem.class);
		editIconMethod.setAccessible(true);
		editIconMethod.invoke(pieView, (PieItem) null);
		assertTrue(pieView.inEditMode());
		assertFalse(pieView.isInFolder());

		// Now grab the folder in EditMode
		editIconMethod.invoke(pieView, folder);

		java.lang.reflect.Field rendererField = AppPieView.class.getDeclaredField("editorRenderer");
		rendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer editorRenderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) rendererField.get(pieView);

		// Verify 2 grabbed actions: DeleteFolderAction and EditFolderAction
		assertEquals(2, editorRenderer.getActiveGrabbedActions().size());
		assertTrue(editorRenderer.getActiveGrabbedActions().get(0) instanceof DeleteFolderAction);
		assertTrue(editorRenderer.getActiveGrabbedActions().get(1) instanceof EditFolderAction);

		// Simulate dropping on the EditFolderAction (action index 1)
		android.graphics.Rect editActionRect = editorRenderer.grabbedActionRects.get(1);
		java.lang.reflect.Field touchField = AppPieView.class.getDeclaredField("touch");
		touchField.setAccessible(true);
		android.graphics.Point touchPoint = (android.graphics.Point) touchField.get(pieView);
		touchPoint.set(editActionRect.centerX(), editActionRect.centerY());

		Method performEditActionMethod = AppPieView.class.getDeclaredMethod("performEditAction", Context.class);
		performEditActionMethod.setAccessible(true);
		boolean handled = (boolean) performEditActionMethod.invoke(pieView, activity);
		assertTrue(handled);

		// Verify that EditMode now displays the folder's content
		assertTrue(pieView.isInFolder());
		assertEquals(folder, pieView.getActiveFolder());

		java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
		pieMenuField.setAccessible(true);
		de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
				(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);
		assertEquals(folderItems, pieMenu.icons);

		// Verify folder is still in main menu (not removed when edited)
		assertTrue(PieLauncherApp.apps.menuPrimary.contains(folder));
	}

	@Test
	public void testOpeningSelectionScreenFromFolderInEditMode_AppliesToFolderNotSecondary() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		Apps.AppIcon folderApp1 = createAppIcon("com.pkg.app1", "App 1");
		Apps.AppIcon folderApp2 = createAppIcon("com.pkg.app2", "App 2");
		Apps.AppIcon secondaryApp = createAppIcon("com.pkg.secondary", "Secondary App");

		PieLauncherApp.apps.apps.clear();
		PieLauncherApp.apps.apps.put(new LauncherItemKey(folderApp1.componentName, folderApp1.userHandle), folderApp1);
		PieLauncherApp.apps.apps.put(new LauncherItemKey(folderApp2.componentName, folderApp2.userHandle), folderApp2);
		PieLauncherApp.apps.apps.put(new LauncherItemKey(secondaryApp.componentName, secondaryApp.userHandle), secondaryApp);

		List<PieItem> folderItems = new ArrayList<>();
		folderItems.add(folderApp1);

		FolderManager.getOrCreateFolder(context, "test_folder_pick", PieLauncherApp.apps.apps);
		FolderItem cachedFolder = FolderManager.getOrCreateFolder(context, "test_folder_pick", PieLauncherApp.apps.apps);
		cachedFolder.items.clear();
		cachedFolder.items.add(folderApp1);

		PieLauncherApp.apps.menuSecondary.clear();
		PieLauncherApp.apps.menuSecondary.add(secondaryApp);
		PieLauncherApp.apps.store(context);

		// Enter folder and EditMode
		pieView.setActiveFolder(cachedFolder);
		Method showEditorMethod = HomeActivity.class.getDeclaredMethod("showEditor");
		showEditorMethod.setAccessible(true);
		showEditorMethod.invoke(activity);

		assertTrue(pieView.inEditMode());
		assertTrue(pieView.isInFolder());

		// Trigger Selection screen via action button (BACK_OR_REMOVE_ICON)
		java.lang.reflect.Field rendererField = AppPieView.class.getDeclaredField("editorRenderer");
		rendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer editorRenderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) rendererField.get(pieView);

		java.lang.reflect.Field touchField = AppPieView.class.getDeclaredField("touch");
		touchField.setAccessible(true);
		android.graphics.Point touchPoint = (android.graphics.Point) touchField.get(pieView);
		touchPoint.set(editorRenderer.iconStartRect.centerX(), editorRenderer.iconStartRect.centerY());

		Method performEditActionMethod = AppPieView.class.getDeclaredMethod("performEditAction", Context.class);
		performEditActionMethod.setAccessible(true);
		boolean handled = (boolean) performEditActionMethod.invoke(pieView, activity);
		assertTrue(handled);

		// Verify PickPieAppsActivity was started with EXTRA_FOLDER_ID
		Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
		assertNotNull(intent);
		assertEquals(PickPieAppsActivity.class.getName(), intent.getComponent().getClassName());
		assertEquals("test_folder_pick", intent.getStringExtra(PickPieAppsActivity.EXTRA_FOLDER_ID));

		// Run PickPieAppsActivity
		ActivityController<PickPieAppsActivity> pickController =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity pickActivity = pickController.get();

		GridView gridView = pickActivity.findViewById(R.id.apps);
		assertNotNull(gridView);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();
		assertNotNull(adapter);

		// Select folderApp2 (find its position in adapter)
		int app2Pos = -1;
		for (int i = 0; i < adapter.getCount(); i++) {
			if (folderApp2.equals(adapter.getItem(i))) {
				app2Pos = i;
				break;
			}
		}
		assertTrue(app2Pos >= 0);
		gridView.performItemClick(null, app2Pos, adapter.getItemId(app2Pos));

		// Apply selection
		pickActivity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
		assertTrue(pickActivity.isFinishing());

		// Resume HomeActivity
		controller.resume();

		// Verify HomeActivity is back in EditMode inside the folder
		assertTrue(pieView.inEditMode());
		assertTrue(pieView.isInFolder());
		assertEquals(cachedFolder, pieView.getActiveFolder());

		// Verify folder items contain both folderApp1 and folderApp2
		assertTrue(cachedFolder.items.contains(folderApp1));
		assertTrue(cachedFolder.items.contains(folderApp2));
		assertFalse(cachedFolder.items.contains(secondaryApp));

		// Verify menuSecondary is unchanged
		assertEquals(1, PieLauncherApp.apps.menuSecondary.size());
		assertTrue(PieLauncherApp.apps.menuSecondary.contains(secondaryApp));
	}

	@Test
	public void testFolderInEditMode_HidesSecondaryMenuSwitch_AndReturnsToMainPieOnReturnButtonClick() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		PieLauncherApp.getPrefs(context).setSplitPieMenu(Preferences.ALTERNATE_MENU_SECONDARY);

		List<PieItem> folderItems = new ArrayList<>();
		Apps.AppIcon folderApp = createAppIcon("com.pkg.folderapp", "Folder App");
		folderItems.add(folderApp);

		FolderItem folder = new FolderItem("folder_return_test", "Return Test Folder", null, folderItems);
		PieLauncherApp.apps.menuPrimary.add(folder);

		// Enter folder and EditMode
		pieView.setActiveFolder(folder);
		Method showEditorMethod = HomeActivity.class.getDeclaredMethod("showEditor");
		showEditorMethod.setAccessible(true);
		showEditorMethod.invoke(activity);

		assertTrue(pieView.inEditMode());
		assertTrue(pieView.isInFolder());
		assertEquals(folder, pieView.getActiveFolder());

		java.lang.reflect.Field rendererField = AppPieView.class.getDeclaredField("editorRenderer");
		rendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer editorRenderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) rendererField.get(pieView);

		assertTrue(editorRenderer.isInFolder());
		assertFalse(editorRenderer.isAddFolderEnabled());
		assertFalse(editorRenderer.iconCenterRect.isEmpty());

		// In a folder, iconCenterRect represents RETURN_TO_MAIN_PIE, not EDIT_SETTINGS_OR_UNDO
		java.lang.reflect.Field touchField = AppPieView.class.getDeclaredField("touch");
		touchField.setAccessible(true);
		android.graphics.Point touchPoint = (android.graphics.Point) touchField.get(pieView);
		touchPoint.set(editorRenderer.iconCenterRect.centerX(), editorRenderer.iconCenterRect.centerY());

		assertEquals(de.markusfisch.android.pielauncher.widget.state.EditorAction.RETURN_TO_MAIN_PIE,
				editorRenderer.getActionAt(touchPoint, false));

		Method performEditActionMethod = AppPieView.class.getDeclaredMethod("performEditAction", Context.class);
		performEditActionMethod.setAccessible(true);
		boolean handled = (boolean) performEditActionMethod.invoke(pieView, activity);
		assertTrue(handled);

		// Verify returned to main pie while remaining in EditMode
		assertFalse(pieView.isInFolder());
		assertNull(pieView.getActiveFolder());
		assertTrue(pieView.inEditMode());

		java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
		pieMenuField.setAccessible(true);
		de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
				(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);
		assertEquals(PieLauncherApp.apps.menuPrimary, pieMenu.icons);

		assertFalse(editorRenderer.isInFolder());
		assertTrue(editorRenderer.isAddFolderEnabled());
		assertEquals(de.markusfisch.android.pielauncher.widget.state.EditorAction.EDIT_SETTINGS_OR_UNDO,
				editorRenderer.getActionAt(touchPoint, false));

		// Test hardware back press inside a folder in EditMode also returns to main pie in EditMode
		pieView.setActiveFolder(folder);
		showEditorMethod.invoke(activity);
		assertTrue(pieView.inEditMode());
		assertTrue(pieView.isInFolder());

		activity.onBackPressed();
		assertFalse(pieView.isInFolder());
		assertNull(pieView.getActiveFolder());
		assertTrue(pieView.inEditMode());
		assertEquals(PieLauncherApp.apps.menuPrimary, pieMenu.icons);
	}

	@Test
	public void testLeavingEditModeFromFolder_ReturnsToMainPie() throws Exception {
		controller = Robolectric.buildActivity(HomeActivity.class).setup();
		activity = controller.get();
		AppPieView pieView = activity.findViewById(R.id.pie);
		pieView.layout(0, 0, 1080, 2280);

		List<PieItem> folderItems = new ArrayList<>();
		folderItems.add(createAppIcon("com.pkg.item", "Folder Item"));
		FolderItem folder = new FolderItem("test_folder", "My Folder", null, folderItems);

		pieView.setActiveFolder(folder);
		java.lang.reflect.Method showEditorMethod = HomeActivity.class.getDeclaredMethod("showEditor");
		showEditorMethod.setAccessible(true);
		showEditorMethod.invoke(activity);

		assertTrue(pieView.inEditMode());
		assertTrue(pieView.isInFolder());
		assertEquals(folder, pieView.getActiveFolder());

		// End edit mode directly (e.g. tapping END_EDIT_MODE or resetHomeState)
		pieView.endEditMode();

		assertFalse(pieView.inEditMode());
		assertFalse(pieView.isInFolder());
		assertNull(pieView.getActiveFolder());

		java.lang.reflect.Field pieMenuField = AppPieView.class.getDeclaredField("pieMenu");
		pieMenuField.setAccessible(true);
		de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
				(de.markusfisch.android.pielauncher.graphics.CanvasPieMenu) pieMenuField.get(pieView);
		assertEquals(PieLauncherApp.apps.menuPrimary, pieMenu.icons);
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
