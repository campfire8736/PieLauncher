package de.markusfisch.android.pielauncher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class MaxHeightScrollView extends ScrollView {
	private int maxHeight;

	public MaxHeightScrollView(Context context) {
		super(context);
		init(context);
	}

	public MaxHeightScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MaxHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		maxHeight = Math.round(context.getResources().getDisplayMetrics().heightPixels * 0.65f);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int mode = MeasureSpec.getMode(heightMeasureSpec);
		int size = MeasureSpec.getSize(heightMeasureSpec);
		if (maxHeight > 0) {
			if (mode == MeasureSpec.UNSPECIFIED || size > maxHeight) {
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
