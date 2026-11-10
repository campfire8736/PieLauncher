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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.PickPieAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
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
}
