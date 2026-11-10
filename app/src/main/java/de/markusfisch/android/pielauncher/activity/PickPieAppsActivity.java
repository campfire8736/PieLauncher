package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import de.markusfisch.android.pielauncher.content.DrawerSectionRegistry;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;
import de.markusfisch.android.pielauncher.content.PieItem;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.Dialog;
import de.markusfisch.android.pielauncher.widget.NestedGridView;
import de.markusfisch.android.pielauncher.widget.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.ScrollWithListenerView;

public class PickPieAppsActivity extends Activity {
	public static final String EXTRA_IS_PRIMARY = "is_primary";

	private final Set<LauncherItemKey> selectedKeys = new HashSet<>();
	private final Set<LauncherItemKey> initialKeys = new HashSet<>();
	private final List<PickPieAppsAdapter> adapters = new ArrayList<>();
	private final List<Apps.AppIcon> allApps = new ArrayList<>();

	private boolean isPrimary = true;
	private ToolbarBackground toolbarBackground;
	private ScrollWithListenerView scrollListenerView;
	private View warningContainer;
	private LinearLayout sectionsContainer;
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
		scrollListenerView = findViewById(R.id.scroll_view);
		warningContainer = findViewById(R.id.warning_container);
		sectionsContainer = findViewById(R.id.sections_container);

		findViewById(R.id.back).setOnClickListener(v -> confirmDiscardChanges());
		findViewById(R.id.apply).setOnClickListener(v -> confirmApplySelection());

		initSelection();
		updateWarning();

		scrollListenerView.setOnScrollPositionListener((y, scrollable) ->
				toolbar.setBackgroundColor(isLight
						? toolbarBackground.backgroundColor
						: toolbarBackground.getColorForY(y)));

		Window window = getWindow();
		SystemBars.addPaddingFromWindowInsets(toolbar, scrollListenerView);
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
		if (getSelectedCount() > 8) {
			sb.append(getString(R.string.warning_pie_too_many_items));
		}
		if (!added.isEmpty()) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}
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
		for (Apps.AppIcon item : allApps) {
			if (item != null && key.equals(new LauncherItemKey(
					item.componentName, item.userHandle))) {
				return item.label;
			}
		}
		return key.componentName != null
				? key.componentName.flattenToShortString()
				: "";
	}

	private int getSelectedCount() {
		List<PieItem> targetMenu = isPrimary
				? PieLauncherApp.apps.menuPrimary
				: PieLauncherApp.apps.menuSecondary;
		int count = selectedKeys.size();
		for (PieItem item : targetMenu) {
			if (item != null && !item.isGhost && !(item instanceof Apps.AppIcon)) {
				count++;
			}
		}
		return count;
	}

	private void updateWarning() {
		if (warningContainer != null) {
			warningContainer.setVisibility(
					getSelectedCount() > 8 ? View.VISIBLE : View.GONE);
		}
	}

	private void toggleSelection(Apps.AppIcon icon) {
		if (icon == null) {
			return;
		}
		LauncherItemKey key = new LauncherItemKey(
				icon.componentName, icon.userHandle);
		if (selectedKeys.contains(key)) {
			selectedKeys.remove(key);
		} else {
			selectedKeys.add(key);
		}
		for (PickPieAppsAdapter a : adapters) {
			a.notifyDataSetChanged();
		}
		updateWarning();
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

		List<DrawerSectionRegistry.SectionGroup> sectionGroups =
				DrawerSectionRegistry.group(all);

		allApps.clear();
		allApps.addAll(DrawerSectionRegistry.flatten(sectionGroups));

		sectionsContainer.removeAllViews();
		adapters.clear();

		float dp = getResources().getDisplayMetrics().density;
		int textColorSecondary = PreferenceSliderView.getColorFromAttribute(
				this, android.R.attr.textColorSecondary,
				getResources().getColor(R.color.text_color_preference));

		NestedGridView primaryGrid = null;

		for (DrawerSectionRegistry.SectionGroup group : sectionGroups) {
			if (group.items.isEmpty()) {
				continue;
			}

			TextView header = new TextView(this);
			header.setText(group.section.getTitle(this));
			header.setTextSize(14f);
			header.setTypeface(Typeface.DEFAULT_BOLD);
			header.setTextColor(textColorSecondary);
			int topPadding = Math.round((sectionsContainer.getChildCount() == 0 ? 8f : 16f) * dp);
			int bottomPadding = Math.round(8f * dp);
			header.setPadding(Math.round(4f * dp), topPadding, Math.round(4f * dp), bottomPadding);
			sectionsContainer.addView(header);

			NestedGridView grid = new NestedGridView(this);
			grid.setColumnWidth(Math.round(72f * dp));
			grid.setHorizontalSpacing(Math.round(8f * dp));
			grid.setVerticalSpacing(Math.round(8f * dp));
			grid.setNumColumns(GridView.AUTO_FIT);
			grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
			grid.setGravity(Gravity.CENTER);
			grid.setClipToPadding(false);

			if (primaryGrid == null || DrawerSectionRegistry.SECTION_ID_APPS.equals(group.section.getId())) {
				grid.setId(R.id.apps);
				primaryGrid = grid;
			}

			PickPieAppsAdapter sectionAdapter = new PickPieAppsAdapter(
					this, group.items, selectedKeys);
			grid.setAdapter(sectionAdapter);
			adapters.add(sectionAdapter);

			grid.setOnItemClickListener((parent, view, position, id) -> {
				Apps.AppIcon clickedIcon = sectionAdapter.getItem(position);
				toggleSelection(clickedIcon);
			});

			sectionsContainer.addView(grid);
		}

		gridView = primaryGrid;
		if (primaryGrid != null) {
			adapter = (PickPieAppsAdapter) primaryGrid.getAdapter();
		}
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
			for (Apps.AppIcon icon : allApps) {
				if (icon != null) {
					LauncherItemKey key = new LauncherItemKey(
							icon.componentName, icon.userHandle);
					if (selectedKeys.contains(key) && !newMenu.contains(icon)) {
						newMenu.add(icon);
					}
				}
			}

			targetMenu.clear();
			targetMenu.addAll(newMenu);
		} else {
			Apps.updateSelectionInRigidMenu(targetMenu, selectedKeys, allApps);
		}
		PieLauncherApp.apps.store(this);
		if (PieLauncherApp.getPrefs(this).excludePie() !=
				Preferences.EXCLUDE_PIE_NONE) {
			PieLauncherApp.apps.propagateUpdate();
		}
		finish();
	}
}
