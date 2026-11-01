package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

import de.markusfisch.android.pielauncher.R;

public class PreferenceCardView extends LinearLayout {
	private final Path clipPath = new Path();
	private final RectF rect = new RectF();
	private float cornerRadius;

	public PreferenceCardView(Context context) {
		super(context);
		init(context, null);
	}

	public PreferenceCardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PreferenceCardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		float defaultRadius = context.getResources().getDimension(R.dimen.card_corner_radius);
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceCardView);
			cornerRadius = a.getDimension(R.styleable.PreferenceCardView_cardCornerRadius, defaultRadius);
			a.recycle();
		} else {
			cornerRadius = defaultRadius;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setOutlineProvider(ViewOutlineProvider.BACKGROUND);
			setClipToOutline(true);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (cornerRadius > 0) {
			rect.set(0, 0, w, h);
			clipPath.reset();
			clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (cornerRadius > 0) {
			int saveCount = canvas.save();
			canvas.clipPath(clipPath);
			super.dispatchDraw(canvas);
			canvas.restoreToCount(saveCount);
		} else {
			super.dispatchDraw(canvas);
		}
	}
}
