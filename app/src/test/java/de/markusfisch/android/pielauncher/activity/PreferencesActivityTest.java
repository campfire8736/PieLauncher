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
		PreferenceView view = activity.findViewById(R.id.rigid_pie_menu);
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
}
