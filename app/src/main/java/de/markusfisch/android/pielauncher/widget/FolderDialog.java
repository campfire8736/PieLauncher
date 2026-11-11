package de.markusfisch.android.pielauncher.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import de.markusfisch.android.pielauncher.R;

public class FolderDialog {
	public interface OnFolderConfiguredListener {
		void onFolderConfigured(String name, int color);
	}

	public static final int[] FOLDER_COLORS = new int[]{
			0xff2196f3, // Blue
			0xff00bcd4, // Cyan
			0xff009688, // Teal
			0xff4caf50, // Green
			0xffffeb3b, // Yellow
			0xffff9800, // Orange
			0xfff44336, // Red
			0xffe91e63, // Pink
			0xff9c27b0, // Purple
			0xff607d8b, // Blue Grey
	};

	public static void show(
			Context context,
			int titleResId,
			String initialName,
			int initialColor,
			int positiveButtonResId,
			OnFolderConfiguredListener listener) {
		AlertDialog.Builder builder = Dialog.newDialog(context);
		builder.setTitle(titleResId);

		View view = LayoutInflater.from(context).inflate(R.layout.dialog_folder, null);
		EditText nameInput = view.findViewById(R.id.folder_name);
		LinearLayout paletteContainer = view.findViewById(R.id.color_palette);

		if (initialName != null) {
			nameInput.setText(initialName);
			nameInput.setSelection(initialName.length());
		}

		final int[] selectedColor = new int[]{
				initialColor != 0 ? initialColor : FOLDER_COLORS[0]
		};

		setupPalette(context, paletteContainer, selectedColor);

		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(positiveButtonResId, (dialog, which) -> {
			String name = nameInput.getText().toString().trim();
			if (name.isEmpty()) {
				name = context.getString(R.string.folder);
			}
			if (listener != null) {
				listener.onFolderConfigured(name, selectedColor[0]);
			}
		});

		builder.show();
	}

	private static void setupPalette(
			Context context,
			LinearLayout container,
			int[] selectedColor) {
		int size = Math.round(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 38f,
				context.getResources().getDisplayMetrics()));
		int margin = Math.round(TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 5f,
				context.getResources().getDisplayMetrics()));

		List<ImageView> swatches = new ArrayList<>();
		int count = FOLDER_COLORS.length;
		int perRow = count / 2;

		LinearLayout row1 = createRow(context);
		LinearLayout row2 = createRow(context);
		container.addView(row1);
		container.addView(row2);

		for (int i = 0; i < count; i++) {
			int color = FOLDER_COLORS[i];
			ImageView swatch = new ImageView(context);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
			lp.setMargins(margin, margin, margin, margin);
			swatch.setLayoutParams(lp);

			GradientDrawable bg = new GradientDrawable();
			bg.setShape(GradientDrawable.OVAL);
			bg.setColor(color);
			swatch.setBackground(bg);
			swatch.setScaleType(ImageView.ScaleType.CENTER);

			swatches.add(swatch);
			if (i < perRow) {
				row1.addView(swatch);
			} else {
				row2.addView(swatch);
			}

			swatch.setOnClickListener(v -> {
				selectedColor[0] = color;
				updateSwatches(swatches, selectedColor[0]);
			});
		}

		updateSwatches(swatches, selectedColor[0]);
	}

	private static LinearLayout createRow(Context context) {
		LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
		return row;
	}

	private static void updateSwatches(List<ImageView> swatches, int currentColor) {
		for (int i = 0; i < swatches.size() && i < FOLDER_COLORS.length; i++) {
			ImageView swatch = swatches.get(i);
			int color = FOLDER_COLORS[i];
			if (color == currentColor) {
				swatch.setImageResource(R.drawable.ic_check_small);
				boolean isLight = isLightColor(color);
				swatch.setImageTintList(ColorStateList.valueOf(
						isLight ? 0xff000000 : 0xffffffff));
			} else {
				swatch.setImageDrawable(null);
			}
		}
	}

	private static boolean isLightColor(int color) {
		double darkness = 1 - (0.299 * Color.red(color)
				+ 0.587 * Color.green(color)
				+ 0.114 * Color.blue(color)) / 255;
		return darkness < 0.35;
	}
}
