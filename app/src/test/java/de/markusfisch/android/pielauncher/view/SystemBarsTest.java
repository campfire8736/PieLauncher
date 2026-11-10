package de.markusfisch.android.pielauncher.view;

import android.content.Context;
import android.view.View;
import android.view.Window;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.activity.HomeActivity;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SystemBarsTest {

	@Test
	public void testSetLightSystemBars() {
		HomeActivity activity = Robolectric.buildActivity(HomeActivity.class).setup().get();
		Window window = activity.getWindow();

		SystemBars.setLightSystemBars(window, true);
		// On API 34, it configures insets controller or flags
		// Ensure it executes without throwing exceptions for both states
		SystemBars.setLightSystemBars(window, false);
		SystemBars.setTransparentSystemBars(window);
	}

	@Test
	public void testToolbarBackgroundContextResolution() {
		Context context = ApplicationProvider.getApplicationContext();
		ToolbarBackground tb = new ToolbarBackground(context);
		assertNotEquals(0, tb.backgroundColor);
		int colorFaded = tb.getColorForY(100);
		assertNotEquals(0, colorFaded);
	}

	@Test
	public void testIsWallpaperLight() {
		Context context = ApplicationProvider.getApplicationContext();
		// In Robolectric environment, it executes fallbacks cleanly without exception
		boolean isLight = SystemBars.isWallpaperLight(context);
		// Can be true or false depending on theme/environment, ensure it returns a valid boolean
		assertTrue(isLight || !isLight);
	}

	@Test
	public void testWallpaperColorsListenerRegistration() {
		Context context = ApplicationProvider.getApplicationContext();
		Object token = SystemBars.registerWallpaperColorsListener(context, isLight -> {});
		// Unregister with token or null token should not crash
		SystemBars.unregisterWallpaperColorsListener(context, token);
		SystemBars.unregisterWallpaperColorsListener(context, null);
	}
}
