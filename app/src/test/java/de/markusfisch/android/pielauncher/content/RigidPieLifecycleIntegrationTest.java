package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RigidPieLifecycleIntegrationTest {

	private Context context;
	private Preferences prefs;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		prefs = PieLauncherApp.getPrefs(context);
		PieLauncherApp.apps.menuPrimary.clear();
		PieLauncherApp.apps.menuSecondary.clear();
	}

	@Test
	public void testRemoveItemFromPieMenu_InRigidMode_LeavesGhostAndPreservesGeometry() {
		List<PieItem> menu = new ArrayList<>();
		Apps.AppIcon icon1 = createAppIcon("com.app1", "App 1");
		Apps.AppIcon icon2 = createAppIcon("com.app2", "App 2");
		Apps.AppIcon icon3 = createAppIcon("com.app3", "App 3");
		Apps.AppIcon icon4 = createAppIcon("com.app4", "App 4");

		menu.add(icon1);
		menu.add(icon2);
		menu.add(icon3);
		menu.add(icon4);

		Apps.removeItemFromPieMenu(menu, icon2, true);

		assertEquals(4, menu.size());
		assertEquals(icon1, menu.get(0));
		assertTrue("Slot 1 should now be a ghost", Apps.isGhost(menu.get(1)));
		assertEquals(icon3, menu.get(2));
		assertEquals(icon4, menu.get(3));
	}

	@Test
	public void testRemoveItemFromPieMenu_InStandardMode_RemovesItem() {
		List<PieItem> menu = new ArrayList<>();
		Apps.AppIcon icon1 = createAppIcon("com.app1", "App 1");
		Apps.AppIcon icon2 = createAppIcon("com.app2", "App 2");

		menu.add(icon1);
		menu.add(icon2);

		Apps.removeItemFromPieMenu(menu, icon1, false);

		assertEquals(1, menu.size());
		assertEquals(icon2, menu.get(0));
	}

	@Test
	public void testRemovePackageAsync_InRigidMode_PreservesRigidSlots() throws Exception {
		prefs.setRigidPie(true);

		Apps.AppIcon icon1 = createAppIcon("com.pkg.keep", "Keep App");
		Apps.AppIcon icon2 = createAppIcon("com.pkg.remove", "Remove App");

		PieLauncherApp.apps.menuPrimary.add(icon1);
		PieLauncherApp.apps.menuPrimary.add(icon2);

		PieLauncherApp.apps.removePackageAsync(context, "com.pkg.remove", Process.myUserHandle());

		// Allow background DB task and UI handler to run
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
		Thread.sleep(200);
		org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		assertEquals(2, PieLauncherApp.apps.menuPrimary.size());
		assertEquals(icon1, PieLauncherApp.apps.menuPrimary.get(0));
		assertTrue("Removed package should be replaced with ghost in rigid mode",
				Apps.isGhost(PieLauncherApp.apps.menuPrimary.get(1)));
	}

	@Test
	public void testDeleteShortcutInAppPieView_InRigidMode_LeavesGhostAndPreservesSlots() throws Exception {
		prefs.setRigidPie(true);

		Apps.AppIcon icon1 = createAppIcon("com.pkg.keep", "Keep App");
		Apps.AppIcon shortcut = createAppIcon("com.example.shortcut", "Shortcut");
		shortcut.shortcutId = "s1";
		shortcut.shortcutPackage = "com.example.shortcut";

		PieLauncherApp.apps.menuPrimary.add(icon1);
		PieLauncherApp.apps.menuPrimary.add(shortcut);

		android.app.Activity activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class)
				.create().start().resume().visible().get();
		de.markusfisch.android.pielauncher.widget.AppPieView appPieView =
				new de.markusfisch.android.pielauncher.widget.AppPieView(activity, null);
		appPieView.layout(0, 0, 1080, 2280);
		appPieView.showEditor();

		java.lang.reflect.Method editIconMethod = de.markusfisch.android.pielauncher.widget.AppPieView.class
				.getDeclaredMethod("editIcon", PieItem.class);
		editIconMethod.setAccessible(true);
		editIconMethod.invoke(appPieView, shortcut);

		java.lang.reflect.Field editorRendererField = de.markusfisch.android.pielauncher.widget.AppPieView.class
				.getDeclaredField("editorRenderer");
		editorRendererField.setAccessible(true);
		de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer renderer =
				(de.markusfisch.android.pielauncher.widget.renderer.EditorRenderer) editorRendererField.get(appPieView);

		// Hover action 1 (delete_shortcut)
		java.lang.reflect.Field touchField = de.markusfisch.android.pielauncher.widget.AppPieView.class
				.getDeclaredField("touch");
		touchField.setAccessible(true);
		android.graphics.Point touchPoint = (android.graphics.Point) touchField.get(appPieView);
		android.graphics.Rect actionRect = renderer.grabbedActionRects.get(1);
		touchPoint.set(actionRect.centerX(), actionRect.centerY());

		// Perform action
		java.lang.reflect.Method performEditActionMethod = de.markusfisch.android.pielauncher.widget.AppPieView.class
				.getDeclaredMethod("performEditAction", Context.class);
		performEditActionMethod.setAccessible(true);
		boolean result = (boolean) performEditActionMethod.invoke(appPieView, context);
		assertTrue(result);

		// Now simulate touch release
		appPieView.releaseIcon();

		// Check menuPrimary: size must still be 2, index 0 is icon1, index 1 is ghost!
		assertEquals(2, PieLauncherApp.apps.menuPrimary.size());
		assertEquals(icon1, PieLauncherApp.apps.menuPrimary.get(0));
		assertTrue("Shortcut slot must be preserved as ghost in rigid mode",
				Apps.isGhost(PieLauncherApp.apps.menuPrimary.get(1)));
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
