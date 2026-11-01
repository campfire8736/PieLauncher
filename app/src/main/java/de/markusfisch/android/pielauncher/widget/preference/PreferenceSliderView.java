package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;

public class PreferenceSliderView extends LinearLayout {
	public interface ValueFormatter {
		String format(int value);
	}

	public interface OnSliderChangeListener {
		void onProgressChanged(PreferenceSliderView view, int progress, boolean fromUser);
	}

	private TextView titleView;
	private TextView summaryView;
	private TextView valueTextView;
	private SeekBar seekBar;
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

	private void init(Context context, AttributeSet attrs) {
		setOrientation(VERTICAL);
		int padding = Math.round(16f * context.getResources().getDisplayMetrics().density);
		setPadding(padding, padding, padding, padding);

		LayoutInflater.from(context).inflate(R.layout.preference_slider, this, true);
		titleView = findViewById(R.id.title);
		summaryView = findViewById(R.id.summary);
		valueTextView = findViewById(R.id.value_text);
		seekBar = findViewById(R.id.seek_bar);

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

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
				updateValueText(progress);
				if (listener != null) {
					listener.onProgressChanged(PreferenceSliderView.this, progress, fromUser);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar sb) {}

			@Override
			public void onStopTrackingTouch(SeekBar sb) {}
		});
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

	public void setMax(int max) {
		seekBar.setMax(max);
	}

	public int getMax() {
		return seekBar.getMax();
	}

	public void setProgress(int progress) {
		seekBar.setProgress(progress);
		updateValueText(progress);
	}

	public int getProgress() {
		return seekBar.getProgress();
	}

	public void setValueFormatter(ValueFormatter formatter) {
		this.formatter = formatter;
		updateValueText(seekBar.getProgress());
	}

	public void setOnSliderChangeListener(OnSliderChangeListener listener) {
		this.listener = listener;
	}

	private void updateValueText(int progress) {
		if (formatter != null) {
			valueTextView.setText(formatter.format(progress));
		} else {
			valueTextView.setText(String.valueOf(progress));
		}
	}
}
