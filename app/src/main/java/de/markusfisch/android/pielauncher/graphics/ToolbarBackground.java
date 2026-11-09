package de.markusfisch.android.pielauncher.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import de.markusfisch.android.pielauncher.R;

public class ToolbarBackground {
	public final int backgroundColor;

	private final float threshold;

	public ToolbarBackground(Resources res) {
		this(res, res.getColor(R.color.bg_search_bar));
	}

	public ToolbarBackground(Context context) {
		this(context.getResources(), resolveSearchBarColor(context));
	}

	public ToolbarBackground(Resources res, int backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.threshold = res.getDisplayMetrics().density * 48f;
	}

	private static int resolveSearchBarColor(Context context) {
		TypedValue tv = new TypedValue();
		if (context.getTheme().resolveAttribute(R.attr.colorSearchBar, tv, true) && tv.data != 0) {
			return tv.data;
		}
		return context.getResources().getColor(R.color.bg_search_bar);
	}

	public int getColorForY(int y) {
		y = Math.abs(y);
		return y > 0
				? fadeColor(backgroundColor, y / threshold)
				: 0;
	}

	private static int fadeColor(int argb, float fraction) {
		fraction = Math.max(0f, Math.min(1f, fraction));
		int alpha = (argb >> 24) & 0xff;
		int rgb = argb & 0xffffff;
		return rgb | Math.round(fraction * alpha) << 24;
	}
}
