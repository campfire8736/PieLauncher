package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.adapter.PickPieAppsAdapter;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.AppLauncher;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;

public class PickPieAppsActivity extends Activity {
	public static final String EXTRA_IS_PRIMARY = "is_primary";

	private final Set<LauncherItemKey> selectedKeys = new HashSet<>();

	private boolean isPrimary = true;
	private ToolbarBackground toolbarBackground;
	private GridView gridView;
	private PickPieAppsAdapter adapter;

	public static void start(Context context, boolean isPrimary) {
		Intent intent = new Intent(context, PickPieAppsActivity.class);
		intent.putExtra(EXTRA_IS_PRIMARY, isPrimary);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_pick_pie_apps);

		isPrimary = getIntent().getBooleanExtra(EXTRA_IS_PRIMARY, true);

		BackgroundBlur.setBlurRadius(getWindow(),
				PieLauncherApp.getPrefs(this).getBackgroundBlurRadius());
		toolbarBackground = new ToolbarBackground(this);
		View toolbar = findViewById(R.id.toolbar);
		gridView = findViewById(R.id.apps);

		findViewById(R.id.back).setOnClickListener(v -> finish());
		findViewById(R.id.apply).setOnClickListener(v -> applySelection());

		initSelection();

		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (visibleItemCount < 1) {
					return;
				}
				int y = 0xffff;
				if (firstVisibleItem == 0) {
					View child = view.getChildAt(0);
					if (child != null) {
						y = child.getTop() - view.getPaddingTop();
					}
				}
				toolbar.setBackgroundColor(toolbarBackground.getColorForY(y));
			}
		});

		gridView.setOnItemClickListener((parent, view, position, id) -> {
			Apps.AppIcon icon = adapter.getItem(position);
			if (icon != null) {
				LauncherItemKey key = new LauncherItemKey(
						icon.componentName, icon.userHandle);
				if (selectedKeys.contains(key)) {
					selectedKeys.remove(key);
				} else {
					selectedKeys.add(key);
				}
				adapter.notifyDataSetChanged();
			}
		});

		Window window = getWindow();
		SystemBars.addPaddingFromWindowInsets(toolbar, gridView);
		SystemBars.setTransparentSystemBars(window);
		SystemBars.setNavigationBarColor(window, toolbarBackground.backgroundColor);

		loadApps();
	}

	private void initSelection() {
		List<Apps.AppIcon> targetMenu = isPrimary
				? PieLauncherApp.apps.menuPrimary
				: PieLauncherApp.apps.menuSecondary;
		selectedKeys.clear();
		for (Apps.AppIcon icon : targetMenu) {
			if (icon != null) {
				selectedKeys.add(new LauncherItemKey(
						icon.componentName, icon.userHandle));
			}
		}
	}

	private void loadApps() {
		UserHandle privateUser = AppLauncher.findPrivateProfileUser(this);
		boolean privateLocked = privateUser != null &&
				AppLauncher.isPrivateProfileLocked(this, privateUser);

		ArrayList<Apps.AppIcon> all = new ArrayList<>();
		for (Apps.AppIcon appIcon : PieLauncherApp.apps.apps.values()) {
			if (privateLocked && appIcon.userHandle != null &&
					appIcon.userHandle.equals(privateUser)) {
				continue;
			}
			all.add(appIcon);
		}
		Collections.sort(all, (a, b) ->
				a.label.compareToIgnoreCase(b.label));

		adapter = new PickPieAppsAdapter(this, all, selectedKeys);
		gridView.setAdapter(adapter);
	}

	private void applySelection() {
		List<Apps.AppIcon> targetMenu = isPrimary
				? PieLauncherApp.apps.menuPrimary
				: PieLauncherApp.apps.menuSecondary;
		Preferences prefs = PieLauncherApp.getPrefs(this);
		boolean isRigid = prefs != null && prefs.isRigidPie();

		if (!isRigid) {
			ArrayList<Apps.AppIcon> newMenu = new ArrayList<>();
			// 1. Retain existing pie icons that remain selected (preserving order).
			for (Apps.AppIcon icon : targetMenu) {
				if (icon == null) continue;
				LauncherItemKey key = new LauncherItemKey(
						icon.componentName, icon.userHandle);
				if (selectedKeys.contains(key)) {
					newMenu.add(icon);
				}
			}
			// 2. Append newly selected icons.
			if (adapter != null) {
				int count = adapter.getCount();
				for (int i = 0; i < count; ++i) {
					Apps.AppIcon icon = adapter.getItem(i);
					if (icon != null) {
						LauncherItemKey key = new LauncherItemKey(
								icon.componentName, icon.userHandle);
						if (selectedKeys.contains(key) && !newMenu.contains(icon)) {
							newMenu.add(icon);
						}
					}
				}
			}

			targetMenu.clear();
			targetMenu.addAll(newMenu);
		} else {
			List<Apps.AppIcon> allAvailable = new ArrayList<>();
			if (adapter != null) {
				int count = adapter.getCount();
				for (int i = 0; i < count; ++i) {
					Apps.AppIcon icon = adapter.getItem(i);
					if (icon != null) {
						allAvailable.add(icon);
					}
				}
			}
			Apps.updateSelectionInRigidMenu(targetMenu, selectedKeys, allAvailable);
		}
		PieLauncherApp.apps.store(this);
		if (PieLauncherApp.getPrefs(this).excludePie() !=
				Preferences.EXCLUDE_PIE_NONE) {
			PieLauncherApp.apps.propagateUpdate();
		}
		finish();
	}
}
