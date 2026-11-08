package de.markusfisch.android.pielauncher.activity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.widget.AppPieView;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HomeActivityTest {
    private ActivityController<HomeActivity> controller;
    private HomeActivity activity;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        PieLauncherApp.getPrefs(context).setSkipSetup();
        controller = Robolectric.buildActivity(HomeActivity.class).setup();
        activity = controller.get();
    }

    @Test
    public void testActionButtonsExistAndInitialState() {
        View editButton = activity.findViewById(R.id.edit);
        View prefsButton = activity.findViewById(R.id.preferences);

        assertNotNull(editButton);
        assertNotNull(prefsButton);
        assertEquals(View.GONE, editButton.getVisibility());
        assertEquals(View.GONE, prefsButton.getVisibility());
    }

    @Test
    public void testEditButtonClickEntersEditMode() {
        AppPieView pieView = activity.findViewById(R.id.pie);
        View editButton = activity.findViewById(R.id.edit);

        assertFalse(pieView.inEditMode());

        editButton.performClick();

        assertTrue(pieView.inEditMode());
    }

    @Test
    public void testPrefsButtonLongClickDoesNotEnterEditMode() {
        AppPieView pieView = activity.findViewById(R.id.pie);
        View prefsButton = activity.findViewById(R.id.preferences);

        assertFalse(pieView.inEditMode());

        prefsButton.performLongClick();

        assertFalse(pieView.inEditMode());
    }

    @Test
    public void testPrefsButtonClickLaunchesPreferencesActivity() {
        View prefsButton = activity.findViewById(R.id.preferences);

        prefsButton.performClick();

        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(PreferencesActivity.class.getName(), nextIntent.getComponent().getClassName());
    }

    @Test
    public void testActionButtonsVisibilityWithDrawerAndSearch() throws Exception {
        View editButton = activity.findViewById(R.id.edit);
        View prefsButton = activity.findViewById(R.id.preferences);
        android.widget.EditText searchInput = activity.findViewById(R.id.search);

        // Open drawer via showAllApps
        java.lang.reflect.Method showAllApps = HomeActivity.class.getDeclaredMethod("showAllApps");
        showAllApps.setAccessible(true);
        showAllApps.invoke(activity);

        assertEquals(View.VISIBLE, editButton.getVisibility());
        assertEquals(View.VISIBLE, prefsButton.getVisibility());

        // Type in search bar -> buttons should hide
        searchInput.setText("test");
        assertEquals(View.GONE, editButton.getVisibility());
        assertEquals(View.GONE, prefsButton.getVisibility());
    }
}
