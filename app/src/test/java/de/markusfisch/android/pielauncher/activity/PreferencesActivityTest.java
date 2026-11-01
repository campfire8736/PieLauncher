package de.markusfisch.android.pielauncher.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceActionView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceCardView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceChoiceView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceSwitchView;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PreferencesActivityTest {
	private Context context;
	private Preferences prefs;

	@Before
	public void setUp() {
		context = RuntimeEnvironment.getApplication();
		prefs = PieLauncherApp.getPrefs(context);
	}

	@Test
	public void testNormalModeInitialization() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		View welcomeView = activity.findViewById(R.id.welcome);
		assertEquals(View.GONE, welcomeView.getVisibility());

		View hideInWelcome = activity.findViewById(R.id.hide_in_welcome_mode);
		assertEquals(View.VISIBLE, hideInWelcome.getVisibility());

		PreferenceSwitchView displayKeyboard = activity.findViewById(R.id.display_keyboard);
		assertNotNull(displayKeyboard);
		assertEquals(prefs.displayKeyboard(), displayKeyboard.isChecked());

		PreferenceChoiceView orientation = activity.findViewById(R.id.orientation);
		assertNotNull(orientation);

		controller.destroy();
	}

	@Test
	public void testWelcomeModeInitialization() {
		Intent intent = new Intent(context, PreferencesActivity.class);
		intent.putExtra("welcome", true);

		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class, intent);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		View welcomeView = activity.findViewById(R.id.welcome);
		assertNotNull(welcomeView);
		assertEquals(View.VISIBLE, welcomeView.getVisibility());

		View hideInWelcome = activity.findViewById(R.id.hide_in_welcome_mode);
		assertEquals(View.GONE, hideInWelcome.getVisibility());

		View skipButton = activity.findViewById(R.id.skip);
		assertEquals(View.VISIBLE, skipButton.getVisibility());

		skipButton.performClick();
		assertTrue(activity.isFinishing());

		controller.destroy();
	}

	@Test
	public void testSwitchTogglesPreference() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		PreferenceSwitchView autoLaunch = activity.findViewById(R.id.auto_launch_matching);
		assertNotNull(autoLaunch);

		boolean initialState = prefs.autoLaunchMatching();
		autoLaunch.performClick();

		assertEquals(!initialState, prefs.autoLaunchMatching());
		assertEquals(!initialState, autoLaunch.isChecked());

		controller.destroy();
	}

	@Test
	public void testSliderUpdatesPreference() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		PreferenceSliderView blurSlider = activity.findViewById(R.id.blur_background);
		if (blurSlider != null && blurSlider.getVisibility() == View.VISIBLE) {
			blurSlider.setProgress(42);
			assertEquals(42, prefs.getBackgroundBlurRadius());
		}

		controller.destroy();
	}

	@Test
	public void testCardClipsToOutline() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		PreferenceCardView setupCard = activity.findViewById(R.id.setup_card);
		assertNotNull(setupCard);
		assertTrue(setupCard.getClipToOutline());

		controller.destroy();
	}

	@Test
	public void testChoiceViewClickIsHandled() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		PreferenceChoiceView orientation = activity.findViewById(R.id.orientation);
		assertNotNull(orientation);
		assertTrue(orientation.isClickable());
		assertTrue(orientation.hasOnClickListeners());

		controller.destroy();
	}

	@Test
	public void testActionViewClickLaunchesHiddenApps() {
		ActivityController<PreferencesActivity> controller =
				Robolectric.buildActivity(PreferencesActivity.class);
		PreferencesActivity activity = controller.create().start().resume().visible().get();

		PreferenceActionView hiddenApps = activity.findViewById(R.id.hidden_apps);
		assertNotNull(hiddenApps);
		assertTrue(hiddenApps.isClickable());
		assertTrue(hiddenApps.hasOnClickListeners());
		assertTrue(hiddenApps.performClick());

		controller.destroy();
	}
}
