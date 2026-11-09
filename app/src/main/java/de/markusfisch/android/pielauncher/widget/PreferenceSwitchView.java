package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.graphics.Ripple;
import de.markusfisch.android.pielauncher.preference.Preferences;

public class PreferenceSwitchView extends LinearLayout {
	public interface OnCheckedChangeListener {
		void onCheckedChanged(boolean checked);
	}

	private final Ripple ripple = Ripple.newPressRipple();

	private Preferences prefs;
	private TextView titleView;
	private TextView descriptionView;
	private Switch switchView;
	private OnCheckedChangeListener listener;

	private int titleResId = 0;
	private int descriptionResId = 0;

	public PreferenceSwitchView(Context context) {
		super(context);
		init(context, null);
	}

	public PreferenceSwitchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PreferenceSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public void setPreference(
			int titleId,
			int descriptionId,
			boolean checked,
			OnCheckedChangeListener listener) {
		this.listener = null;
		this.titleResId = titleId;
		this.descriptionResId = descriptionId;
		updateText();
		switchView.setChecked(checked);
		this.listener = listener;
	}

	public void setChecked(boolean checked) {
		if (switchView.isChecked() != checked) {
			switchView.setChecked(checked);
		}
	}

	public boolean isChecked() {
		return switchView.isChecked();
	}

	public Switch getSwitch() {
		return switchView;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (ripple.draw(canvas, prefs)) {
			invalidate();
		}
	}

	private void init(Context context, AttributeSet attrs) {
		setWillNotDraw(false);
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		prefs = PieLauncherApp.getPrefs(context);

		LinearLayout textContainer = new LinearLayout(context);
		textContainer.setOrientation(VERTICAL);
		LayoutParams textLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
		int marginEnd = (int) (getResources().getDisplayMetrics().density * 16);
		textLp.setMarginEnd(marginEnd);
		textLp.rightMargin = marginEnd;
		textContainer.setLayoutParams(textLp);

		titleView = new TextView(context);
		titleView.setTextColor(PreferenceSliderView.getColorFromAttribute(
				context,
				android.R.attr.textColorPrimary,
				getResources().getColor(R.color.text_color)));
		titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
		titleView.setTypeface(null, android.graphics.Typeface.BOLD);
		textContainer.addView(titleView);

		descriptionView = new TextView(context);
		descriptionView.setTextColor(PreferenceSliderView.getColorFromAttribute(
				context,
				android.R.attr.textColorSecondary,
				getResources().getColor(R.color.text_color_preference)));
		descriptionView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
		descriptionView.setVisibility(View.GONE);
		LayoutParams descLp = new LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		descLp.topMargin = (int) (getResources().getDisplayMetrics().density * 2);
		descriptionView.setLayoutParams(descLp);
		textContainer.addView(descriptionView);

		addView(textContainer);

		switchView = new Switch(context);
		switchView.setFocusable(false);
		switchView.setClickable(false);
		switchView.setSplitTrack(false);
		switchView.setShowText(false);
		switchView.setThumbTextPadding(0);
		switchView.setSwitchMinWidth((int) (getResources().getDisplayMetrics().density * 52));
		switchView.setTrackDrawable(context.getDrawable(R.drawable.switch_track));
		switchView.setThumbDrawable(context.getDrawable(R.drawable.switch_thumb));
		LayoutParams switchLp = new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		switchView.setLayoutParams(switchLp);
		addView(switchView);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, new int[]{
					android.R.attr.text
			});
			CharSequence text = a.getText(0);
			if (text != null) {
				titleView.setText(text);
			}
			a.recycle();
		}

		setOnTouchListener(ripple.getOnTouchListener());
		setOnClickListener(v -> {
			boolean newState = !switchView.isChecked();
			switchView.setChecked(newState);
			if (listener != null) {
				listener.onCheckedChanged(newState);
			}
		});
	}

	private void updateText() {
		Context context = getContext();
		if (titleResId != 0) {
			titleView.setText(context.getString(titleResId));
		}
		if (descriptionResId > 0) {
			descriptionView.setText(context.getString(descriptionResId));
			descriptionView.setVisibility(VISIBLE);
		} else {
			descriptionView.setVisibility(GONE);
		}
	}
}
