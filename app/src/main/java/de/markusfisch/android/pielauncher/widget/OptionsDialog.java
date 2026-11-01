package de.markusfisch.android.pielauncher.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.graphics.BackgroundBlur;

public class OptionsDialog {
	public static void show(Context context,
			int titleId,
			CharSequence[] items,
			DialogInterface.OnClickListener onClickListener) {
		show(context, titleId, items, -1, onClickListener);
	}

	public static void show(Context context,
			int titleId,
			CharSequence[] items,
			int selectedIndex,
			DialogInterface.OnClickListener onClickListener) {
		if (context == null || items == null) {
			return;
		}

		Dialog dialog = new Dialog(context);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		View view = LayoutInflater.from(context).inflate(
				R.layout.dialog_options, null);
		dialog.setContentView(view);

		TextView titleView = view.findViewById(R.id.dialog_title);
		View titleDivider = view.findViewById(R.id.dialog_title_divider);
		if (titleId != 0) {
			titleView.setText(titleId);
			titleView.setVisibility(View.VISIBLE);
			titleDivider.setVisibility(View.VISIBLE);
		} else {
			titleView.setVisibility(View.GONE);
			titleDivider.setVisibility(View.GONE);
		}

		LinearLayout container = view.findViewById(R.id.options_container);
		LayoutInflater inflater = LayoutInflater.from(context);

		for (int i = 0; i < items.length; i++) {
			final int position = i;
			View itemView = inflater.inflate(
					R.layout.dialog_option_item, container, false);

			TextView text = itemView.findViewById(R.id.option_text);
			ImageView check = itemView.findViewById(R.id.option_check);

			text.setText(items[i]);

			if (position == selectedIndex) {
				check.setVisibility(View.VISIBLE);
				text.setTextColor(context.getResources().getColor(R.color.accent));
			} else {
				check.setVisibility(View.GONE);
			}

			itemView.setOnClickListener(v -> {
				if (onClickListener != null) {
					onClickListener.onClick(dialog, position);
				}
				dialog.dismiss();
			});

			container.addView(itemView);
		}

		Window window = dialog.getWindow();
		if (window != null) {
			window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			window.setDimAmount(0.7f);
			BackgroundBlur.setBlurRadius(window, 20);

			int maxWidth = context.getResources().getDimensionPixelSize(
					R.dimen.dialog_max_width);
			int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
			int dialogWidth = Math.min(maxWidth, Math.round(screenWidth * 0.88f));

			window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
		}

		dialog.setCanceledOnTouchOutside(true);
		dialog.setCancelable(true);
		dialog.show();
	}
}
