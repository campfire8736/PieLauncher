package de.markusfisch.android.pielauncher.widget.renderer;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.List;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.DrawerSectionRegistry;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.widget.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.state.Fader;

public class ListRenderer {

    private final Context context;
    private final float dp;
    private final TextPaint paintText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint paintSectionTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintList = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final ColorMatrixColorFilter dimFilter;
    private final Bitmap iconLaunchFirst;
    private final int iconLaunchFirstHalf;
    private final Rect drawRect = new Rect();

    private int listPadding;
    private int searchInputHeight;
    private int iconSize;
    private int spaceBetween;
    private int iconTextPadding;
    private float textHeight;
    private float textOffset;
    private int listFadeHeight;
    private int sectionHeaderHeight;
    private int sectionPaddingTop;
    private int sectionPaddingBottom;
    private float sectionTitleOffset;

    public ListRenderer(Context context) {
        this.context = context;
        Resources res = context.getResources();
        this.dp = res.getDisplayMetrics().density;

        paintText.setColor(PreferenceSliderView.getColorFromAttribute(
                context, android.R.attr.textColorPrimary, res.getColor(R.color.text_color)));
        paintText.setTextAlign(Paint.Align.CENTER);

        paintSectionTitle.setColor(PreferenceSliderView.getColorFromAttribute(
                context, android.R.attr.textColorSecondary, res.getColor(R.color.text_color_preference)));
        paintSectionTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintSectionTitle.setTextAlign(Paint.Align.LEFT);

        ColorMatrix colorMatrix = new ColorMatrix();
        float dimAmount = .5f;
        colorMatrix.setScale(dimAmount, dimAmount, dimAmount, 1);
        dimFilter = new ColorMatrixColorFilter(colorMatrix);

        iconLaunchFirst = Converter.getBitmapFromDrawable(context, R.drawable.ic_launch_first);
        iconLaunchFirstHalf = iconLaunchFirst.getWidth() >> 1;
    }

    public TextPaint getPaintText() {
        return paintText;
    }

    public float getTextOffset() {
        return textOffset;
    }

    public float getTextHeight() {
        return textHeight;
    }

    public int getSectionHeaderHeight() {
        return sectionHeaderHeight;
    }

    public void layout(int listPadding, int searchInputHeight, int iconSize, int spaceBetween, int iconTextPadding, int listFadeHeight, float sp) {
        this.listPadding = listPadding;
        this.searchInputHeight = searchInputHeight;
        this.iconSize = iconSize;
        this.spaceBetween = spaceBetween;
        this.iconTextPadding = iconTextPadding;
        this.listFadeHeight = listFadeHeight;
        paintText.setTextSize(14f * sp);
        this.textHeight = paintText.descent() - paintText.ascent();
        this.textOffset = (this.textHeight / 2) - paintText.descent();

        paintSectionTitle.setTextSize(13f * sp);
        float secTextHeight = paintSectionTitle.descent() - paintSectionTitle.ascent();
        this.sectionTitleOffset = -paintSectionTitle.ascent();
        this.sectionPaddingTop = Math.round(16f * dp);
        this.sectionPaddingBottom = Math.round(8f * dp);
        this.sectionHeaderHeight = Math.round(secTextHeight) + sectionPaddingTop + sectionPaddingBottom;
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeList,
            List<Apps.AppIcon> appList,
            int scrollY,
            int viewWidth,
            int viewHeight,
            Apps.AppIcon touchedIcon,
            Apps.AppIcon highlightedIcon,
            boolean isDraggingDownList,
            int selectedApp,
            long highlightedFrom,
            float animationDuration,
            boolean showAppNames,
            int listAppearanceAnimation,
            float dragProgress,
            android.graphics.RenderNode listRenderNode) {
        return draw(canvas, fadeList, appList, scrollY, viewWidth, viewHeight,
                touchedIcon, highlightedIcon, isDraggingDownList, selectedApp,
                highlightedFrom, animationDuration, showAppNames,
                listAppearanceAnimation, dragProgress, listRenderNode, 0);
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeList,
            List<Apps.AppIcon> appList,
            int scrollY,
            int viewWidth,
            int viewHeight,
            Apps.AppIcon touchedIcon,
            Apps.AppIcon highlightedIcon,
            boolean isDraggingDownList,
            int selectedApp,
            long highlightedFrom,
            float animationDuration,
            boolean showAppNames,
            int listAppearanceAnimation,
            float dragProgress,
            android.graphics.RenderNode listRenderNode,
            int shortcutsHeight) {
        return draw(canvas, fadeList, DrawerSectionRegistry.group(appList), appList,
                scrollY, viewWidth, viewHeight, touchedIcon, highlightedIcon,
                isDraggingDownList, selectedApp, highlightedFrom, animationDuration,
                showAppNames, listAppearanceAnimation, dragProgress, listRenderNode,
                shortcutsHeight);
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeList,
            List<DrawerSectionRegistry.SectionGroup> sectionGroups,
            List<Apps.AppIcon> appList,
            int scrollY,
            int viewWidth,
            int viewHeight,
            Apps.AppIcon touchedIcon,
            Apps.AppIcon highlightedIcon,
            boolean isDraggingDownList,
            int selectedApp,
            long highlightedFrom,
            float animationDuration,
            boolean showAppNames,
            int listAppearanceAnimation,
            float dragProgress,
            android.graphics.RenderNode listRenderNode,
            int shortcutsHeight) {

        long now = SystemClock.uptimeMillis();
        float f = Math.min(fadeList.get(now, animationDuration), dragProgress);
        if (f <= 0) {
            return false;
        }

        int innerWidth = viewWidth - listPadding * 2;
        int columns = Math.min(5, innerWidth / (iconSize + spaceBetween));
        int iconAndTextHeight = iconSize + (showAppNames ? iconTextPadding + Math.round(textHeight) : 0);
        int cellWidth = innerWidth / columns;
        int cellHeight = iconAndTextHeight + iconTextPadding * 2;
        int wrapX = listPadding + cellWidth * columns;

        int maxTextWidth = cellWidth - iconTextPadding;
        int hpad = (cellWidth - iconSize) / 2;
        int vpad = (cellHeight - iconAndTextHeight) / 2;
        int labelX = cellWidth >> 1;
        int labelY = cellHeight - vpad - Math.round(textOffset);
        int viewTop = scrollY - cellHeight;
        int viewBottom = scrollY + viewHeight;
        int x = listPadding;
        int y = searchInputHeight + listPadding;

        if (listAppearanceAnimation == 1 || isDraggingDownList) {
            y += Math.round((1f - f) * listFadeHeight);
        }

        paintList.setAlpha(Math.round(f * 255f));
        paintText.setAlpha(Math.round(f * 255f));
        paintSectionTitle.setAlpha(Math.round(f * 255f));

        int magSize = Math.round(Math.max(cellWidth, cellHeight) * .2f);
        boolean invalidate = f < 1f;
        if (highlightedFrom > 0) {
            if (animationDuration > 0) {
                float t = Math.min(1f, (now - highlightedFrom) / animationDuration);
                if (t < 1f) {
                    invalidate = true;
                }
                magSize = Math.round(magSize * t);
            } else {
                magSize = 0;
            }
        }

        if ((sectionGroups == null || sectionGroups.isEmpty()) && appList != null && !appList.isEmpty()) {
            sectionGroups = DrawerSectionRegistry.group(appList);
        }

        if (sectionGroups != null) {
            for (DrawerSectionRegistry.SectionGroup group : sectionGroups) {
                if (group.items.isEmpty()) {
                    continue;
                }
                x = listPadding;
                CharSequence title = group.section.getTitle(context);
                if (title != null && y + sectionHeaderHeight > viewTop && y < viewBottom) {
                    canvas.drawText(title, 0, title.length(),
                            x + hpad, y + sectionPaddingTop + sectionTitleOffset,
                            paintSectionTitle);
                }
                y += sectionHeaderHeight;

                for (Apps.AppIcon appIcon : group.items) {
                    if (y > viewTop && y < viewBottom) {
                        appIcon.hitRect.set(x, y, x + cellWidth, y + cellHeight);
                        int ix = x + hpad;
                        int iy = y + vpad;
                        int mag = appIcon == highlightedIcon ? magSize : 0;
                        drawRect.set(ix - mag, iy - mag, ix + iconSize + mag, iy + iconSize + mag);
                        if (mag == 0 && appIcon == touchedIcon) {
                            paintList.setColorFilter(dimFilter);
                        }
                        canvas.drawBitmap(appIcon.bitmap, null, drawRect, paintList);
                        paintList.setColorFilter(null);
                        if (showAppNames) {
                            CharSequence label = TextUtils.ellipsize(appIcon.label, paintText, maxTextWidth, TextUtils.TruncateAt.END);
                            canvas.drawText(label, 0, label.length(), x + labelX, y + labelY, paintText);
                        }
                    }
                    x += cellWidth;
                    if (x >= wrapX) {
                        x = listPadding;
                        y += cellHeight;
                    }
                }
                if (x > listPadding) {
                    x = listPadding;
                    y += cellHeight;
                }
            }
        }

        if (selectedApp > -1 && appList != null && selectedApp < appList.size()) {
            Apps.AppIcon selectedIcon = appList.get(selectedApp);
            if (selectedIcon != null && !selectedIcon.hitRect.isEmpty()) {
                canvas.drawBitmap(iconLaunchFirst,
                        selectedIcon.hitRect.left + labelX - iconLaunchFirstHalf,
                        selectedIcon.hitRect.top - listPadding,
                        paintList);
            }
        }

        int maxHeight = y + listPadding;
        maxHeight += shortcutsHeight;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && listRenderNode != null) {
            listRenderNode.setPosition(0, 0, viewWidth, maxHeight);
        }

        return invalidate;
    }
}

