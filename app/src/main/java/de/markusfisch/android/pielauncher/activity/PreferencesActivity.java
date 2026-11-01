package de.markusfisch.android.pielauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;
import de.markusfisch.android.pielauncher.graphics.ToolbarBackground;
import de.markusfisch.android.pielauncher.os.BatteryOptimization;
import de.markusfisch.android.pielauncher.os.DefaultLauncher;
import de.markusfisch.android.pielauncher.preference.Preferences;
import de.markusfisch.android.pielauncher.view.SystemBars;
import de.markusfisch.android.pielauncher.widget.OptionsDialog;
import de.markusfisch.android.pielauncher.widget.ScrollWithListenerView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceActionView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceChoiceView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.preference.PreferenceSwitchView;

public class PreferencesActivity extends Activity {
	private static final String WELCOME = "welcome";

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final ExecutorService executor =
			Executors.newSingleThreadExecutor();

	private OnBackInvokedCallback onBackInvokedCallback = null;

	private Preferences prefs;
	private ToolbarBackground toolbarBackground;
	private View setupCard;
	private PreferenceActionView disableBatteryOptimizations;
	private PreferenceActionView defaultLauncherView;
	private boolean isWelcomeMode = false;

	public static void startWelcome(Context context) {
		start(context, true);
	}

	public static void start(Context context) {
		start(context, false);
	}

	public static boolean isReady(Context context) {
		return PieLauncherApp.getPrefs(context).skipSetup() ||
				(BatteryOptimization.isIgnoringBatteryOptimizations(context) &&
						DefaultLauncher.isDefault(context));
	}

	@Override
	protected void onCreate(Bundle state) {
		prefs = PieLauncherApp.getPrefs(this);
		if (prefs.invertColors()) {
			setTheme(R.style.HomeTheme_Inverted);
		}
		super.onCreate(state);
		registerBackCallback();
		setContentView(R.layout.activity_preferences);

		BackgroundBlur.setBlurRadius(getWindow(),
				prefs.getBackgroundBlurRadius());
		toolbarBackground = new ToolbarBackground(getResources(),
				getResources().getColor(prefs.invertColors()
						? R.color.bg_ui_inverted
						: R.color.bg_ui),
				getResources().getColor(prefs.invertColors()
						? R.color.bg_search_bar_inverted
						: R.color.bg_search_bar));

		TextView toolbar = findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(toolbarBackground.baseColor);
		int primaryTextColor = getResources().getColor(prefs.invertColors()
				? R.color.text_color_inverted
				: R.color.text_color);
		for (android.graphics.drawable.Drawable d : toolbar.getCompoundDrawables()) {
			if (d != null) {
				d.mutate().setColorFilter(primaryTextColor,
						android.graphics.PorterDuff.Mode.SRC_IN);
			}
		}
		View skipButton = findViewById(R.id.skip);

		setupCard = findViewById(R.id.setup_card);
		disableBatteryOptimizations = findViewById(
				R.id.disable_battery_optimization);
		defaultLauncherView = findViewById(R.id.make_default_launcher);
		if (disableBatteryOptimizations != null) {
			disableBatteryOptimizations.setChevronVisible(false);
		}
		if (defaultLauncherView != null) {
			defaultLauncherView.setChevronVisible(false);
		}

		Intent intent = getIntent();
		isWelcomeMode = intent != null &&
				intent.getBooleanExtra(WELCOME, false);

		PreferenceActionView welcomeView = findViewById(R.id.welcome);
		if (welcomeView != null) {
			welcomeView.setChevronVisible(false);
			welcomeView.setClickable(false);
			welcomeView.setFocusable(false);
			welcomeView.setBackground(null);
		}

		if (isWelcomeMode) {
			toolbar.setText(R.string.welcome);
			toolbar.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

			skipButton.setOnClickListener(view -> {
				prefs.setSkipSetup();
				finish();
			});

			findViewById(R.id.hide_in_welcome_mode).setVisibility(View.GONE);
		} else {
			toolbar.setOnClickListener(v -> finish());
			if (welcomeView != null) {
				welcomeView.setVisibility(View.GONE);
			}

			skipButton.setVisibility(View.GONE);

			initPreferences();
			View hiddenApps = findViewById(R.id.hidden_apps);
			if (hiddenApps != null) {
				hiddenApps.setOnClickListener((view) -> {
					HiddenAppsActivity.start(this);
				});
			}
		}

		Window window = getWindow();
		ScrollWithListenerView scrollView = findViewById(R.id.content);
		scrollView.setOnScrollPositionListener((int y, boolean scrollable) -> {
			toolbar.setBackgroundColor(toolbarBackground.getColorForY(y));
			SystemBars.setNavigationBarColor(window,
					scrollable ? toolbarBackground.backgroundColor : 0);
		});
		SystemBars.addPaddingFromWindowInsets(toolbar, scrollView);
		SystemBars.setTransparentSystemBars(window);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setRequestedOrientation(prefs.getOrientation());

		boolean disableBatteryVisible = updateDisableBatteryOptimizations();
		boolean updateDefaultVisible = updateDefaultLauncher();
		if (disableBatteryVisible && updateDefaultVisible && isWelcomeMode) {
			finish();
		}

		if (setupCard != null && !isWelcomeMode) {
			setupCard.setVisibility(
					(!disableBatteryVisible || !updateDefaultVisible)
							? View.VISIBLE
							: View.GONE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterBackCallback();
		executor.shutdownNow();
	}

	private void registerBackCallback() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
				onBackInvokedCallback != null) {
			return;
		}

		onBackInvokedCallback = this::finish;
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

	private void initPreferences() {
		initChoice(R.id.orientation,
				R.string.orientation,
				PreferencesActivity::getOrientationOptions,
				prefs::getOrientation,
				prefs::setOrientation);

		initChoice(R.id.darken_background,
				R.string.darken_background,
				PreferencesActivity::getDarkenBackgroundOptions,
				prefs::darkenBackground,
				prefs::setDarkenBackground);

		initSwitch(R.id.invert_colors,
				prefs::invertColors,
				value -> {
					prefs.setInvertColors(value);
					recreate();
				});

		if (BackgroundBlur.canBlur(getWindow())) {
			PreferenceSliderView slider = findViewById(R.id.blur_background);
			if (slider != null) {
				slider.setMax(80);
				slider.setProgress(prefs.getBackgroundBlurRadius());
				slider.setValueFormatter(val -> val == 0
						? getString(R.string.background_blur_radius_none)
						: val + " dp");
				slider.setOnSliderChangeListener((v, progress, fromUser) -> {
					prefs.setBackgroundBlurRadius(progress);
					BackgroundBlur.setBlurRadius(getWindow(), progress);
				});
			}
		} else {
			View blurBg = findViewById(R.id.blur_background);
			if (blurBg != null) {
				blurBg.setVisibility(View.GONE);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			initSwitch(R.id.blur_menu,
					prefs::blurMenu,
					prefs::setBlurMenu);
		} else {
			View blurMenu = findViewById(R.id.blur_menu);
			if (blurMenu != null) {
				blurMenu.setVisibility(View.GONE);
			}
		}

		initChoice(R.id.dead_zone,
				R.string.dead_zone,
				PreferencesActivity::getDeadZoneOptions,
				prefs::getDeadZone,
				prefs::setDeadZone);

		initSwitch(R.id.expand_panel_in_dead_zone,
				prefs::expandPanelInDeadZone,
				value -> {
					if (value) {
						prefs.setDeadZone(Preferences.DEAD_ZONE_ALL);
						PreferenceChoiceView deadZoneView = findViewById(R.id.dead_zone);
						if (deadZoneView != null) {
							updateChoiceValue(deadZoneView,
									getDeadZoneOptions().get(prefs.getDeadZone()));
						}
					}
					prefs.setExpandPanelInDeadZone(value);
				});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			initChoice(R.id.immersive_mode,
					R.string.immersive_mode,
					PreferencesActivity::getImmersiveModeOptions,
					prefs::getImmersiveMode,
					prefs::setImmersiveMode);
		} else {
			View imm = findViewById(R.id.immersive_mode);
			if (imm != null) {
				imm.setVisibility(View.GONE);
			}
		}

		initSwitch(R.id.animate_in_out,
				prefs::animateInOut,
				prefs::setAnimateInOut);

		initChoice(R.id.split_pie_menu,
				R.string.split_pie_menu,
				PreferencesActivity::getAlternateMenuOptions,
				prefs::splitPieMenu,
				prefs::setSplitPieMenu);

		initChoice(R.id.circle_swaps_menus,
				R.string.circle_swaps_menus,
				PreferencesActivity::getAlternateMenuOptions,
				prefs::circleSwapsMenus,
				prefs::setCircleSwapsMenus);

		initSwitch(R.id.home_button_opens_drawer,
				prefs::showDrawerOnHome,
				prefs::setShowDrawerOnHome);

		initChoice(R.id.open_list_with,
				R.string.open_list_with,
				PreferencesActivity::getOpenListWithOptions,
				prefs::openListWith,
				value -> {
					prefs.setOpenListWith(value);
					PieLauncherApp.apps.updateIconsAsync(this);
				});

		initChoice(R.id.list_animation_appearance,
				R.string.list_animation_appearance,
				PreferencesActivity::getListAnimationAppearanceOptions,
				prefs::listAnimationAppearance,
				prefs::setListAnimationAppearance);

		initSwitch(R.id.display_keyboard,
				prefs::displayKeyboard,
				prefs::setDisplayKeyboard);

		initSwitch(R.id.space_action,
				prefs::doubleSpaceLaunch,
				prefs::setDoubleSpaceLaunch);

		initSwitch(R.id.show_app_shortcuts,
				prefs::showAppShortcuts,
				prefs::setShowAppShortcuts);

		initSwitch(R.id.auto_launch_matching,
				prefs::autoLaunchMatching,
				prefs::setAutoLaunchMatching);

		initSwitch(R.id.remember_search_picks,
				prefs::rememberSearchPicks,
				prefs::setRememberSearchPicks);

		initChoice(R.id.search_strictness,
				R.string.search_strictness,
				PreferencesActivity::getSearchStrictnessOptions,
				prefs::getSearchStrictness,
				prefs::setSearchStrictness);

		initChoice(R.id.search_parameter,
				R.string.search_parameter,
				PreferencesActivity::getSearchParameterOptions,
				prefs::getSearchParameter,
				prefs::setSearchParameter);

		initChoice(R.id.app_sorting,
				R.string.app_sorting,
				PreferencesActivity::getAppSortingOptions,
				prefs::getAppSorting,
				prefs::setAppSorting);

		initChoice(R.id.show_app_names,
				R.string.show_app_names,
				PreferencesActivity::getShowAppNamesOptions,
				prefs::showAppNames,
				prefs::setShowAppNames);

		initChoice(R.id.exclude_pie,
				R.string.exclude_pie,
				PreferencesActivity::getExcludePieOptions,
				prefs::excludePie,
				value -> {
					prefs.setExcludePie(value);
					PieLauncherApp.apps.updateIconsAsync(this);
				});

		initChoice(R.id.icon_press,
				R.string.icon_press,
				PreferencesActivity::getIconPressOptions,
				prefs::getIconPress,
				prefs::setIconPress);

		initChoice(R.id.icon_pack,
				R.string.icon_pack,
				this::getIconPackOptions,
				prefs::getIconPack,
				value -> {
					prefs.setIconPack(value);
					PieLauncherApp.apps.updateIconsAsync(this);
				},
				() -> PieLauncherApp.iconPack.updatePacks(
						getPackageManager()));

		initChoice(R.id.haptic_feedback,
				R.string.haptic_feedback,
				PreferencesActivity::getHapticFeedbackOptions,
				prefs::hapticFeedback,
				prefs::setHapticFeedback);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initSwitch(R.id.use_light_dialogs,
					prefs::useLightDialogs,
					prefs::setUseLightDialogs);
		} else {
			View lightDlg = findViewById(R.id.use_light_dialogs);
			if (lightDlg != null) {
				lightDlg.setVisibility(View.GONE);
			}
		}

		initSwitch(R.id.force_relaunch,
				prefs::forceRelaunch,
				prefs::setForceRelaunch);
	}

	private interface BooleanGetter {
		boolean get();
	}

	private interface BooleanSetter {
		void set(boolean value);
	}

	private void initSwitch(int viewId, BooleanGetter getter, BooleanSetter setter) {
		PreferenceSwitchView psv = findViewById(viewId);
		if (psv == null) {
			return;
		}
		psv.setChecked(getter.get());
		psv.setOnCheckedChangeListener((v, isChecked) -> setter.set(isChecked));
	}

	private <T, G> void initChoice(
			int viewId,
			int titleId,
			GetOptionsListener<T, G> options,
			GetListener<T> getter,
			SetListener<T> setter) {
		initChoice(viewId, titleId, options, getter, setter, null);
	}

	private <T, G> void initChoice(
			int viewId,
			int titleId,
			GetOptionsListener<T, G> options,
			GetListener<T> getter,
			SetListener<T> setter,
			Initializer initializer) {
		PreferenceChoiceView pcv = findViewById(viewId);
		if (pcv == null) {
			return;
		}
		if (initializer != null) {
			executor.execute(() -> {
				initializer.onInit();
				handler.post(() -> initChoice(
						viewId, titleId, options, getter, setter, null));
			});
			pcv.setValue(R.string.tip_loading);
			return;
		}
		Map<T, G> optionsMap = options.onGetOptions();
		pcv.setOnClickListener(v -> {
			CharSequence[] items = getItemsFromOptions(optionsMap);
			int selectedIndex = -1;
			int index = 0;
			T current = getter.onGet();
			for (T key : optionsMap.keySet()) {
				if ((key != null && key.equals(current)) ||
						(key == null && current == null)) {
					selectedIndex = index;
					break;
				}
				index++;
			}
			OptionsDialog.show(this, titleId, items, selectedIndex, (dialog, which) -> {
				Set<T> keys = optionsMap.keySet();
				int i = 0;
				for (T key : keys) {
					if (i++ == which) {
						setter.onSet(key);
						updateChoiceValue(pcv, optionsMap.get(getter.onGet()));
						break;
					}
				}
			});
		});
		updateChoiceValue(pcv, optionsMap.get(getter.onGet()));
	}

	private static <T> void updateChoiceValue(PreferenceChoiceView pcv, T value) {
		if (value instanceof Integer) {
			pcv.setValue((int) value);
		} else if (value instanceof String) {
			pcv.setValue((String) value);
		}
	}

	private <T, G> CharSequence[] getItemsFromOptions(Map<T, G> options) {
		CharSequence[] items = new CharSequence[options.size()];
		int i = 0;
		for (G value : options.values()) {
			items[i++] = getName(value);
		}
		return items;
	}

	private <T> String getName(T value) {
		if (value instanceof Integer) {
			return getString((int) value);
		} else if (value instanceof String) {
			return (String) value;
		} else {
			return null;
		}
	}

	private boolean updateDisableBatteryOptimizations() {
		if (BatteryOptimization.isIgnoringBatteryOptimizations(this)) {
			disableBatteryOptimizations.setVisibility(View.GONE);
			return true;
		} else {
			disableBatteryOptimizations.setVisibility(View.VISIBLE);
			disableBatteryOptimizations.setOnClickListener(v ->
					BatteryOptimization.requestDisable(
							PreferencesActivity.this));
			return false;
		}
	}

	private boolean updateDefaultLauncher() {
		if (DefaultLauncher.isDefault(this)) {
			defaultLauncherView.setVisibility(View.GONE);
			return true;
		} else {
			defaultLauncherView.setVisibility(View.VISIBLE);
			defaultLauncherView.setOnClickListener(v ->
					DefaultLauncher.setAsDefault(this));
			return false;
		}
	}

	private static void start(Context context, boolean welcome) {
		Intent intent = new Intent(context, PreferencesActivity.class);
		if (welcome) {
			intent.putExtra(WELCOME, true);
		}
		context.startActivity(intent);
	}

	private static Map<Integer, Integer> getOrientationOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
				R.string.orientation_portrait);
		map.put(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
				R.string.orientation_landscape);
		map.put(ActivityInfo.SCREEN_ORIENTATION_USER,
				R.string.orientation_user);
		return map;
	}

	private static Map<Integer, Integer> getDarkenBackgroundOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.DARKEN_BACKGROUND_NONE,
				R.string.darken_background_no);
		map.put(Preferences.DARKEN_BACKGROUND_LIGHT,
				R.string.darken_background_light);
		map.put(Preferences.DARKEN_BACKGROUND_HEAVY,
				R.string.darken_background_yes);
		return map;
	}

	private static Map<Integer, Integer> getDeadZoneOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.DEAD_ZONE_NONE, R.string.dead_zone_none);
		map.put(Preferences.DEAD_ZONE_TOP, R.string.dead_zone_top);
		map.put(Preferences.DEAD_ZONE_BOTTOM, R.string.dead_zone_bottom);
		map.put(Preferences.DEAD_ZONE_TOP_BOTTOM,
				R.string.dead_zone_top_bottom);
		map.put(Preferences.DEAD_ZONE_ALL, R.string.dead_zone_all);
		return map;
	}

	private static Map<Integer, Integer> getImmersiveModeOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.IMMERSIVE_MODE_DISABLED,
				R.string.immersive_mode_disabled);
		map.put(Preferences.IMMERSIVE_MODE_STATUS_BAR,
				R.string.immersive_mode_status_bar);
		map.put(Preferences.IMMERSIVE_MODE_NAVIGATION_BAR,
				R.string.immersive_mode_navigation_bar);
		map.put(Preferences.IMMERSIVE_MODE_FULL,
				R.string.immersive_mode_full);
		return map;
	}

	private static Map<Integer, Integer> getAlternateMenuOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.ALTERNATE_MENU_NONE,
				R.string.alternate_menu_none);
		map.put(Preferences.ALTERNATE_MENU_SECONDARY,
				R.string.alternate_menu_secondary);
		map.put(Preferences.ALTERNATE_MENU_ALL_APPS,
				R.string.alternate_menu_all_apps);
		map.put(Preferences.ALTERNATE_MENU_LAST_USED,
				R.string.alternate_menu_last_used);
		return map;
	}

	private static Map<Integer, Integer> getOpenListWithOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.OPEN_LIST_WITH_TAP,
				R.string.open_list_with_tap);
		map.put(Preferences.OPEN_LIST_WITH_ANY_TOUCH,
				R.string.open_list_with_any_touch);
		map.put(Preferences.OPEN_LIST_WITH_LONG_PRESS,
				R.string.open_list_with_long_press);
		map.put(Preferences.OPEN_LIST_WITH_DOUBLE_TAP,
				R.string.open_list_with_double_tap);
		map.put(Preferences.OPEN_LIST_WITH_TWO_FINGER_TAP,
				R.string.open_list_with_two_finger_tap);
		map.put(Preferences.OPEN_LIST_WITH_FOUR_FINGER_TAP,
				R.string.open_list_with_four_finger_tap);
		map.put(Preferences.OPEN_LIST_WITH_ICON,
				R.string.open_list_with_icon);
		return map;
	}

	private static Map<Integer, Integer> getListAnimationAppearanceOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.LIST_APPEARANCE_ANIMATION_SLIDE,
				R.string.list_animation_appearance_slide);
		map.put(Preferences.LIST_APPEARANCE_ANIMATION_FADE,
				R.string.list_animation_appearance_fade);
		return map;
	}

	private static Map<Integer, Integer> getSearchStrictnessOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SEARCH_STRICTNESS_HAMMING,
				R.string.search_strictness_hamming);
		map.put(Preferences.SEARCH_STRICTNESS_CONTAINS,
				R.string.search_strictness_contains);
		map.put(Preferences.SEARCH_STRICTNESS_STARTS_WITH,
				R.string.search_strictness_starts_with);
		return map;
	}

	private static Map<Integer, Integer> getSearchParameterOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SEARCH_PARAMETER_APP_LABEL,
				R.string.search_parameter_app_label);
		map.put(Preferences.SEARCH_PARAMETER_PACKAGE_NAME,
				R.string.search_parameter_package_name);
		return map;
	}

	private static Map<Integer, Integer> getAppSortingOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.APP_SORT_ALPHABETICAL,
				R.string.app_sorting_alphabetical);
		map.put(Preferences.APP_SORT_FRECENCY,
				R.string.app_sorting_frecency);
		return map;
	}

	private static Map<Integer, Integer> getShowAppNamesOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.SHOW_APP_NAMES_ALWAYS,
				R.string.show_app_names_always);
		map.put(Preferences.SHOW_APP_NAMES_SEARCH,
				R.string.show_app_names_search);
		map.put(Preferences.SHOW_APP_NAMES_NEVER,
				R.string.show_app_names_never);
		return map;
	}

	private static Map<Integer, Integer> getExcludePieOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.EXCLUDE_PIE_NONE, R.string.exclude_pie_no);
		map.put(Preferences.EXCLUDE_PIE_PRIMARY, R.string.exclude_pie_yes);
		map.put(Preferences.EXCLUDE_PIE_ALL, R.string.exclude_pie_all);
		return map;
	}

	private static Map<Integer, Integer> getIconPressOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.ICON_PRESS_DEFAULT, R.string.icon_press_default);
		map.put(Preferences.ICON_PRESS_LONGER, R.string.icon_press_longer);
		map.put(Preferences.ICON_PRESS_MENU, R.string.icon_press_menu);
		map.put(Preferences.ICON_LOCK_MENU, R.string.icon_lock_menu);
		return map;
	}

	private Map<String, String> getIconPackOptions() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put(null, getString(R.string.icon_pack_default));
		map.putAll(PieLauncherApp.iconPack.getIconPacks());
		return map;
	}

	private static Map<Integer, Integer> getHapticFeedbackOptions() {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(Preferences.HAPTIC_FEEDBACK_FOLLOW_SYSTEM,
				R.string.haptic_feedback_follow_system);
		map.put(Preferences.HAPTIC_FEEDBACK_DISABLE_LAUNCH,
				R.string.haptic_feedback_disable_launch);
		map.put(Preferences.HAPTIC_FEEDBACK_DISABLE_ALL,
				R.string.haptic_feedback_disable_all);
		return map;
	}

	private interface GetOptionsListener<T, G> {
		Map<T, G> onGetOptions();
	}

	private interface GetListener<T> {
		T onGet();
	}

	private interface SetListener<T> {
		void onSet(T value);
	}

	private interface Initializer {
		void onInit();
	}
}
