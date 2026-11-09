package de.markusfisch.android.pielauncher.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.Database;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SoftKeyboard;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.AppPieView;

public class HomeActivity extends Activity {
	private Preferences prefs;
	private SoftKeyboard kb;
	private GestureDetector gestureDetector;
	private ToolbarBackground toolbarBackground;
	private AppPieView pieView;
	private View toolbar;
	private EditText searchInput;
	private ImageView prefsButton;
	private ImageView editButton;
	private boolean updateAfterTextChange = true;
	private boolean showAllAppsOnResume = false;
	private boolean showEditorOnResume = false;
	private boolean isLight;
	private int immersiveMode = Preferences.IMMERSIVE_MODE_DISABLED;
	private long pausedAt = 0L;
	private OnBackInvokedCallback onBackInvokedCallback = null;

	@SuppressLint("GestureBackNavigation")
	@Override
	public void onBackPressed() {
		handleBack();
	}

	private void handleBack() {
		if (pieView.inEditMode()) {
			pieView.endEditMode();
			showAllApps();
		} else {
			hideAllApps();
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (ev == null) {
			return false;
		}
		if (pieView.inListMode() && gestureDetector.onTouchEvent(ev)) {
			return true;
		}
		try {
			return super.dispatchTouchEvent(ev);
		} catch (IllegalStateException e) {
			// Never saw this happen on any of my devices but had two reports
			// from Google so it's probably better to catch and ignore this
			// exception than letting the launcher crash.
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle state) {
		prefs = PieLauncherApp.getPrefs(this);
		isLight = prefs.isLight(this);
		setTheme(isLight ? R.style.HomeTheme_Light : R.style.HomeTheme);
		super.onCreate(state);

		if (state != null) {
			showEditorOnResume = state.getBoolean("show_editor_on_resume", false);
		}

		kb = new SoftKeyboard(this);
		gestureDetector = new GestureDetector(this, new FlingListener(
				ViewConfiguration.get(this).getScaledMinimumFlingVelocity()));

		setContentView(R.layout.activity_home);
		registerBackCallback();
		if (!PreferencesActivity.isReady(this)) {
			PreferencesActivity.startWelcome(this);
		}

		toolbarBackground = new ToolbarBackground(this);
		pieView = findViewById(R.id.pie);
		toolbar = findViewById(R.id.toolbar);
		searchInput = findViewById(R.id.search);
		prefsButton = findViewById(R.id.preferences);
		editButton = findViewById(R.id.edit);

		initPieView();
		initActionButtons();
		initSearchInput();

		SystemBars.listenForWindowInsets(pieView,
				(left, top, right, bottom) -> {
					pieView.setInsets(left, top, right, bottom);
					if (toolbar != null) {
						toolbar.setPadding(left, top, right, 0);
					}
				});

		immersiveMode = prefs.getImmersiveMode();
		SystemBars.setTransparentSystemBars(getWindow(), immersiveMode);
		SystemBars.setLightSystemBars(getWindow(), isLight);

		// We may have been started with a request to pin a shortcut, e.g. when
		// a browser adds a PWA to the home screen. No-op for regular launches.
		PieLauncherApp.apps.acceptPinRequest(this, getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.preferences) {
			showPreferences();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// A shortcut-pin request (e.g. adding a PWA from a browser) arrives
		// here while the launcher is already running. Handle it and return so
		// the home-gesture logic below doesn't act on this unrelated intent.
		if (Apps.isPinItemRequest(intent)) {
			PieLauncherApp.apps.acceptPinRequest(this, intent);
			return;
		}
		// Because this activity has the launch mode "singleTask", it'll get
		// an onNewIntent() when the activity is re-launched.
		if (intent != null &&
				(intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) !=
						Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {
			if (isGestureNavigationEnabled()) {
				if (Build.VERSION.SDK_INT ==
						Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
					// Deal with the gesture navigation/recent apps bug in
					// Android 14:
					// * make the home gesture twice
					// * enter list of recent apps
					// * now an empty, broken screen is in recent apps
					finish();
				} else {
					resetHomeState();
				}
				return;
			}
			// If this activity is re-launched but _not_ brought to front,
			// the home button was pressed while this activity was on screen.
			if (pieView.inEditMode()) {
				pieView.endEditMode();
			} else if (prefs.showDrawerOnHome() &&
					!isSearchVisible() &&
					// Only show all apps if the activity was recently paused
					// (by pressing the home button) and _not_ if onPause()
					// was triggered by pressing the overview button and
					// *then* the home button.
					System.currentTimeMillis() - pausedAt < 100) {
				// onNewIntent() is always followed by onResume()
				showAllAppsOnResume = true;
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		// On some devices, after unlock, the activity resumes normally but
		// WindowManager never issues a draw command to its Surface. Forcing
		// an invalidate here triggers a redraw when the window regains focus.
		if (hasFocus) {
			getWindow().getDecorView().invalidate();
		}
	}

	@Override
	public void onRestart() {
		super.onRestart();
		if (prefs.forceRelaunch()) {
			finish();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		setRequestedOrientation(prefs.getOrientation());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("show_editor_on_resume",
				showEditorOnResume || (pieView != null && pieView.inEditMode()));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isLight != prefs.isLight(this)) {
			recreate();
			return;
		}
		updateSystemBars();
		if (showEditorOnResume) {
			showEditor();
			showEditorOnResume = false;
		} else if (showAllAppsOnResume) {
			showAllApps();
			showAllAppsOnResume = false;
		} else {
			hideAllApps();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		pausedAt = System.currentTimeMillis();
	}

	@Override
	protected void onDestroy() {
		unregisterBackCallback();
		PieLauncherApp.apps.setUpdateListener(null);
		super.onDestroy();
	}

	private void initPieView() {
		pieView.setWindow(getWindow());
		pieView.setActionCallback(new AppPieView.ActionCallback() {
			@Override
			public void onAppTapped(Apps.AppIcon icon) {
				if (de.markusfisch.android.pielauncher.content.AppLauncher.launchApp(HomeActivity.this, icon)) {
					Database database = PieLauncherApp.getDatabase(HomeActivity.this);
					database.recordLaunchAsync(icon);
					if (pieView.inListMode() && pieView.isFiltered() &&
							prefs.rememberSearchPicks()) {
						database.recordSearchPickAsync(pieView.getQuery(), icon);
					}
				}
			}

			@Override
			public void onAppInfoRequested(Apps.AppIcon icon) {
				de.markusfisch.android.pielauncher.content.AppLauncher.launchAppInfo(HomeActivity.this, icon);
			}

			@Override
			public void onEditOptionsRequested(Apps.AppIcon icon) {
				if (icon == null) return;
				java.util.ArrayList<String> list = new java.util.ArrayList<>();
				list.add(getString(R.string.edit_pie_menu));
				list.add(getString(R.string.add_to_pie_menu));
				list.add(getString(R.string.show_app_info));
				list.add(getString(R.string.hide_app));
				if (PieLauncherApp.iconPack.hasPacks()) {
					list.add(getString(R.string.change_icon));
				}
				de.markusfisch.android.pielauncher.widget.OptionsDialog.show(HomeActivity.this, R.string.edit_app,
						list.toArray(new CharSequence[0]),
						(view, which) -> {
							switch (which) {
								case 0:
									pieView.addIconInteractively(null);
									break;
								case 1:
									pieView.addIconInteractively(icon);
									pieView.postDelayed(() -> pieView.releaseIcon(), 100);
									break;
								case 2:
									de.markusfisch.android.pielauncher.content.AppLauncher.launchAppInfo(HomeActivity.this, icon);
									break;
								case 3:
									de.markusfisch.android.pielauncher.activity.PickIconActivity.askToHide(HomeActivity.this, icon.componentName);
									break;
								case 4:
									pieView.returnToList();
									pieView.changeIcon(HomeActivity.this, icon);
									break;
							}
						});
			}
		});
		pieView.setListListener(new AppPieView.ListListener() {
			@Override
			public void onOpenList(boolean resume) {
				showAllAppsOnResume = resume;
				showAllApps();
			}

			@Override
			public void onHideList() {
				hideAllApps();
			}

			@Override
			public void onScrollList(int y, boolean isScrolling) {
				if (isScrolling && y != 0) {
					hideKeyboardAndActionButtons();
				}
				setToolbarColor(toolbarBackground.backgroundColor);
			}

			@Override
			public void onDragDown(float alpha) {
				hideKeyboardAndActionButtons();
				setAlpha(toolbar, alpha);
			}
		});
		PieLauncherApp.apps.setUpdateListener(new Apps.UpdateListener() {
			@Override
			public void onUpdate() {
				updateAppList(false);
			}

			@Override
			public void onShowAllAppsOnResume() {
				showAllAppsOnResume = true;
			}
		});
		pieView.setEditListener(isPrimary -> {
			showEditorOnResume = true;
			PickPieAppsActivity.start(HomeActivity.this, isPrimary);
		});
	}

	private void initActionButtons() {
		editButton.setOnClickListener(v -> showEditor());
		prefsButton.setOnClickListener(v -> showPreferences());
	}

	private void initSearchInput() {
		searchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable e) {
				if (!updateAfterTextChange) {
					return;
				} else if (e.length() > 0) {
					hideActionButtons();
				}
				String s = e.toString();
				if (s.equals("..")) {
					e.clear();
					showPreferences();
					return;
				} else if (s.equals(",,")) {
					e.clear();
					showEditor();
					return;
				}
				// Replace ". " before updateAppList().
				if (endsWithDoubleSpace(e)) {
					pieView.launchSelectedAppFromList();
				}
				updateAppList(true);
				// Check icon count after the list was updated.
				if (prefs.autoLaunchMatching() &&
						pieView.getIconCount() == 1) {
					pieView.launchSelectedAppFromList();
				}
			}
		});
		searchInput.setOnEditorActionListener((v, actionId, event) -> {
			switch (actionId) {
				case EditorInfo.IME_ACTION_GO:
				case EditorInfo.IME_ACTION_SEND:
				case EditorInfo.IME_ACTION_DONE:
				case EditorInfo.IME_ACTION_NEXT:
				case EditorInfo.IME_ACTION_SEARCH:
				case EditorInfo.IME_NULL:
					if (!searchInput.getText().toString().isEmpty()) {
						pieView.launchSelectedAppFromList();
					}
					hideAllApps();
					return true;
				default:
					return false;
			}
		});
	}

	private boolean endsWithDoubleSpace(Editable e) {
		boolean doubleSpaceLaunch = prefs.doubleSpaceLaunch();
		String s = e.toString();
		if ((doubleSpaceLaunch && s.endsWith("  ")) ||
				// Some keyboards auto-replace two spaces with ". ",
				// which means a new, different search result.
				s.endsWith(". ")) {
			updateAfterTextChange = false;
			e.clear();
			e.append(s.substring(0, s.length() - 2));
			if (!doubleSpaceLaunch) {
				// Restore the two spaces for moving the selection.
				e.append("  ");
			}
			updateAfterTextChange = true;
			return doubleSpaceLaunch;
		}
		return false;
	}

	private void updateSystemBars() {
		int newImmersiveMode = prefs.getImmersiveMode();
		if (immersiveMode != newImmersiveMode) {
			immersiveMode = newImmersiveMode;
			SystemBars.setSystemUIVisibility(getWindow(), immersiveMode);
			SystemBars.setLightSystemBars(getWindow(), isLight);
		}
	}

	private void showPreferences() {
		// Hide searchInput to avoid inactive InputConnection.
		hideAllApps();
		PreferencesActivity.start(this);
		showAllAppsOnResume = true;
	}

	private void showEditor() {
		hideAllApps();
		pieView.showEditor();
	}

	private void hideActionButtons() {
		if (prefsButton.getVisibility() == View.VISIBLE) {
			prefsButton.setVisibility(View.GONE);
		}
		if (editButton.getVisibility() == View.VISIBLE) {
			editButton.setVisibility(View.GONE);
		}
	}

	private void showAllApps() {
		if (isSearchVisible()) {
			return;
		}

		toolbar.setVisibility(View.VISIBLE);
		searchInput.setVisibility(View.VISIBLE);
		prefsButton.setVisibility(View.VISIBLE);
		editButton.setVisibility(View.VISIBLE);
		setAlpha(toolbar, 1f);
		setToolbarColor(toolbarBackground.backgroundColor);
		if (prefs.displayKeyboard()) {
			kb.showFor(searchInput);
		}

		// Clear search input.
		Editable editable = searchInput.getText();
		updateAfterTextChange = false;
		editable.clear();
		updateAfterTextChange = true;

		updateAppList(true);
		pieView.showList();
	}

	private void setToolbarColor(int color) {
		if (toolbar != null) {
			toolbar.setBackgroundColor(color);
		}
	}

	private void setAlpha(View view, float alpha) {
		view.setAlpha(alpha);
	}

	private void hideAllApps() {
		if (isSearchVisible()) {
			toolbar.setVisibility(View.GONE);
			searchInput.setVisibility(View.GONE);
			hideKeyboardAndActionButtons();
		}
		// Ensure the pie menu is initially hidden because on some devices
		// there's not always a matching ACTION_UP/_CANCEL event for every
		// ACTION_DOWN event.
		pieView.hideList();
	}

	private void resetHomeState() {
		showAllAppsOnResume = false;
		if (pieView.inEditMode()) {
			pieView.endEditMode();
		}
		hideAllApps();
	}

	private void hideKeyboardAndActionButtons() {
		kb.hideFrom(searchInput);
		hideActionButtons();
	}

	private boolean isSearchVisible() {
		return toolbar != null && toolbar.getVisibility() == View.VISIBLE;
	}

	private boolean isGestureNavigationEnabled() {
		return Settings.Secure.getInt(getContentResolver(),
				"navigation_mode", 0) == 2;
	}

	private void updateAppList(boolean resetScroll) {
		pieView.filterAppList(searchInput.getText().toString(), resetScroll);
	}

	private void registerBackCallback() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
				onBackInvokedCallback != null) {
			return;
		}

		onBackInvokedCallback = this::handleBack;
		getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
				OnBackInvokedDispatcher.PRIORITY_DEFAULT,
				onBackInvokedCallback);
	}

	private void unregisterBackCallback() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
				onBackInvokedCallback == null) {
			return;
		}

		getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
				onBackInvokedCallback);
		onBackInvokedCallback = null;
	}

	private class FlingListener extends GestureDetector.SimpleOnGestureListener {
		private final int minimumVelocity;

		private FlingListener(int minimumVelocity) {
			this.minimumVelocity = minimumVelocity;
		}

		@Override
		public boolean onScroll(
				MotionEvent e1,
				MotionEvent e2,
				float distanceX,
				float distanceY) {
			if (pieView.appListNotScrolled() &&
					!pieView.isScrollingAppShortcuts() &&
					Math.abs(distanceY) > Math.abs(distanceX)) {
				pieView.dragDownListBy(distanceY);
			}
			return false;
		}

		@Override
		public boolean onFling(
				MotionEvent e1,
				MotionEvent e2,
				float velocityX,
				float velocityY) {
			if (pieView.appListNotScrolled() &&
					!pieView.isScrollingAppShortcuts() &&
					velocityY > velocityX &&
					velocityY >= minimumVelocity &&
					e1 != null && e2 != null &&
					e2.getY() - e1.getY() > 0) {
				pieView.resetScroll();
				hideAllApps();
				return true;
			}
			return false;
		}
	}
}
