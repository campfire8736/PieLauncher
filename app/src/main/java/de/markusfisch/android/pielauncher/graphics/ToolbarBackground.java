package de.markusfisch.android.pielauncher.graphics;

import android.content.res.Resources;

import de.markusfisch.android.pielauncher.R;

public class ToolbarBackground {
	public final int baseColor;
	public final int backgroundColor;

	private final float threshold;

	public ToolbarBackground(Resources res) {
		this(res, 0);
	}

	public ToolbarBackground(Resources res, int baseColor) {
		this(res, baseColor, res.getColor(R.color.bg_search_bar));
	}

	public ToolbarBackground(Resources res, int baseColor, int backgroundColor) {
		this.baseColor = baseColor;
		this.backgroundColor = backgroundColor;
		threshold = res.getDisplayMetrics().density * 48f;
	}

	public int getColorForY(int y) {
		y = Math.abs(y);
		return y > 0
				? blendColor(baseColor, backgroundColor, y / threshold)
				: baseColor;
	}

	private static int blendColor(int from, int to, float fraction) {
		fraction = Math.max(0f, Math.min(1f, fraction));
		int a1 = (from >> 24) & 0xff;
		int r1 = (from >> 16) & 0xff;
		int g1 = (from >> 8) & 0xff;
		int b1 = from & 0xff;

		int a2 = (to >> 24) & 0xff;
		int r2 = (to >> 16) & 0xff;
		int g2 = (to >> 8) & 0xff;
		int b2 = to & 0xff;

		int a = Math.round(a1 + (a2 - a1) * fraction);
		int r = Math.round(r1 + (r2 - r1) * fraction);
		int g = Math.round(g1 + (g2 - g1) * fraction);
		int b = Math.round(b1 + (b2 - b1) * fraction);

		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
