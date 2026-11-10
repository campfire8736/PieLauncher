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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.action.AppActionHandler;
import de.markusfisch.android.pielauncher.widget.state.PieEditor;
import de.markusfisch.android.pielauncher.widget.touch.TapFlags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ModularPieItemIntegrationTest {

	private Context context;
	private Database database;
	private Bitmap testBitmap;

	/**
	 * Example custom PieItem representing a phone contact.
	 */
	public static class ContactPieItem extends PieItem {
		public final String name;
		public final String phone;
		public boolean launched = false;

		public ContactPieItem(String name, String phone, Bitmap icon) {
			super(icon);
			this.name = name;
			this.phone = phone;
		}

		@Override
		public String getItemKey(Context context) {
			return "contact:" + phone;
		}

		@Override
		public String getLabel() {
			return name;
		}

		@Override
		public boolean launch(Context context) {
			launched = true;
			return true;
		}
	}

	/**
	 * Example custom PieItem representing a system action (e.g. Flashlight toggle).
	 */
	public static class FlashlightShortcutPieItem extends PieItem {
		public static boolean flashlightToggled = false;

		public FlashlightShortcutPieItem(Bitmap icon) {
			super(icon);
		}

		@Override
		public String getItemKey(Context context) {
			return "system:flashlight";
		}

		@Override
		public String getLabel() {
			return "Flashlight";
		}

		@Override
		public boolean launch(Context context) {
			flashlightToggled = !flashlightToggled;
			return true;
		}
	}

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		database = new Database(context);
		testBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);

		FlashlightShortcutPieItem.flashlightToggled = false;

		// Register resolvers for the new modular pie item types
		PieItemRegistry.registerResolver("contact", (ctx, subKey, allApps) ->
				new ContactPieItem("Contact " + subKey, subKey, testBitmap));

		PieItemRegistry.registerResolver("system", (ctx, subKey, allApps) -> {
			if ("flashlight".equals(subKey)) {
				return new FlashlightShortcutPieItem(testBitmap);
			}
			return null;
		});
	}

	@After
	public void tearDown() {
		PieItemRegistry.unregisterResolver("contact");
		PieItemRegistry.unregisterResolver("system");
	}

	private Apps.AppIcon createTestAppIcon(String label, String pkg) {
		try {
			Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
					ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
			constructor.setAccessible(true);
			return constructor.newInstance(
					new ComponentName(pkg, "MainActivity"),
					label,
					testBitmap,
					Process.myUserHandle()
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testPolymorphicLaunchInAppActionHandler() {
		Preferences prefs = PieLauncherApp.getPrefs(context);
		prefs.setOpenListWith(Preferences.OPEN_LIST_WITH_ICON);

		List<PieItem> launchedIcons = new ArrayList<>();
		AppActionHandler handler = new AppActionHandler(prefs, new AppActionHandler.Listener() {
			@Override
			public void onLaunchingIcon(PieItem icon) {
				launchedIcons.add(icon);
			}

			@Override
			public boolean isListFiltered() {
				return false;
			}

			@Override
			public String getFilterQuery() {
				return "";
			}
		});

		ContactPieItem contact = new ContactPieItem("Alice", "123456", testBitmap);
		FlashlightShortcutPieItem flashlight = new FlashlightShortcutPieItem(testBitmap);

		TapFlags flags = new TapFlags();
		flags.wasTap = false;

		// Launch contact
		assertFalse(contact.launched);
		boolean handled = handler.performPieAction(context, flags, contact, true);
		assertTrue(handled);
		assertTrue(contact.launched);
		assertEquals(1, launchedIcons.size());
		assertEquals(contact, launchedIcons.get(0));

		// Launch flashlight
		assertFalse(FlashlightShortcutPieItem.flashlightToggled);
		handled = handler.performPieAction(context, flags, flashlight, true);
		assertTrue(handled);
		assertTrue(FlashlightShortcutPieItem.flashlightToggled);
		assertEquals(2, launchedIcons.size());
		assertEquals(flashlight, launchedIcons.get(1));
	}

	@Test
	public void testPieEditorSupportsModularPieItems() {
		CanvasPieMenu<PieItem> pieMenu = new CanvasPieMenu<>();
		PieEditor.Listener listener = new PieEditor.Listener() {
			@Override
			public void invalidate() {}

			@Override
			public void storeMenu() {}

			@Override
			public void releaseIcon() {}
		};
		PieEditor editor = new PieEditor(listener, pieMenu);

		Apps.AppIcon app = createTestAppIcon("Test App", "com.test.app");
		ContactPieItem contact = new ContactPieItem("Bob", "987654", testBitmap);
		FlashlightShortcutPieItem flashlight = new FlashlightShortcutPieItem(testBitmap);

		List<PieItem> initial = new ArrayList<>();
		initial.add(app);
		initial.add(contact);
		initial.add(flashlight);

		pieMenu.icons.addAll(initial);
		editor.beginEdit(initial);
		assertEquals(3, pieMenu.icons.size());

		// Grab contact item
		editor.editIcon(contact);
		assertTrue(editor.hasGrabbedIcon());
		assertEquals(contact, editor.getGrabbedIcon());

		// Swap order and commit
		pieMenu.icons.clear();
		pieMenu.icons.add(contact);
		pieMenu.icons.add(app);
		pieMenu.icons.add(flashlight);

		assertTrue(editor.isMenuChanged(pieMenu.icons));
		editor.commitGrabbedItem();
		assertFalse(editor.hasGrabbedIcon());
		editor.commit();
	}

	@Test
	public void testDatabaseRoundTripPreservesModularPieItems() throws Exception {
		Apps.AppIcon app = createTestAppIcon("App", "com.example.app");
		ContactPieItem contact = new ContactPieItem("Charlie", "555-1234", testBitmap);
		FlashlightShortcutPieItem flashlight = new FlashlightShortcutPieItem(testBitmap);

		Map<LauncherItemKey, Apps.AppIcon> allApps = new HashMap<>();
		allApps.put(new LauncherItemKey(app.componentName, app.userHandle), app);

		List<PieItem> menuToStore = new ArrayList<>();
		menuToStore.add(app);
		menuToStore.add(contact);
		menuToStore.add(flashlight);

		// Store menu in Database
		database.storeMenu("test_menu", menuToStore);

		// Wait for single-threaded database executor to complete
		java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
		database.execute(latch::countDown);
		assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS));

		// Restore menu from Database
		ArrayList<PieItem> restored = database.restoreMenu("test_menu", allApps);
		assertNotNull(restored);
		assertEquals(3, restored.size());

		// Item 0: AppIcon
		assertTrue(restored.get(0) instanceof Apps.AppIcon);
		assertEquals("App", restored.get(0).getLabel());

		// Item 1: ContactPieItem resolved via "contact:" prefix
		assertTrue(restored.get(1) instanceof ContactPieItem);
		ContactPieItem restoredContact = (ContactPieItem) restored.get(1);
		assertEquals("555-1234", restoredContact.phone);

		// Item 2: FlashlightShortcutPieItem resolved via "system:flashlight" key
		assertTrue(restored.get(2) instanceof FlashlightShortcutPieItem);
		assertEquals("Flashlight", restored.get(2).getLabel());
	}
}
