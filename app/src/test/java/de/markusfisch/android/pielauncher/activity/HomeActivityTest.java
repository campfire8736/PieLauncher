package de.markusfisch.android.pielauncher.activity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Process;
import android.os.UserHandle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;
import de.markusfisch.android.pielauncher.widget.AppPieView;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HomeActivityTest {
    private ActivityController<HomeActivity> controller;
    private HomeActivity activity;

    @Before
    public void setUp() throws Exception {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Context context = ApplicationProvider.getApplicationContext();
        PieLauncherApp.getPrefs(context).setSkipSetup();

        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        java.lang.reflect.Constructor<Apps.AppIcon> constructor = Apps.AppIcon.class.getDeclaredConstructor(
                ComponentName.class, String.class, Bitmap.class, UserHandle.class);
        constructor.setAccessible(true);
        Apps.AppIcon icon = constructor.newInstance(
                new ComponentName("com.mock", "MockActivity"),
                "Mock App", bitmap, Process.myUserHandle());
        PieLauncherApp.apps.apps.put(new LauncherItemKey(icon.componentName, icon.userHandle), icon);

        controller = Robolectric.buildActivity(HomeActivity.class).setup();
        activity = controller.get();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
        PieLauncherApp.apps.apps.clear();
        PieLauncherApp.apps.menuPrimary.clear();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    @Test
    public void testActionButtonsExistAndInitialState() {
        View editButton = activity.findViewById(R.id.edit);
        View prefsButton = activity.findViewById(R.id.preferences);
        android.widget.TextView toolbar = activity.findViewById(R.id.toolbar);
        View floatingToolbar = activity.findViewById(R.id.floating_toolbar);

        assertNotNull(editButton);
        assertNotNull(prefsButton);
        assertNotNull(toolbar);
        assertNotNull(floatingToolbar);
        assertEquals(View.GONE, toolbar.getVisibility());
        assertEquals(View.GONE, floatingToolbar.getVisibility());
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
        android.widget.TextView toolbar = activity.findViewById(R.id.toolbar);
        View floatingToolbar = activity.findViewById(R.id.floating_toolbar);
        android.widget.EditText searchInput = activity.findViewById(R.id.search);

        // Open drawer via showAllApps
        java.lang.reflect.Method showAllApps = HomeActivity.class.getDeclaredMethod("showAllApps");
        showAllApps.setAccessible(true);
        showAllApps.invoke(activity);

        assertEquals(View.VISIBLE, toolbar.getVisibility());
        assertEquals(activity.getString(R.string.category_app_drawer), toolbar.getText().toString());
        assertEquals(View.VISIBLE, floatingToolbar.getVisibility());
        assertEquals(View.VISIBLE, editButton.getVisibility());
        assertEquals(View.VISIBLE, prefsButton.getVisibility());

        // Type in search bar -> action buttons should hide, but floatingToolbar and toolbar remain
        searchInput.setText("test");
        assertEquals(View.GONE, editButton.getVisibility());
        assertEquals(View.GONE, prefsButton.getVisibility());
        assertEquals(View.VISIBLE, floatingToolbar.getVisibility());
        assertEquals(View.VISIBLE, toolbar.getVisibility());
    }

    @Test
    public void testToolbarInEditModeAndBackClick() {
        AppPieView pieView = activity.findViewById(R.id.pie);
        android.widget.TextView toolbar = activity.findViewById(R.id.toolbar);
        View floatingToolbar = activity.findViewById(R.id.floating_toolbar);
        View editButton = activity.findViewById(R.id.edit);

        assertFalse(pieView.inEditMode());

        editButton.performClick();

        assertTrue(pieView.inEditMode());
        assertEquals(View.VISIBLE, toolbar.getVisibility());
        assertEquals(activity.getString(R.string.edit_pie_menu), toolbar.getText().toString());
        assertEquals(View.GONE, floatingToolbar.getVisibility());

        // Clicking toolbar (back arrow) should handle back: exit edit mode
        toolbar.performClick();
        assertFalse(pieView.inEditMode());
    }

    @Test
    public void testShowEditorOnResumeReEntersEditMode() throws Exception {
        AppPieView pieView = activity.findViewById(R.id.pie);
        assertFalse(pieView.inEditMode());

        java.lang.reflect.Field field = HomeActivity.class.getDeclaredField("showEditorOnResume");
        field.setAccessible(true);
        field.setBoolean(activity, true);

        controller.resume();

        assertTrue(pieView.inEditMode());
        assertFalse(field.getBoolean(activity));
    }

    @Test
    public void testPreferencesFromEditModeReturnsToEditMode() throws Exception {
        AppPieView pieView = activity.findViewById(R.id.pie);
        android.widget.TextView toolbar = activity.findViewById(R.id.toolbar);
        View editButton = activity.findViewById(R.id.edit);

        // Enter edit mode
        editButton.performClick();
        assertTrue(pieView.inEditMode());

        // Get editListener from pieView and trigger onPreferences
        java.lang.reflect.Field editListenerField = AppPieView.class.getDeclaredField("editListener");
        editListenerField.setAccessible(true);
        AppPieView.EditListener editListener = (AppPieView.EditListener) editListenerField.get(pieView);
        assertNotNull(editListener);

        editListener.onPreferences();

        Intent nextIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(PreferencesActivity.class.getName(), nextIntent.getComponent().getClassName());

        // Resume activity (as if coming back from PreferencesActivity)
        controller.resume();

        assertTrue(pieView.inEditMode());
        assertEquals(View.VISIBLE, toolbar.getVisibility());
        assertEquals(activity.getString(R.string.edit_pie_menu), toolbar.getText().toString());
    }
}
