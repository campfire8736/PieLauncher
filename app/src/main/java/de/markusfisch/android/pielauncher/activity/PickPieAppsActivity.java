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
import de.markusfisch.android.pielauncher.content.PieItem;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.Dialog;

public class PickPieAppsActivity extends Activity {
	public static final String EXTRA_IS_PRIMARY = "is_primary";

	private final Set<LauncherItemKey> selectedKeys = new HashSet<>();
	private final Set<LauncherItemKey> initialKeys = new HashSet<>();

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
		Preferences prefs = PieLauncherApp.getPrefs(this);
		boolean isLight = prefs.isLight(this);
		setTheme(isLight ? R.style.HomeTheme_Light : R.style.HomeTheme);
		super.onCreate(state);

		setContentView(R.layout.activity_pick_pie_apps);

		isPrimary = getIntent().getBooleanExtra(EXTRA_IS_PRIMARY, true);

		BackgroundBlur.setBlurRadius(getWindow(),
				PieLauncherApp.getPrefs(this).getBackgroundBlurRadius());
		toolbarBackground = new ToolbarBackground(this);
		View toolbar = findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(isLight
				? toolbarBackground.backgroundColor
				: toolbarBackground.getColorForY(0));
		gridView = findViewById(R.id.apps);

		findViewById(R.id.back).setOnClickListener(v -> confirmDiscardChanges());
		findViewById(R.id.apply).setOnClickListener(v -> confirmApplySelection());

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
				toolbar.setBackgroundColor(isLight
						? toolbarBackground.backgroundColor
						: toolbarBackground.getColorForY(y));
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
		SystemBars.setLightSystemBars(window, isLight);
		SystemBars.setNavigationBarColor(window, toolbarBackground.backgroundColor);

		loadApps();
	}

	private void initSelection() {
		List<PieItem> targetMenu = isPrimary
				? PieLauncherApp.apps.menuPrimary
				: PieLauncherApp.apps.menuSecondary;
		selectedKeys.clear();
		for (PieItem item : targetMenu) {
			if (item instanceof Apps.AppIcon) {
				Apps.AppIcon icon = (Apps.AppIcon) item;
				selectedKeys.add(new LauncherItemKey(
						icon.componentName, icon.userHandle));
			}
		}
		initialKeys.clear();
		initialKeys.addAll(selectedKeys);
	}

	@Override
	public void onBackPressed() {
		confirmDiscardChanges();
	}

	private boolean hasModifications() {
		return !selectedKeys.equals(initialKeys);
	}

	private void confirmDiscardChanges() {
		if (!hasModifications()) {
			finish();
			return;
		}
		Dialog.newDialog(this)
				.setTitle(R.string.discard_changes)
				.setMessage(R.string.discard_changes_message)
				.setPositiveButton(R.string.discard, (dialog, which) -> finish())
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void confirmApplySelection() {
		if (!hasModifications()) {
			finish();
			return;
		}
		Dialog.newDialog(this)
				.setTitle(R.string.apply_changes)
				.setMessage(buildChangesSummary())
				.setPositiveButton(R.string.apply, (dialog, which) -> applySelection())
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private CharSequence buildChangesSummary() {
		List<String> added = new ArrayList<>();
		for (LauncherItemKey key : selectedKeys) {
			if (!initialKeys.contains(key)) {
				added.add(getAppLabel(key));
			}
		}
		Collections.sort(added, String.CASE_INSENSITIVE_ORDER);

		List<String> removed = new ArrayList<>();
		for (LauncherItemKey key : initialKeys) {
			if (!selectedKeys.contains(key)) {
				removed.add(getAppLabel(key));
			}
		}
		Collections.sort(removed, String.CASE_INSENSITIVE_ORDER);

		StringBuilder sb = new StringBuilder();
		if (!added.isEmpty()) {
			sb.append(getString(R.string.changes_added));
			for (String name : added) {
				sb.append("\n• ").append(name);
			}
		}
		if (!removed.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
			sb.append(getString(R.string.changes_removed));
			for (String name : removed) {
				sb.append("\n• ").append(name);
			}
		}
		return sb.toString();
	}

	private String getAppLabel(LauncherItemKey key) {
		Apps.AppIcon icon = PieLauncherApp.apps.apps.get(key);
		if (icon != null && icon.label != null) {
			return icon.label;
		}
		if (adapter != null) {
			int count = adapter.getCount();
			for (int i = 0; i < count; ++i) {
				Apps.AppIcon item = adapter.getItem(i);
				if (item != null && key.equals(new LauncherItemKey(
						item.componentName, item.userHandle))) {
					return item.label;
				}
			}
		}
		return key.componentName != null
				? key.componentName.flattenToShortString()
				: "";
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
		List<PieItem> targetMenu = isPrimary
				? PieLauncherApp.apps.menuPrimary
				: PieLauncherApp.apps.menuSecondary;
		Preferences prefs = PieLauncherApp.getPrefs(this);
		boolean isRigid = prefs != null && prefs.isRigidPie();

		if (!isRigid) {
			ArrayList<PieItem> newMenu = new ArrayList<>();
			// 1. Retain existing pie items (selected apps and all non-app items).
			for (PieItem item : targetMenu) {
				if (item == null) continue;
				if (item instanceof Apps.AppIcon) {
					Apps.AppIcon icon = (Apps.AppIcon) item;
					LauncherItemKey key = new LauncherItemKey(
							icon.componentName, icon.userHandle);
					if (selectedKeys.contains(key)) {
						newMenu.add(icon);
					}
				} else {
					newMenu.add(item);
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
