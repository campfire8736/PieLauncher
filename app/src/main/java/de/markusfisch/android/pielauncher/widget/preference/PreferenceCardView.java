package de.markusfisch.android.pielauncher.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

import de.markusfisch.android.pielauncher.R;

public class PreferenceCardView extends LinearLayout {
	private final Path clipPath = new Path();
	private final RectF rect = new RectF();
	private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint strokePaint;
	private float cornerRadius;
	private int cardColor;

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
		setWillNotDraw(false);
		float defaultRadius = context.getResources().getDimension(R.dimen.card_corner_radius);
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceCardView);
			cornerRadius = a.getDimension(R.styleable.PreferenceCardView_cardCornerRadius, defaultRadius);
			a.recycle();
		} else {
			cornerRadius = defaultRadius;
		}

		cardColor = context.getResources().getColor(R.color.bg_card);
		cardPaint.setColor(cardColor);

		float strokeWidth = context.getResources().getDimension(R.dimen.card_stroke_width);
		int strokeColor = context.getResources().getColor(R.color.card_stroke);
		if (strokeWidth > 0 && strokeColor != 0) {
			strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			strokePaint.setColor(strokeColor);
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(strokeWidth);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setOutlineProvider(ViewOutlineProvider.BACKGROUND);
			setClipToOutline(true);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (getBackground() == null && cardColor != 0) {
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				if (child.getVisibility() == View.VISIBLE) {
					rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
					if (cornerRadius > 0) {
						canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cardPaint);
						if (strokePaint != null) {
							canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint);
						}
					} else {
						canvas.drawRect(rect, cardPaint);
						if (strokePaint != null) {
							canvas.drawRect(rect, strokePaint);
						}
					}
				}
			}
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (cornerRadius > 0) {
			clipPath.reset();
			if (getBackground() != null) {
				rect.set(0, 0, getWidth(), getHeight());
				clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
			} else {
				int childCount = getChildCount();
				for (int i = 0; i < childCount; i++) {
					View child = getChildAt(i);
					if (child.getVisibility() == View.VISIBLE) {
						rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
						clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
					}
				}
			}
			int saveCount = canvas.save();
			canvas.clipPath(clipPath);
			super.dispatchDraw(canvas);
			canvas.restoreToCount(saveCount);
		} else {
			super.dispatchDraw(canvas);
		}
	}
}
