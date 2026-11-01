package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;

public class PreferenceSwitchView extends LinearLayout {
	public interface OnCheckedChangeListener {
		void onCheckedChanged(PreferenceSwitchView view, boolean isChecked);
	}

	private TextView titleView;
	private TextView summaryView;
	private Switch switchWidget;
	private OnCheckedChangeListener listener;

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

		LayoutInflater.from(context).inflate(R.layout.preference_switch, this, true);
		titleView = findViewById(R.id.title);
		summaryView = findViewById(R.id.summary);
		switchWidget = findViewById(R.id.switch_widget);

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
			a.recycle();
		}
	}

	@Override
	public boolean performClick() {
		toggle();
		return super.performClick();
	}

	@Override
	protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
		dispatchFreezeSelfOnly(container);
	}

	@Override
	protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
		dispatchThawSelfOnly(container);
	}

	public void toggle() {
		setChecked(!switchWidget.isChecked());
		if (listener != null) {
			listener.onCheckedChanged(this, switchWidget.isChecked());
		}
	}

	public void setTitle(int titleResId) {
		titleView.setText(titleResId);
	}

	public void setTitle(CharSequence title) {
		titleView.setText(title);
	}

	public void setSummary(int summaryResId) {
		summaryView.setText(summaryResId);
		summaryView.setVisibility(View.VISIBLE);
	}

	public void setSummary(CharSequence summary) {
		if (summary != null && summary.length() > 0) {
			summaryView.setText(summary);
			summaryView.setVisibility(View.VISIBLE);
		} else {
			summaryView.setVisibility(View.GONE);
		}
	}

	public void setChecked(boolean checked) {
		switchWidget.setChecked(checked);
	}

	public boolean isChecked() {
		return switchWidget.isChecked();
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		this.listener = listener;
	}
}
