package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;

public class PreferenceActionView extends LinearLayout {
	private ImageView iconView;
	private TextView titleView;
	private TextView summaryView;
	private ImageView chevronView;

	public PreferenceActionView(Context context) {
		super(context);
		init(context, null);
	}

	public PreferenceActionView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PreferenceActionView(Context context, AttributeSet attrs, int defStyleAttr) {
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

		LayoutInflater.from(context).inflate(R.layout.preference_action, this, true);
		iconView = findViewById(R.id.icon);
		titleView = findViewById(R.id.title);
		summaryView = findViewById(R.id.summary);
		chevronView = findViewById(R.id.chevron);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceView);
			String title = a.getString(R.styleable.PreferenceView_title);
			if (title != null) {
				titleView.setText(title);
			}
			String summary = a.getString(R.styleable.PreferenceView_summary);
			if (summary != null) {
				setSummary(summary);
			}
			Drawable icon = a.getDrawable(R.styleable.PreferenceView_icon);
			if (icon != null) {
				setIcon(icon);
			}
			a.recycle();
		}
	}

	public void setIcon(int iconResId) {
		iconView.setImageResource(iconResId);
		iconView.setVisibility(VISIBLE);
	}

	public void setIcon(Drawable icon) {
		iconView.setImageDrawable(icon);
		iconView.setVisibility(icon != null ? VISIBLE : GONE);
	}

	public void setTitle(int titleResId) {
		titleView.setText(titleResId);
	}

	public void setTitle(CharSequence title) {
		titleView.setText(title);
	}

	public void setSummary(int summaryResId) {
		summaryView.setText(summaryResId);
		summaryView.setVisibility(VISIBLE);
	}

	public void setSummary(CharSequence summary) {
		if (summary != null && summary.length() > 0) {
			summaryView.setText(summary);
			summaryView.setVisibility(VISIBLE);
		} else {
			summaryView.setVisibility(GONE);
		}
	}

	public void setChevronVisible(boolean visible) {
		chevronView.setVisibility(visible ? VISIBLE : GONE);
	}
}
