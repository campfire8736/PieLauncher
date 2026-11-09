package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.Ripple;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class PreferenceView extends TextView {
	private final Ripple ripple = Ripple.newPressRipple();

	private Preferences prefs;

	public PreferenceView(Context context) {
		super(context);
		init(context);
	}

	public PreferenceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PreferenceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(formatText(text), type);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (ripple.draw(canvas, prefs)) {
			invalidate();
		}
	}

	private void init(Context context) {
		prefs = PieLauncherApp.getPrefs(context);
		ripple.setColor(PreferenceSliderView.getColorFromAttribute(
				context, R.attr.colorSeparator, 0x22ffffff));
		setOnTouchListener(ripple.getOnTouchListener());
		CharSequence text = getText();
		if (text != null && text.length() > 0) {
			setText(formatText(text));
		}
	}

	private CharSequence formatText(CharSequence text) {
		if (text == null || text.length() == 0) {
			return text;
		}
		if (text instanceof Spanned) {
			Spanned spanned = (Spanned) text;
			ForegroundColorSpan[] colorSpans = spanned.getSpans(
					0, spanned.length(), ForegroundColorSpan.class);
			if (colorSpans != null && colorSpans.length > 0) {
				return text;
			}
		}

		Context context = getContext();
		if (context == null) {
			return text;
		}

		int primaryColor = PreferenceSliderView.getColorFromAttribute(
				context,
				android.R.attr.textColorPrimary,
				0xffffffff);
		int secondaryColor = PreferenceSliderView.getColorFromAttribute(
				context,
				android.R.attr.textColorSecondary,
				0xafffffff);

		String raw = text.toString();
		int newline = raw.indexOf('\n');
		SpannableStringBuilder ssb = new SpannableStringBuilder();

		if (newline > 0) {
			String title = raw.substring(0, newline);
			String desc = raw.substring(newline + 1);
			ssb.append(title);
			ssb.setSpan(new RelativeSizeSpan(1.15f), 0,
					title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(new ForegroundColorSpan(primaryColor), 0,
					title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(new StyleSpan(Typeface.BOLD),
					0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.append("\n");
			int descStart = ssb.length();
			ssb.append(desc);
			ssb.setSpan(new ForegroundColorSpan(secondaryColor),
					descStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			ssb.append(raw);
			ssb.setSpan(new RelativeSizeSpan(1.15f), 0,
					ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(new ForegroundColorSpan(primaryColor), 0,
					ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(new StyleSpan(Typeface.BOLD),
					0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		return ssb;
	}
}
