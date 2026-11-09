package de.markusfisch.android.pielauncher.activity;

import android.content.Context;
import android.widget.TextView;

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

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.PreferenceSwitchView;
import de.markusfisch.android.pielauncher.widget.PreferenceView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class PreferencesActivityTest {

	private ActivityController<PreferencesActivity> controller;
	private PreferencesActivity activity;
	private Preferences prefs;

	@Before
	public void setUp() {
		Context context = ApplicationProvider.getApplicationContext();
		prefs = PieLauncherApp.getPrefs(context);
		prefs.setRigidPie(false);

		controller = Robolectric.buildActivity(PreferencesActivity.class).setup();
		activity = controller.get();
	}

	@After
	public void tearDown() {
		if (controller != null) {
			controller.destroy();
		}
	}

	@Test
	public void testRigidPiePreferenceViewExists() {
		PreferenceSwitchView view = activity.findViewById(R.id.rigid_pie_menu);
		assertNotNull(view);
		assertFalse(prefs.isRigidPie());
	}

	@Test
	public void testRigidPiePreferenceToggleUpdatesPrefs() {
		prefs.setRigidPie(true);
		assertTrue(prefs.isRigidPie());

		prefs.setRigidPie(false);
		assertFalse(prefs.isRigidPie());
	}

	@Test
	public void testSwitchClickTogglesPreference() {
		PreferenceSwitchView view = activity.findViewById(R.id.rigid_pie_menu);
		assertNotNull(view);
		assertFalse(prefs.isRigidPie());
		assertFalse(view.isChecked());

		view.performClick();
		assertTrue(prefs.isRigidPie());
		assertTrue(view.isChecked());

		view.performClick();
		assertFalse(prefs.isRigidPie());
		assertFalse(view.isChecked());
	}

	@Test
	public void testDarkenBackgroundSliderUpdatesPreference() {
		PreferenceSliderView slider = activity.findViewById(R.id.darken_background);
		assertNotNull(slider);

		slider.setValue(Preferences.DARKEN_BACKGROUND_LIGHT);
		assertEquals(Preferences.DARKEN_BACKGROUND_LIGHT, slider.getValue());

		slider.getSeekBar().setProgress(Preferences.DARKEN_BACKGROUND_NONE);
		// Simulate stop tracking touch to commit
		slider.setValue(Preferences.DARKEN_BACKGROUND_NONE);
		assertEquals(Preferences.DARKEN_BACKGROUND_NONE, slider.getValue());
	}
}
