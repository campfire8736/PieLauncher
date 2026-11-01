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
	private final Path childPath = new Path();
	private final RectF rect = new RectF();
	private final float[] radii = new float[8];
	private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint strokePaint;
	private float cornerRadius;
	private int cardColor;
	private boolean inverted;

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

		inverted = de.markusfisch.android.pielauncher.app.PieLauncherApp.getPrefs(context).invertColors();
		cardColor = context.getResources().getColor(inverted
				? R.color.bg_card_inverted
				: R.color.bg_card);
		cardPaint.setColor(cardColor);

		float strokeWidth = context.getResources().getDimension(R.dimen.card_stroke_width);
		int strokeColor = context.getResources().getColor(inverted
				? R.color.card_stroke_inverted
				: R.color.card_stroke);
		if (strokeWidth > 0 && strokeColor != 0) {
			strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			strokePaint.setColor(strokeColor);
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(strokeWidth);
		}

		if (inverted) {
			setDividerDrawable(context.getResources().getDrawable(
					R.drawable.preference_divider_inverted));
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setOutlineProvider(ViewOutlineProvider.BACKGROUND);
			setClipToOutline(true);
		}
	}

	private static void getChildRadii(int index, int firstIndex, int lastIndex, float r, float[] outRadii) {
		float topR = (index == firstIndex) ? r : 0f;
		float bottomR = (index == lastIndex) ? r : 0f;

		outRadii[0] = topR;
		outRadii[1] = topR;
		outRadii[2] = topR;
		outRadii[3] = topR;
		outRadii[4] = bottomR;
		outRadii[5] = bottomR;
		outRadii[6] = bottomR;
		outRadii[7] = bottomR;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (getBackground() == null && cardColor != 0) {
			int firstVisible = -1;
			int lastVisible = -1;
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				if (getChildAt(i).getVisibility() == View.VISIBLE) {
					if (firstVisible == -1) {
						firstVisible = i;
					}
					lastVisible = i;
				}
			}

			if (firstVisible != -1) {
				if (inverted) {
					View first = getChildAt(firstVisible);
					View last = getChildAt(lastVisible);
					rect.set(first.getLeft(), first.getTop(), first.getRight(), last.getBottom());
					if (cornerRadius > 0) {
						childPath.reset();
						childPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
						canvas.drawPath(childPath, cardPaint);
						if (strokePaint != null) {
							canvas.drawPath(childPath, strokePaint);
						}
					} else {
						canvas.drawRect(rect, cardPaint);
						if (strokePaint != null) {
							canvas.drawRect(rect, strokePaint);
						}
					}
				} else {
					for (int i = 0; i < childCount; i++) {
						View child = getChildAt(i);
						if (child.getVisibility() == View.VISIBLE) {
							rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
							if (cornerRadius > 0) {
								childPath.reset();
								getChildRadii(i, firstVisible, lastVisible, cornerRadius, radii);
								childPath.addRoundRect(rect, radii, Path.Direction.CW);
								canvas.drawPath(childPath, cardPaint);
								if (strokePaint != null) {
									canvas.drawPath(childPath, strokePaint);
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
				int firstVisible = -1;
				int lastVisible = -1;
				int childCount = getChildCount();
				for (int i = 0; i < childCount; i++) {
					if (getChildAt(i).getVisibility() == View.VISIBLE) {
						if (firstVisible == -1) {
							firstVisible = i;
						}
						lastVisible = i;
					}
				}
				if (firstVisible != -1) {
					if (inverted) {
						View first = getChildAt(firstVisible);
						View last = getChildAt(lastVisible);
						rect.set(first.getLeft(), first.getTop(), first.getRight(), last.getBottom());
						clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW);
					} else {
						for (int i = 0; i < childCount; i++) {
							View child = getChildAt(i);
							if (child.getVisibility() == View.VISIBLE) {
								rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
								getChildRadii(i, firstVisible, lastVisible, cornerRadius, radii);
								clipPath.addRoundRect(rect, radii, Path.Direction.CW);
							}
						}
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
