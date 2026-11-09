package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;

public class PreferenceSliderView extends LinearLayout {
	public interface ValueFormatter {
		String format(int value);
	}

	public interface OnSliderChangeListener {
		void onSliderChanged(int value, boolean isFinal);
	}

	private TextView titleView;
	private TextView valueView;
	private TextView descriptionView;
	private SeekBar seekBar;

	private int min = 0;
	private int max = 100;
	private int step = 1;
	private ValueFormatter formatter;
	private OnSliderChangeListener listener;

	public PreferenceSliderView(Context context) {
		super(context);
		init(context, null);
	}

	public PreferenceSliderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PreferenceSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public void setPreference(
			int titleId,
			int descriptionId,
			int min,
			int max,
			int step,
			int currentValue,
			ValueFormatter formatter,
			OnSliderChangeListener listener) {
		this.min = min;
		this.max = max;
		this.step = step <= 0 ? 1 : step;
		this.formatter = formatter;
		this.listener = listener;

		Context context = getContext();
		if (titleId > 0) {
			titleView.setText(context.getString(titleId));
		}
		if (descriptionId > 0) {
			descriptionView.setText(context.getString(descriptionId));
			descriptionView.setVisibility(View.VISIBLE);
		} else {
			descriptionView.setVisibility(View.GONE);
		}

		int maxProgress = (max - min) / this.step;
		seekBar.setMax(maxProgress);
		setValue(currentValue);
	}

	public void setValue(int value) {
		int progress = (clamp(value, min, max) - min) / step;
		seekBar.setProgress(progress);
		updateValueLabel(value);
	}

	public int getValue() {
		return min + seekBar.getProgress() * step;
	}

	public SeekBar getSeekBar() {
		return seekBar;
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(VERTICAL);

		LinearLayout headerRow = new LinearLayout(context);
		headerRow.setOrientation(HORIZONTAL);
		headerRow.setGravity(Gravity.CENTER_VERTICAL);
		LayoutParams headerLp = new LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		headerRow.setLayoutParams(headerLp);

		titleView = new TextView(context);
		titleView.setTextColor(getResources().getColor(R.color.text_color));
		titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		titleView.setTypeface(null, android.graphics.Typeface.BOLD);
		LayoutParams titleLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
		int marginEnd = (int) (getResources().getDisplayMetrics().density * 8);
		titleLp.setMarginEnd(marginEnd);
		titleLp.rightMargin = marginEnd;
		titleView.setLayoutParams(titleLp);
		headerRow.addView(titleView);

		valueView = new TextView(context);
		TypedValue accentValue = new TypedValue();
		if (context.getTheme().resolveAttribute(android.R.attr.colorAccent, accentValue, true)) {
			valueView.setTextColor(accentValue.data);
		} else {
			valueView.setTextColor(getResources().getColor(R.color.text_color_preference));
		}
		valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		valueView.setTypeface(null, android.graphics.Typeface.BOLD);
		LayoutParams valueLp = new LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		valueView.setLayoutParams(valueLp);
		headerRow.addView(valueView);

		addView(headerRow);

		descriptionView = new TextView(context);
		descriptionView.setTextColor(getResources().getColor(R.color.text_color_preference));
		descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		descriptionView.setVisibility(View.GONE);
		LayoutParams descLp = new LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		descLp.topMargin = (int) (getResources().getDisplayMetrics().density * 2);
		descriptionView.setLayoutParams(descLp);
		addView(descriptionView);

		seekBar = new SeekBar(context);
		LayoutParams seekBarLp = new LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		int padTop = (int) (getResources().getDisplayMetrics().density * 8);
		int padBottom = (int) (getResources().getDisplayMetrics().density * 2);
		seekBarLp.topMargin = padTop;
		seekBarLp.bottomMargin = padBottom;
		seekBar.setLayoutParams(seekBarLp);
		addView(seekBar);

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

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
				int val = min + progress * step;
				updateValueLabel(val);
				if (fromUser && listener != null) {
					listener.onSliderChanged(val, false);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {
				int val = min + sb.getProgress() * step;
				updateValueLabel(val);
				if (listener != null) {
					listener.onSliderChanged(val, true);
				}
			}
		});
	}

	private void updateValueLabel(int val) {
		if (formatter != null) {
			valueView.setText(formatter.format(val));
		} else {
			valueView.setText(String.valueOf(val));
		}
	}

	private static int clamp(int val, int min, int max) {
		return Math.max(min, Math.min(max, val));
	}
}
