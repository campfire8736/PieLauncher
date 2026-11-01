package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;

public class PreferenceChoiceView extends LinearLayout {
	private TextView titleView;
	private TextView valueTextView;
	private ImageView indicatorView;

	public PreferenceChoiceView(Context context) {
		super(context);
		init(context, null);
	}

	public PreferenceChoiceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PreferenceChoiceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		int padding = Math.round(16f * context.getResources().getDisplayMetrics().density);
		setPadding(padding, padding, padding, padding);
		setFocusable(true);
		setClickable(true);

		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(
				android.R.attr.selectableItemBackground,
				outValue,
				true);
		setBackgroundResource(outValue.resourceId);

		LayoutInflater.from(context).inflate(R.layout.preference_choice, this, true);
		titleView = findViewById(R.id.title);
		valueTextView = findViewById(R.id.value_text);
		indicatorView = findViewById(R.id.indicator);

		if (de.markusfisch.android.pielauncher.app.PieLauncherApp.getPrefs(context).invertColors()) {
			int textColor = context.getResources().getColor(R.color.text_color_inverted);
			indicatorView.setColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN);
		}

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceView);
			String title = a.getString(R.styleable.PreferenceView_title);
			if (title != null) {
				titleView.setText(title);
			}
			a.recycle();
		}
	}

	public void setTitle(int titleResId) {
		titleView.setText(titleResId);
	}

	public void setTitle(CharSequence title) {
		titleView.setText(title);
	}

	public void setValue(int valueResId) {
		valueTextView.setText(valueResId);
	}

	public void setValue(CharSequence value) {
		valueTextView.setText(value);
	}

	public void setIndicatorVisible(boolean visible) {
		indicatorView.setVisibility(visible ? VISIBLE : GONE);
	}
}
