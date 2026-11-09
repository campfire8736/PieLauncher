package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
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
	private TextView textView;
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

		textView = new TextView(context);
		textView.setTextColor(getResources().getColor(R.color.text_color_preference));
		textView.setLineSpacing(getResources().getDisplayMetrics().density * 3, 1f);
		LayoutParams textLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
		int marginEnd = (int) (getResources().getDisplayMetrics().density * 16);
		textLp.setMarginEnd(marginEnd);
		textLp.rightMargin = marginEnd;
		textView.setLayoutParams(textLp);
		addView(textView);

		switchView = new Switch(context);
		switchView.setFocusable(false);
		switchView.setClickable(false);
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
				textView.setText(text);
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
		if (titleResId == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<big><font color=\"#ffffff\">")
				.append(context.getString(titleResId))
				.append("</font></big>");
		if (descriptionResId > 0) {
			sb.append("<br/><font color=\"#afff\"><small>")
					.append(context.getString(descriptionResId))
					.append("</small></font>");
		}
		Spanned text = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
				? Html.fromHtml(sb.toString())
				: Html.fromHtml(sb.toString(), 0);
		textView.setText(text);
	}
}
