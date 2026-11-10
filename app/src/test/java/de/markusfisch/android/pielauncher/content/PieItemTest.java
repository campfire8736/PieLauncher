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
		assertEquals("com.example.test/com.example.test.MainActivity", testApp.getItemKey(context));
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
}
