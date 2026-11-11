package de.markusfisch.android.pielauncher.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.widget.CheckBox;
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
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.PickPieAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.FolderItem;
import de.markusfisch.android.pielauncher.content.FolderManager;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;
import de.markusfisch.android.pielauncher.preference.Preferences;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class PickPieAppsActivityTest {
	private Apps.AppIcon appA;
	private Apps.AppIcon appB;
	private Apps.AppIcon appC;

	@Before
	public void setUp() throws Exception {
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
		Context context = ApplicationProvider.getApplicationContext();
		PieLauncherApp.getPrefs(context).setSkipSetup();

		Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
		Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
				ComponentName.class, String.class, Bitmap.class, UserHandle.class);
		constructor.setAccessible(true);

		appA = constructor.newInstance(
				new ComponentName("com.test.a", "ActivityA"),
				"App A", bitmap, Process.myUserHandle());
		appB = constructor.newInstance(
				new ComponentName("com.test.b", "ActivityB"),
				"App B", bitmap, Process.myUserHandle());
		appC = constructor.newInstance(
				new ComponentName("com.test.c", "ActivityC"),
				"App C", bitmap, Process.myUserHandle());

		PieLauncherApp.apps.apps.clear();
		PieLauncherApp.apps.apps.put(new LauncherItemKey(appA.componentName, appA.userHandle), appA);
		PieLauncherApp.apps.apps.put(new LauncherItemKey(appB.componentName, appB.userHandle), appB);
		PieLauncherApp.apps.apps.put(new LauncherItemKey(appC.componentName, appC.userHandle), appC);

		PieLauncherApp.apps.menuPrimary.clear();
		PieLauncherApp.apps.menuPrimary.add(appA);
		PieLauncherApp.apps.menuPrimary.add(appB);
	}

	@After
	public void tearDown() {
		PieLauncherApp.apps.apps.clear();
		PieLauncherApp.apps.menuPrimary.clear();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
	}

	@Test
	public void testInitialSelectionMatchesPieMenu() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		assertNotNull(gridView);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();
		assertNotNull(adapter);
		assertEquals(3, adapter.getCount());

		// Verify initial checkbox states
		View view0 = adapter.getView(0, null, gridView);
		CheckBox cb0 = view0.findViewById(R.id.selected);
		assertTrue(cb0.isChecked()); // App A is in menuPrimary
		assertEquals(0.5f, view0.findViewById(R.id.icon).getAlpha(), 0.01f);
		android.widget.FrameLayout.LayoutParams lp0 = (android.widget.FrameLayout.LayoutParams) cb0.getLayoutParams();
		assertEquals(android.view.Gravity.BOTTOM | android.view.Gravity.END, lp0.gravity);
		assertTrue(lp0.bottomMargin < 0);
		assertTrue(lp0.rightMargin < 0 || lp0.getMarginEnd() < 0);

		View view1 = adapter.getView(1, null, gridView);
		CheckBox cb1 = view1.findViewById(R.id.selected);
		assertTrue(cb1.isChecked()); // App B is in menuPrimary
		assertEquals(0.5f, view1.findViewById(R.id.icon).getAlpha(), 0.01f);

		View view2 = adapter.getView(2, null, gridView);
		CheckBox cb2 = view2.findViewById(R.id.selected);
		assertFalse(cb2.isChecked()); // App C is not in menuPrimary
		assertEquals(1.0f, view2.findViewById(R.id.icon).getAlpha(), 0.01f);
	}

	@Test
	public void testItemClickTogglesSelection() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Click App C (position 2) -> should become selected
		gridView.performItemClick(null, 2, adapter.getItemId(2));
		View view2 = adapter.getView(2, null, gridView);
		CheckBox cb2 = view2.findViewById(R.id.selected);
		assertTrue(cb2.isChecked());
		assertEquals(0.5f, view2.findViewById(R.id.icon).getAlpha(), 0.01f);

		// Click App A (position 0) -> should become unselected
		gridView.performItemClick(null, 0, adapter.getItemId(0));
		View view0 = adapter.getView(0, null, gridView);
		CheckBox cb0 = view0.findViewById(R.id.selected);
		assertFalse(cb0.isChecked());
		assertEquals(1.0f, view0.findViewById(R.id.icon).getAlpha(), 0.01f);
	}

	@Test
	public void testBackButtonShowsDiscardDialogWhenModified() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Toggle App C (add)
		gridView.performItemClick(null, 2, adapter.getItemId(2));

		// Click Back
		activity.findViewById(R.id.back).performClick();

		// Dialog should be shown asking to discard changes
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		assertFalse(activity.isFinishing());

		// Cancel -> stays open
		dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
		assertFalse(activity.isFinishing());

		// Click Back again and confirm discard
		activity.findViewById(R.id.back).performClick();
		dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());
		// Pie menu should remain untouched: [App A, App B]
		assertEquals(2, PieLauncherApp.apps.menuPrimary.size());
		assertEquals(appA, PieLauncherApp.apps.menuPrimary.get(0));
		assertEquals(appB, PieLauncherApp.apps.menuPrimary.get(1));
	}

	@Test
	public void testApplyButtonShowsSummaryDialogWhenModified() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Deselect App A (position 0)
		gridView.performItemClick(null, 0, adapter.getItemId(0));
		// Select App C (position 2)
		gridView.performItemClick(null, 2, adapter.getItemId(2));

		// Click Apply -> should show summary dialog
		activity.findViewById(R.id.apply).performClick();

		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		assertFalse(activity.isFinishing());

		// Verify summary lists added (App C) and removed (App A)
		android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
		assertNotNull(messageView);
		String message = messageView.getText().toString();
		assertTrue(message.contains(appC.label));
		assertTrue(message.contains(appA.label));

		// Click Cancel -> stays open
		dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
		assertFalse(activity.isFinishing());

		// Click Apply again and confirm
		activity.findViewById(R.id.apply).performClick();
		dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());
		// Pie menu should have App B (kept, position preserved) and App C (appended)
		assertEquals(2, PieLauncherApp.apps.menuPrimary.size());
		assertEquals(appB, PieLauncherApp.apps.menuPrimary.get(0));
		assertEquals(appC, PieLauncherApp.apps.menuPrimary.get(1));
	}

	@Test
	public void testBackWithoutModificationsFinishesImmediately() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		// Click Back without changes
		activity.findViewById(R.id.back).performClick();

		assertTrue(activity.isFinishing());
	}

	@Test
	public void testApplyWithoutModificationsFinishesImmediately() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		// Click Apply without changes
		activity.findViewById(R.id.apply).performClick();

		assertTrue(activity.isFinishing());
	}

	@Test
	public void testOnBackPressedShowsDiscardDialogWhenModified() {
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Toggle App C (add)
		gridView.performItemClick(null, 2, adapter.getItemId(2));

		// Press back
		activity.onBackPressed();

		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		assertFalse(activity.isFinishing());

		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
		assertTrue(activity.isFinishing());
	}

	@Test
	public void testToolbarBackgroundInLightMode() {
		Context context = ApplicationProvider.getApplicationContext();
		PieLauncherApp.getPrefs(context).setDisplayTheme(Preferences.THEME_LIGHT);

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		View toolbar = activity.findViewById(R.id.toolbar);
		assertNotNull(toolbar);
		android.graphics.drawable.ColorDrawable bg = (android.graphics.drawable.ColorDrawable) toolbar.getBackground();
		assertNotNull(bg);
		assertEquals(0xe8ffffff, bg.getColor());

		controller.destroy();
	}

	private Apps.AppIcon createAppIcon(String name, String pkg) {
		return createAppIcon(name, pkg, "Activity");
	}

	private Apps.AppIcon createAppIcon(String name, String pkg, String cls) {
		try {
			Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
			Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
					ComponentName.class, String.class, Bitmap.class, UserHandle.class);
			constructor.setAccessible(true);
			return constructor.newInstance(
					new ComponentName(pkg, cls),
					name, bitmap, Process.myUserHandle());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testSectionsSeparatedForAppsAndShortcuts() {
		Apps.AppIcon shortcut = createAppIcon("Search Shortcut", "com.test.sc");
		shortcut.shortcutId = "sc_1";
		shortcut.shortcutPackage = "com.test.sc";
		PieLauncherApp.apps.apps.put(new LauncherItemKey(shortcut.componentName, shortcut.userHandle), shortcut);

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		android.widget.LinearLayout sectionsContainer = activity.findViewById(R.id.sections_container);
		assertNotNull(sectionsContainer);

		// With 2 sections, sectionsContainer should contain 4 views:
		// child 0: Header "Android Apps"
		// child 1: GridView for Android Apps (App A, App B, App C)
		// child 2: Header "App shortcuts"
		// child 3: GridView for App shortcuts (Search Shortcut)
		assertEquals(4, sectionsContainer.getChildCount());

		android.widget.TextView header0 = (android.widget.TextView) sectionsContainer.getChildAt(0);
		assertEquals("Android Apps", header0.getText().toString());

		GridView grid0 = (GridView) sectionsContainer.getChildAt(1);
		assertEquals(3, grid0.getAdapter().getCount());

		android.widget.TextView header1 = (android.widget.TextView) sectionsContainer.getChildAt(2);
		assertEquals("App shortcuts", header1.getText().toString());

		GridView grid1 = (GridView) sectionsContainer.getChildAt(3);
		assertEquals(1, grid1.getAdapter().getCount());
		assertEquals(shortcut, grid1.getAdapter().getItem(0));

		// Click the shortcut in section 1 to select it
		grid1.performItemClick(null, 0, grid1.getAdapter().getItemId(0));

		// Click Apply
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		// Confirm apply
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());
		// Target menu should now have App A, App B, and the shortcut!
		assertTrue(PieLauncherApp.apps.menuPrimary.contains(shortcut));
	}

	@Test
	public void testModularCustomSectionInSelectionScreen() {
		de.markusfisch.android.pielauncher.content.DrawerSection customSection =
				new de.markusfisch.android.pielauncher.content.DrawerSection() {
					@Override
					public String getId() {
						return "custom_section";
					}

					@Override
					public CharSequence getTitle(Context context) {
						return "Custom Items";
					}

					@Override
					public boolean matches(Apps.AppIcon item) {
						return item != null && "App C".equals(item.label);
					}
				};

		de.markusfisch.android.pielauncher.content.DrawerSectionRegistry.registerSection(0, customSection);

		try {
			Intent intent = new Intent();
			intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

			ActivityController<PickPieAppsActivity> controller =
					Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
			PickPieAppsActivity activity = controller.get();

			android.widget.LinearLayout sectionsContainer = activity.findViewById(R.id.sections_container);
			assertNotNull(sectionsContainer);

			// Section 0 is "Custom Items" (App C)
			android.widget.TextView header0 = (android.widget.TextView) sectionsContainer.getChildAt(0);
			assertEquals("Custom Items", header0.getText().toString());
			GridView grid0 = (GridView) sectionsContainer.getChildAt(1);
			assertEquals(1, grid0.getAdapter().getCount());
			assertEquals(appC, grid0.getAdapter().getItem(0));

			// Section 1 is "Android Apps" (App A, App B)
			android.widget.TextView header1 = (android.widget.TextView) sectionsContainer.getChildAt(2);
			assertEquals("Android Apps", header1.getText().toString());
			GridView grid1 = (GridView) sectionsContainer.getChildAt(3);
			assertEquals(2, grid1.getAdapter().getCount());
		} finally {
			de.markusfisch.android.pielauncher.content.DrawerSectionRegistry.resetToDefaults();
		}
	}

	@Test
	public void testWarningVisibilityBasedOnSelectionCount() throws Exception {
		Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
		java.lang.reflect.Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
				ComponentName.class, String.class, Bitmap.class, UserHandle.class);
		constructor.setAccessible(true);

		// Add 10 apps to apps map
		PieLauncherApp.apps.apps.clear();
		PieLauncherApp.apps.menuPrimary.clear();
		List<Apps.AppIcon> createdApps = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Apps.AppIcon icon = constructor.newInstance(
					new ComponentName("com.test.app" + i, "Activity" + i),
					"App " + i, bitmap, Process.myUserHandle());
			PieLauncherApp.apps.apps.put(new LauncherItemKey(icon.componentName, icon.userHandle), icon);
			createdApps.add(icon);
		}

		// Initially select 8 apps
		for (int i = 0; i < 8; i++) {
			PieLauncherApp.apps.menuPrimary.add(createdApps.get(i));
		}

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_IS_PRIMARY, true);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		View warningContainer = activity.findViewById(R.id.warning_container);
		assertNotNull(warningContainer);
		// 8 items selected -> warning should be GONE
		assertEquals(View.GONE, warningContainer.getVisibility());

		GridView gridView = activity.findViewById(R.id.apps);
		assertNotNull(gridView);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Find an unselected app (e.g. index 8) and click it -> 9 items selected
		gridView.performItemClick(null, 8, adapter.getItemId(8));

		// Warning should now be VISIBLE
		assertEquals(View.VISIBLE, warningContainer.getVisibility());

		// Clicking Apply should show the warning in the dialog message
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
		assertNotNull(messageView);
		assertTrue(messageView.getText().toString().contains(
				activity.getString(R.string.warning_pie_too_many_items)));

		// Dismiss dialog
		dialog.dismiss();

		// Deselect an app (e.g. index 8) -> back to 8 items
		gridView.performItemClick(null, 8, adapter.getItemId(8));

		// Warning should be GONE again
		assertEquals(View.GONE, warningContainer.getVisibility());
	}

	@Test
	public void testFolderSelection_InitializesFromFolderAndAppliesToFolderNotSecondary() {
		Context context = ApplicationProvider.getApplicationContext();

		FolderItem folder = FolderManager.createNewFolder(context, PieLauncherApp.apps.apps);
		folder.items.clear();
		folder.items.add(appA);

		PieLauncherApp.apps.menuSecondary.clear();
		PieLauncherApp.apps.menuSecondary.add(appB);

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_ID, folder.id);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		assertNotNull(gridView);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();
		assertNotNull(adapter);

		// Verify appA is selected (index 0) and appB is unselected (index 1) in folder selection
		View view0 = adapter.getView(0, null, gridView);
		CheckBox cb0 = view0.findViewById(R.id.selected);
		assertTrue(cb0.isChecked());

		View view1 = adapter.getView(1, null, gridView);
		CheckBox cb1 = view1.findViewById(R.id.selected);
		assertFalse(cb1.isChecked());

		// Toggle appC (index 2) to select it
		gridView.performItemClick(null, 2, adapter.getItemId(2));

		// Apply changes
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());

		// Verify folder items updated to include appA and appC
		assertEquals(2, folder.items.size());
		assertTrue(folder.items.contains(appA));
		assertTrue(folder.items.contains(appC));
		assertFalse(folder.items.contains(appB));

		// Verify menuSecondary remains unchanged with only appB
		assertEquals(1, PieLauncherApp.apps.menuSecondary.size());
		assertTrue(PieLauncherApp.apps.menuSecondary.contains(appB));
	}

	@Test
	public void testAppReassignation_ShowsMovedFromInConfirmationAndRemovesFromPreviousLocation() {
		Context context = ApplicationProvider.getApplicationContext();

		// Put appA on the Main Menu and store it
		PieLauncherApp.apps.menuPrimary.clear();
		PieLauncherApp.apps.menuPrimary.add(appA);
		PieLauncherApp.getDatabase(context).storeMenu("menu", PieLauncherApp.apps.menuPrimary);

		// Create a folder
		FolderItem folder = FolderManager.createNewFolder(context, PieLauncherApp.apps.apps);
		folder.items.clear();

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_ID, folder.id);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		assertNotNull(gridView);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();
		assertNotNull(adapter);

		// View for appA (index 0) should show location as "Main Menu"
		View view0 = adapter.getView(0, null, gridView);
		android.widget.TextView locationView0 = view0.findViewById(R.id.location);
		assertNotNull(locationView0);
		assertEquals(View.VISIBLE, locationView0.getVisibility());
		assertEquals(activity.getString(R.string.main_pie_menu), locationView0.getText().toString());

		// Select appA (click index 0) to add it to the folder
		gridView.performItemClick(null, 0, adapter.getItemId(0));

		// Click Apply to trigger confirmation dialog
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);

		android.widget.TextView messageView = dialog.findViewById(android.R.id.message);
		assertNotNull(messageView);
		String message = messageView.getText().toString();

		// Verify confirmation dialog displays where appA is moving from
		assertTrue(message.contains(activity.getString(R.string.moved_from, activity.getString(R.string.main_pie_menu))));

		// Confirm dialog
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());

		// Verify appA was added to the folder
		assertTrue(folder.items.contains(appA));

		// Verify appA was removed from the Main Menu
		assertFalse(PieLauncherApp.apps.menuPrimary.contains(appA));

		// Verify database assignments reflect the reassignation
		Map<String, String> assignments = PieLauncherApp.getDatabase(context).getPrimaryMenuFolderAssignments();
		assertEquals(folder.id, assignments.get(appA.getItemKey(context)));
	}

	@Test
	public void testAddingAppToFolder_DoesNotDuplicateExistingFolderItems() {
		Context context = ApplicationProvider.getApplicationContext();

		// Simulate folder containing AppIcon instances created prior to re-indexing (distinct objects from appA, appB)
		Apps.AppIcon folderAppA = createAppIcon("App A", "com.test.a", "ActivityA");
		Apps.AppIcon folderAppB = createAppIcon("App B", "com.test.b", "ActivityB");

		FolderItem folder = FolderManager.createNewFolder(context, PieLauncherApp.apps.apps);
		folder.items.clear();
		folder.items.add(folderAppA);
		folder.items.add(folderAppB);

		// Selection screen opens with appA and appB already in folder, and user toggles appC
		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_ID, folder.id);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Toggle appC (index 2) to add it
		gridView.performItemClick(null, 2, adapter.getItemId(2));

		// Apply changes
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());

		// Verify folder items contain exactly 3 items: appA, appB, appC without any duplication
		assertEquals(3, folder.items.size());
		assertTrue(folder.items.contains(appA));
		assertTrue(folder.items.contains(appB));
		assertTrue(folder.items.contains(appC));
	}

	@Test
	public void testCreateFolder_DiscardDoesNotCreateFolder() {
		Context context = ApplicationProvider.getApplicationContext();
		int initialFolderCount = FolderManager.getAllFolders().size();
		int initialPrimaryCount = PieLauncherApp.apps.menuPrimary.size();

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_CREATE_FOLDER, true);
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_NAME, "Unsaved Folder");
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_COLOR, 0xff009688);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Toggle appA (index 0)
		gridView.performItemClick(null, 0, adapter.getItemId(0));

		// Press back to trigger discard dialog
		activity.onBackPressed();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick(); // Discard
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());

		// Verify no new folder was created or added to primary menu
		assertEquals(initialFolderCount, FolderManager.getAllFolders().size());
		assertEquals(initialPrimaryCount, PieLauncherApp.apps.menuPrimary.size());
	}

	@Test
	public void testCreateFolder_ApplyCreatesFolderWithColorAndItems() {
		Context context = ApplicationProvider.getApplicationContext();
		int testColor = 0xffe91e63;

		Intent intent = new Intent();
		intent.putExtra(PickPieAppsActivity.EXTRA_CREATE_FOLDER, true);
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_NAME, "My New Folder");
		intent.putExtra(PickPieAppsActivity.EXTRA_FOLDER_COLOR, testColor);

		ActivityController<PickPieAppsActivity> controller =
				Robolectric.buildActivity(PickPieAppsActivity.class, intent).setup();
		PickPieAppsActivity activity = controller.get();

		GridView gridView = activity.findViewById(R.id.apps);
		PickPieAppsAdapter adapter = (PickPieAppsAdapter) gridView.getAdapter();

		// Toggle appA (index 0) and appB (index 1)
		gridView.performItemClick(null, 0, adapter.getItemId(0));
		gridView.performItemClick(null, 1, adapter.getItemId(1));

		// Apply
		activity.findViewById(R.id.apply).performClick();
		android.app.AlertDialog dialog = (android.app.AlertDialog)
				org.robolectric.shadows.ShadowAlertDialog.getLatestDialog();
		assertNotNull(dialog);
		dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick(); // Apply
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertTrue(activity.isFinishing());

		// Find created folder in menuPrimary
		FolderItem createdFolder = null;
		for (de.markusfisch.android.pielauncher.content.PieItem item : PieLauncherApp.apps.menuPrimary) {
			if (item instanceof FolderItem && "My New Folder".equals(item.getLabel())) {
				createdFolder = (FolderItem) item;
				break;
			}
		}
		assertNotNull(createdFolder);
		assertEquals("My New Folder", createdFolder.getLabel());
		assertEquals(testColor, createdFolder.color);
		assertNotNull(createdFolder.bitmap);
		assertTrue(createdFolder.items.contains(appA));
		assertTrue(createdFolder.items.contains(appB));

		// Verify database also persisted it
		de.markusfisch.android.pielauncher.content.Database.FolderRecord record =
				PieLauncherApp.getDatabase(context).getFolder(createdFolder.id);
		assertNotNull(record);
		assertEquals("My New Folder", record.name);
		assertEquals(testColor, record.color);
	}
}
