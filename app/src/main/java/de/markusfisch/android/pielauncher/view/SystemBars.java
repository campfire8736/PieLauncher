package de.markusfisch.android.pielauncher.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class SystemBars {
	public interface OnInsetListener {
		void onApplyInsets(int left, int top, int right, int bottom);
	}

	public interface OnWallpaperColorsChangedListener {
		void onWallpaperColorsChanged(boolean isWallpaperLight);
	}

	public static void setTransparentSystemBars(Window window) {
		setTransparentSystemBars(window, Preferences.IMMERSIVE_MODE_DISABLED);
	}

	public static void setTransparentSystemBars(Window window, int immersive) {
		if (window == null) {
			return;
		}
		// This is important or subsequent (not the very first!) openings of
		// the soft keyboard will reposition the DecorView according to the
		// window insets.
		window.setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		setSystemUIVisibility(window, immersive);
		// System bars can no longer be colored from VANILLA_ICE_CREAM on.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			window.setStatusBarColor(0);
			window.setNavigationBarColor(0);
		}
	}

	public static void setSystemUIVisibility(Window window, int immersive) {
		if (window == null) {
			return;
		}

		int uiFlags = 0;
		boolean hideStatusBar =
				(immersive & Preferences.IMMERSIVE_MODE_STATUS_BAR) > 0;
		boolean hideNavigationBar =
				(immersive & Preferences.IMMERSIVE_MODE_NAVIGATION_BAR) > 0;
		boolean hideAnyBar = hideStatusBar || hideNavigationBar;
		if (hideStatusBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN;
		}
		if (hideNavigationBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		}
		if (hideAnyBar) {
			uiFlags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		window.getDecorView().setSystemUiVisibility(uiFlags |
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

		if (hideAnyBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			WindowManager.LayoutParams params = window.getAttributes();
			params.layoutInDisplayCutoutMode =
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
							? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
							: WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			window.setAttributes(params);
		}
	}

	public static void setNavigationBarColor(Window window, int color) {
		if (window == null ||
				// setNavigationBarColor() has no effect over UPSIDE_DOWN_CAKE.
				Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			return;
		}
		window.setNavigationBarColor(color);
	}

	public static void setLightSystemBars(Window window, boolean isLight) {
		if (window == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			android.view.WindowInsetsController controller =
					window.getInsetsController();
			if (controller != null) {
				int appearance = android.view.WindowInsetsController
						.APPEARANCE_LIGHT_STATUS_BARS |
						android.view.WindowInsetsController
						.APPEARANCE_LIGHT_NAVIGATION_BARS;
				if (isLight) {
					controller.setSystemBarsAppearance(appearance, appearance);
				} else {
					controller.setSystemBarsAppearance(0, appearance);
				}
				return;
			}
		}
		int flags = window.getDecorView().getSystemUiVisibility();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (isLight) {
				flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
			} else {
				flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
			}
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (isLight) {
				flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
			} else {
				flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
			}
		}
		window.getDecorView().setSystemUiVisibility(flags);
	}

	public static boolean isWallpaperLight(Context context) {
		if (context == null) {
			return false;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			try {
				WallpaperManager wm = WallpaperManager.getInstance(context);
				if (wm != null) {
					WallpaperColors colors = wm.getWallpaperColors(
							WallpaperManager.FLAG_SYSTEM);
					if (colors != null) {
						return (colors.getColorHints() &
								WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
					}
				}
			} catch (Throwable t) {
				// Fall through
			}
		}
		try {
			WallpaperManager wm = WallpaperManager.getInstance(context);
			if (wm != null) {
				Drawable drawable = wm.getDrawable();
				if (drawable instanceof BitmapDrawable) {
					Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
					if (bitmap != null) {
						return isBitmapLight(bitmap);
					}
				}
			}
		} catch (Throwable t) {
			// Fall through
		}
		try {
			return PieLauncherApp.getPrefs(context).isLight(context);
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean isBitmapLight(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		if (width <= 0 || height <= 0) {
			return false;
		}
		long totalLuminance = 0;
		int samples = 0;
		int stepX = Math.max(1, width / 4);
		int maxY = Math.min(height, Math.max(1, height / 4));
		int stepY = Math.max(1, maxY / 4);
		for (int y = 0; y < maxY; y += stepY) {
			for (int x = 0; x < width; x += stepX) {
				int pixel = bitmap.getPixel(x, y);
				int r = (pixel >> 16) & 0xff;
				int g = (pixel >> 8) & 0xff;
				int b = pixel & 0xff;
				totalLuminance += (r * 299L + g * 587L + b * 114L) / 1000L;
				samples++;
			}
		}
		return samples > 0 && (totalLuminance / samples) > 128;
	}

	public static Object registerWallpaperColorsListener(
			Context context,
			OnWallpaperColorsChangedListener listener) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
				context != null && listener != null) {
			return Api27Impl.registerWallpaperColorsListener(context, listener);
		}
		return null;
	}

	public static void unregisterWallpaperColorsListener(
			Context context,
			Object registration) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
				context != null && registration != null) {
			Api27Impl.unregisterWallpaperColorsListener(context, registration);
		}
	}

	@TargetApi(Build.VERSION_CODES.O_MR1)
	private static class Api27Impl {
		static Object registerWallpaperColorsListener(
				Context context,
				OnWallpaperColorsChangedListener listener) {
			try {
				WallpaperManager wm = WallpaperManager.getInstance(context);
				if (wm == null) {
					return null;
				}
				WallpaperManager.OnColorsChangedListener osListener =
						(colors, which) -> {
							if ((which & WallpaperManager.FLAG_SYSTEM) != 0) {
								boolean isLight = colors != null &&
										(colors.getColorHints() &
												WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
								listener.onWallpaperColorsChanged(isLight);
							}
						};
				wm.addOnColorsChangedListener(osListener,
						new Handler(Looper.getMainLooper()));
				return osListener;
			} catch (Throwable t) {
				return null;
			}
		}

		static void unregisterWallpaperColorsListener(
				Context context,
				Object registration) {
			try {
				if (registration instanceof WallpaperManager.OnColorsChangedListener) {
					WallpaperManager wm = WallpaperManager.getInstance(context);
					if (wm != null) {
						wm.removeOnColorsChangedListener(
								(WallpaperManager.OnColorsChangedListener) registration);
					}
				}
			} catch (Throwable t) {
				// Ignore
			}
		}
	}

	public static void listenForWindowInsets(
			View view,
			OnInsetListener listener) {
		view.setOnApplyWindowInsetsListener((v, insets) -> {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				Insets systemBarsInsets = insets.getInsets(
						WindowInsets.Type.systemBars());
				listener.onApplyInsets(
						systemBarsInsets.left,
						systemBarsInsets.top,
						systemBarsInsets.right,
						systemBarsInsets.bottom);
				return insets;
			} else {
				//noinspection deprecation
				if (insets.hasSystemWindowInsets()) {
					//noinspection deprecation
					listener.onApplyInsets(
							insets.getSystemWindowInsetLeft(),
							insets.getSystemWindowInsetTop(),
							insets.getSystemWindowInsetRight(),
							insets.getSystemWindowInsetBottom());
				}
				//noinspection deprecation
				return insets.consumeSystemWindowInsets();
			}
		});
	}

	public static void addPaddingFromWindowInsets(
			View toolbar,
			View content) {
		Rect contentPadding = new Rect(
				content.getPaddingLeft(),
				content.getPaddingTop(),
				content.getPaddingRight(),
				content.getPaddingBottom());
		final int toolbarHeight;
		final Rect toolbarPadding;
		if (toolbar == null) {
			toolbarHeight = 0;
			toolbarPadding = null;
		} else {
			toolbar.measure(0, 0);
			toolbarHeight = toolbar.getMeasuredHeight();
			toolbarPadding = new Rect(
					toolbar.getPaddingLeft(),
					toolbar.getPaddingTop(),
					toolbar.getPaddingRight(),
					toolbar.getPaddingBottom());
		}
		SystemBars.listenForWindowInsets(
				content,
				(left, top, right, bottom) -> {
					content.setPadding(
							contentPadding.left + left,
							contentPadding.top + top + toolbarHeight,
							contentPadding.right + right,
							contentPadding.bottom + bottom);
					if (toolbar != null) {
						toolbar.setPadding(
								toolbarPadding.left + left,
								toolbarPadding.top + top,
								toolbarPadding.right + right,
								// Skip bottom inset because the toolbar
								// doesn't touch the lower edge.
								toolbarPadding.bottom);
					}
				});
	}

	public static void expandNotificationPanel(Context context) {
		if (context == null) {
			return;
		}
		try {
			@SuppressLint("WrongConstant")
			Object statusBarService = context.getSystemService("statusbar");
			if (statusBarService != null) {
				Class<?> statusBarManager = Class.forName(
						"android.app.StatusBarManager");
				//noinspection JavaReflectionMemberAccess
				statusBarManager.getMethod("expandNotificationsPanel")
						.invoke(statusBarService);
			}
		} catch (Exception e) {
			// Silently fail if reflection doesn't work.
		}
	}
}
