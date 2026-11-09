package de.markusfisch.android.pielauncher.widget.renderer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.Converter;
import de.markusfisch.android.pielauncher.graphics.PieMenu;
import de.markusfisch.android.pielauncher.widget.PreferenceSliderView;
import de.markusfisch.android.pielauncher.widget.state.Fader;
import de.markusfisch.android.pielauncher.widget.state.EditorAction;

public class EditorRenderer {

    public final Rect iconStartRect = new Rect();
    public final Rect iconCenterRect = new Rect();
    public final Rect iconEndRect = new Rect();
    public final Rect iconChangeTwistRect = new Rect();
    public final Rect iconChangeIconScaleRect = new Rect();
    public final Rect iconChangeRadiusRect = new Rect();
    public final Rect topBarRect = new Rect();
    public final Rect bottomBarRect = new Rect();
    private final RectF topBarRectF = new RectF();
    private final RectF bottomBarRectF = new RectF();
    private final RectF tipBarRectF = new RectF();

    private final Paint paintBar;
    private final Paint paintDropZone;
    private final Paint paintPressed;
    private final Paint paintAction;
    private final Paint paintText;
    private final Paint paintSlotHighlight;
    private final int barColor;
    private final float dp;
    private final int alphaDropZone;
    private final int alphaPressed;
    private final int alphaText;

    private final String dragToOrderTip;
    private final String removeIconTip;
    private final String editAppTip;
    private final String hideAppTip;
    private final String removeAppTip;

    private final Point touch = new Point();
    private boolean hasTouch = false;
    private int controlsPadding;
    private float textOffset;

    private final Bitmap iconRemove;
    private final Bitmap iconEdit;
    private final Bitmap iconHide;
    private final Bitmap iconDetails;
    public Bitmap iconChangeTwist;
    public Bitmap iconChangeIconScale;
    public Bitmap iconChangeRadius;
    public Bitmap iconAdd;
    public Bitmap iconSplitPie;
    public Bitmap iconPreferences;
    public Bitmap iconDone;

    private float actionSize;
    private float actionSizeSq;

    public EditorRenderer(Context context) {
        Resources res = context.getResources();
        dp = res.getDisplayMetrics().density;

        barColor = PreferenceSliderView.getColorFromAttribute(
                context, R.attr.colorSearchBar, res.getColor(R.color.bg_search_bar));
        paintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBar.setColor(barColor);
        paintBar.setStyle(Paint.Style.FILL);

        paintDropZone = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDropZone.setColor(PreferenceSliderView.getColorFromAttribute(
                context, R.attr.colorDropZone, res.getColor(R.color.bg_drop_zone)));
        paintDropZone.setStyle(Paint.Style.FILL);
        alphaDropZone = paintDropZone.getAlpha();

        paintPressed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPressed.setColor(PreferenceSliderView.getColorFromAttribute(
                context, R.attr.colorSeparator, res.getColor(R.color.bg_action_pressed)));
        paintPressed.setStyle(Paint.Style.FILL);
        alphaPressed = paintPressed.getAlpha();

        paintAction = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAction.setFilterBitmap(true);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(PreferenceSliderView.getColorFromAttribute(
                context, android.R.attr.textColorPrimary, res.getColor(R.color.text_color)));
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(14f * res.getDisplayMetrics().scaledDensity);
        alphaText = paintText.getAlpha();

        paintSlotHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSlotHighlight.setColor(0xffffffff);
        paintSlotHighlight.setStyle(Paint.Style.STROKE);
        paintSlotHighlight.setStrokeWidth(2.5f * res.getDisplayMetrics().density);

        dragToOrderTip = context.getString(R.string.tip_drag_to_order);
        removeIconTip = context.getString(R.string.tip_remove_icon);
        editAppTip = context.getString(R.string.change_icon);
        hideAppTip = context.getString(R.string.hide_app);
        removeAppTip = context.getString(R.string.tip_remove_app);

        iconAdd = Converter.getBitmapFromDrawable(context, R.drawable.ic_add);
        iconEdit = Converter.getBitmapFromDrawable(context, R.drawable.ic_edit);
        iconHide = Converter.getBitmapFromDrawable(context, R.drawable.ic_hide);
        iconRemove = Converter.getBitmapFromDrawable(context, R.drawable.ic_remove);
        iconDetails = Converter.getBitmapFromDrawable(context, R.drawable.ic_details);
        iconDone = Converter.getBitmapFromDrawable(context, R.drawable.ic_done);
        iconPreferences = Converter.getBitmapFromDrawable(context, R.drawable.ic_preferences);
    }


    public boolean draw(
            Canvas canvas,
            Fader fadeEdit,
            CanvasPieMenu pieMenu,
            Apps.AppIcon grabbedIcon,
            long grabbedIconAt,
            Rect highlightedAction,
            float animationDuration,
            boolean hasSecondaryMenu,
            boolean hasPacks) {
        return draw(
                canvas,
                fadeEdit,
                pieMenu,
                grabbedIcon,
                grabbedIconAt,
                highlightedAction,
                animationDuration,
                hasSecondaryMenu,
                hasPacks,
                -1,
                -1,
                false);
    }

    public boolean draw(
            Canvas canvas,
            Fader fadeEdit,
            CanvasPieMenu pieMenu,
            Apps.AppIcon grabbedIcon,
            long grabbedIconAt,
            Rect highlightedAction,
            float animationDuration,
            boolean hasSecondaryMenu,
            boolean hasPacks,
            int hoveredSlot,
            int sourceSlot,
            boolean isDisplacedOccupied) {

        long now = SystemClock.uptimeMillis();
        float f = fadeEdit.get(now, animationDuration);
        if (f <= 0) {
            return false;
        }

        int alpha = Math.round(f * 255f);
        CanvasPieMenu.paint.setAlpha(alpha);
        paintDropZone.setAlpha(Math.round(f * alphaDropZone));
        paintPressed.setAlpha(Math.round(f * alphaPressed));
        paintAction.setAlpha(alpha);
        paintText.setAlpha(Math.round(f * alphaText));
        paintSlotHighlight.setAlpha(alpha);

        boolean hasIcon = grabbedIcon != null;
        boolean invalidate = f < 1f;

        int barAlpha = Math.round(f * ((barColor >>> 24) & 0xff));
        paintBar.setColor((barAlpha << 24) | (barColor & 0xffffff));

        boolean portrait = canvas.getHeight() > canvas.getWidth();
        float cornerRadiusTop = (portrait ? topBarRect.height() : topBarRect.width()) * 0.5f;
        float cornerRadiusBottom = (portrait ? bottomBarRect.height() : bottomBarRect.width()) * 0.5f;

        if (!hasIcon) {
            topBarRectF.set(topBarRect);
            canvas.drawRoundRect(topBarRectF, cornerRadiusTop, cornerRadiusTop, paintBar);
            bottomBarRectF.set(bottomBarRect);
            canvas.drawRoundRect(bottomBarRectF, cornerRadiusBottom, cornerRadiusBottom, paintBar);
        } else {
            String tip = getTip(hasIcon, hasPacks);
            if (tip != null && portrait) {
                float tipWidth = paintText.measureText(tip);
                float cx = canvas.getWidth() * 0.5f;
                float cy = controlsPadding;
                float tipH = Math.max(topBarRect.height(), 40f * dp);
                tipBarRectF.set(
                        cx - tipWidth * 0.5f - 16f * dp,
                        cy - tipH * 0.5f,
                        cx + tipWidth * 0.5f + 16f * dp,
                        cy + tipH * 0.5f);
                canvas.drawRoundRect(tipBarRectF, tipH * 0.5f, tipH * 0.5f, paintBar);
            }
        }

        drawPlaceholders(canvas, pieMenu, hasIcon, hoveredSlot, sourceSlot, isDisplacedOccupied);
        drawSlotHighlights(canvas, pieMenu, hasIcon, hoveredSlot, sourceSlot, grabbedIcon);

        // Only draw tips in portrait orientation.
        // There's probably not enough space in landscape.
        if (canvas.getWidth() < canvas.getHeight()) {
            drawTip(canvas, getTip(hasIcon, hasPacks));
        }

        if (hasIcon) {
            float r = actionSize;
            if (animationDuration > 0) {
                float t = Math.min(1f, (now - grabbedIconAt) / animationDuration);
                if (t < 1f) {
                    invalidate = true;
                }
                r *= t;
            }
            Rect activeHighlight = highlightedAction;
            if (hasTouch) {
                if (contains(iconStartRect, touch)) {
                    activeHighlight = iconStartRect;
                } else if (contains(iconCenterRect, touch)) {
                    activeHighlight = iconCenterRect;
                } else if (contains(iconEndRect, touch)) {
                    activeHighlight = iconEndRect;
                } else {
                    activeHighlight = null;
                }
            }
            drawAction(canvas, iconRemove, iconStartRect, r, activeHighlight);
            drawAction(canvas, hasPacks ? iconEdit : iconHide, iconCenterRect, r, activeHighlight);
            drawAction(canvas, iconDetails, iconEndRect, r, activeHighlight);
        } else {
            drawAction(canvas, iconChangeTwist, iconChangeTwistRect, 0f, highlightedAction);
            drawAction(canvas, iconChangeIconScale, iconChangeIconScaleRect, 0f, highlightedAction);
            drawAction(canvas, iconChangeRadius, iconChangeRadiusRect, 0f, highlightedAction);
            drawAction(canvas, iconAdd, iconStartRect, 0f, highlightedAction);
            drawAction(canvas, hasSecondaryMenu ? iconSplitPie : iconPreferences, iconCenterRect, 0f, highlightedAction);
            drawAction(canvas, iconDone, iconEndRect, 0f, highlightedAction);
        }

        if (hasIcon && !pieMenu.icons.contains(grabbedIcon)) {
            double iconDrawSize = grabbedIcon.size;
            if (iconDrawSize <= 0) {
                for (int i = 0; i < pieMenu.icons.size(); ++i) {
                    PieMenu.Icon ic = (PieMenu.Icon) pieMenu.icons.get(i);
                    if (ic != null && ic.size > 0) {
                        iconDrawSize = ic.size;
                        break;
                    }
                }
            }
            if (iconDrawSize <= 0) {
                iconDrawSize = actionSize;
            }
            grabbedIcon.draw(canvas, iconDrawSize, grabbedIcon.x, grabbedIcon.y);
        }

        return invalidate;
    }

    private String getTip(boolean hasIcon, boolean hasPacks) {
        if (hasIcon) {
            if (hasTouch) {
                if (contains(iconStartRect, touch)) {
                    return removeIconTip;
                } else if (contains(iconCenterRect, touch)) {
                    return hasPacks ? editAppTip : hideAppTip;
                } else if (contains(iconEndRect, touch)) {
                    return removeAppTip;
                }
            }
            return dragToOrderTip;
        }
        return null;
    }

    private void drawTip(Canvas canvas, String tip) {
        if (tip != null) {
            canvas.drawText(tip, canvas.getWidth() >> 1,
                    controlsPadding + textOffset, paintText);
        }
    }

    private void drawAction(Canvas canvas, Bitmap icon, Rect rect, float radius, Rect highlightedAction) {
        boolean pressed = rect == highlightedAction;
        Paint circlePaint = null;
        if (radius > 0) {
            circlePaint = pressed ? paintPressed : paintDropZone;
        } else if (pressed) {
            radius = Math.min(actionSize, (Math.min(rect.width(), rect.height()) * 0.5f) + 8f * dp);
            circlePaint = paintPressed;
        }
        if (circlePaint != null) {
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius, circlePaint);
        }
        canvas.drawBitmap(icon, null, rect, paintAction);
    }


    public void setTouch(Point touch) {
        if (touch != null) {
            this.touch.set(touch.x, touch.y);
            this.hasTouch = true;
        } else {
            this.hasTouch = false;
        }
    }

    public void setTextDimensions(int controlsPadding, float textOffset, float sp) {
        this.controlsPadding = controlsPadding;
        this.textOffset = textOffset;
        paintText.setTextSize(14f * sp);
    }

    public void setActionSize(int actionSize) {
        this.actionSize = actionSize;
        this.actionSizeSq = actionSize * actionSize;
    }

    public float getActionSize() {
        return actionSize;
    }

    public void layout(
            int viewWidth,
            int viewHeight,
            int controlsPadding,
            float textOffset,
            float sp,
            int spaceBetween,
            int iconSize) {
        boolean portrait = viewHeight > viewWidth;
        int max = distributeButtons(
                iconAdd,
                iconPreferences,
                iconDone,
                iconStartRect,
                iconCenterRect,
                iconEndRect,
                -controlsPadding,
                viewWidth,
                viewHeight,
                portrait);

        distributeButtons(
                iconChangeTwist,
                iconChangeIconScale,
                iconChangeRadius,
                iconChangeTwistRect,
                iconChangeIconScaleRect,
                iconChangeRadiusRect,
                controlsPadding,
                viewWidth,
                viewHeight,
                portrait);

        setActionSize(Math.min(iconSize, max / 2 - spaceBetween));
        setTextDimensions(controlsPadding, textOffset, sp);

        int hPad = Math.round(16f * dp);
        int vPad = Math.round(12f * dp);

        if (portrait) {
            int topMinX = Math.min(iconChangeTwistRect.left, Math.min(iconChangeIconScaleRect.left, iconChangeRadiusRect.left));
            int topMaxX = Math.max(iconChangeTwistRect.right, Math.max(iconChangeIconScaleRect.right, iconChangeRadiusRect.right));
            int topMinY = Math.min(iconChangeTwistRect.top, Math.min(iconChangeIconScaleRect.top, iconChangeRadiusRect.top));
            int topMaxY = Math.max(iconChangeTwistRect.bottom, Math.max(iconChangeIconScaleRect.bottom, iconChangeRadiusRect.bottom));

            int botMinX = Math.min(iconStartRect.left, Math.min(iconCenterRect.left, iconEndRect.left));
            int botMaxX = Math.max(iconStartRect.right, Math.max(iconCenterRect.right, iconEndRect.right));
            int botMinY = Math.min(iconStartRect.top, Math.min(iconCenterRect.top, iconEndRect.top));
            int botMaxY = Math.max(iconStartRect.bottom, Math.max(iconCenterRect.bottom, iconEndRect.bottom));

            topBarRect.set(topMinX - hPad, topMinY - vPad, topMaxX + hPad, topMaxY + vPad);
            bottomBarRect.set(botMinX - hPad, botMinY - vPad, botMaxX + hPad, botMaxY + vPad);
        } else {
            int topMinX = Math.min(iconChangeTwistRect.left, Math.min(iconChangeIconScaleRect.left, iconChangeRadiusRect.left));
            int topMaxX = Math.max(iconChangeTwistRect.right, Math.max(iconChangeIconScaleRect.right, iconChangeRadiusRect.right));
            int topMinY = Math.min(iconChangeTwistRect.top, Math.min(iconChangeIconScaleRect.top, iconChangeRadiusRect.top));
            int topMaxY = Math.max(iconChangeTwistRect.bottom, Math.max(iconChangeIconScaleRect.bottom, iconChangeRadiusRect.bottom));

            int botMinX = Math.min(iconStartRect.left, Math.min(iconCenterRect.left, iconEndRect.left));
            int botMaxX = Math.max(iconStartRect.right, Math.max(iconCenterRect.right, iconEndRect.right));
            int botMinY = Math.min(iconStartRect.top, Math.min(iconCenterRect.top, iconEndRect.top));
            int botMaxY = Math.max(iconStartRect.bottom, Math.max(iconCenterRect.bottom, iconEndRect.bottom));

            topBarRect.set(topMinX - vPad, topMinY - hPad, topMaxX + vPad, topMaxY + hPad);
            bottomBarRect.set(botMinX - vPad, botMinY - hPad, botMaxX + vPad, botMaxY + hPad);
        }
    }

    private int distributeButtons(
            Bitmap icon1, Bitmap icon2, Bitmap icon3,
            Rect rect1, Rect rect2, Rect rect3,
            int base,
            int viewWidth,
            int viewHeight,
            boolean portrait) {
        int w1 = icon1 != null ? icon1.getWidth() : 0;
        int h1 = icon1 != null ? icon1.getHeight() : 0;
        int w2 = icon2 != null ? icon2.getWidth() : 0;
        int h2 = icon2 != null ? icon2.getHeight() : 0;
        int w3 = icon3 != null ? icon3.getWidth() : 0;
        int h3 = icon3 != null ? icon3.getHeight() : 0;

        rect1.set(0, 0, w1, h1);
        rect2.set(0, 0, w2, h2);
        rect3.set(0, 0, w3, h3);

        int totalWidth = w1 + w2 + w3;
        int totalHeight = h1 + h2 + h3;
        int largestWidth = Math.max(w1, Math.max(w2, w3));
        int largestHeight = Math.max(h1, Math.max(h2, h3));

        int spaces = 4;
        int max;
        if (portrait) {
            int step = Math.round((float) (viewWidth - totalWidth) / spaces);
            max = largestWidth + step;
            int x = step;
            int y = (base > 0 ? base : viewHeight + base) - largestHeight / 2;

            rect1.offset(x, y);
            x += step + rect1.width();

            rect2.offset(x, y);
            x += step + rect2.width();

            rect3.offset(x, y);
        } else {
            int step = Math.round((float) (viewHeight - totalHeight) / spaces);
            max = largestHeight + step;
            int x = (base > 0 ? base : viewWidth + base) - largestWidth / 2;
            int y = step;

            rect1.offset(x, y);
            y += step + rect1.height();

            rect2.offset(x, y);
            y += step + rect2.height();

            rect3.offset(x, y);
        }
        return max;
    }

    public EditorAction getActionAt(Point touch, boolean hasGrabbedIcon) {
        if (!hasGrabbedIcon) {
            if (contains(iconChangeTwistRect, touch)) {
                return EditorAction.CHANGE_TWIST;
            } else if (contains(iconChangeIconScaleRect, touch)) {
                return EditorAction.CHANGE_ICON_SCALE;
            } else if (contains(iconChangeRadiusRect, touch)) {
                return EditorAction.CHANGE_RADIUS;
            }
        }

        if (contains(iconStartRect, touch)) {
            return EditorAction.BACK_OR_REMOVE_ICON;
        } else if (contains(iconCenterRect, touch)) {
            return EditorAction.EDIT_SETTINGS_OR_UNDO;
        } else if (contains(iconEndRect, touch)) {
            return EditorAction.END_EDIT_MODE;
        }

        return EditorAction.NONE;
    }

    private void drawSlotHighlights(
            Canvas canvas,
            CanvasPieMenu pieMenu,
            boolean hasGrabbedIcon,
            int hoveredSlot,
            int sourceSlot,
            Apps.AppIcon grabbedIcon) {
        if (!hasGrabbedIcon || pieMenu == null || !pieMenu.isRigid()) {
            return;
        }
        int numberOfSlots = pieMenu.icons.size();
        if (numberOfSlots < 1) {
            return;
        }
        int cx = pieMenu.getCenterX();
        int cy = pieMenu.getCenterY();
        if (cx <= 0 || cy <= 0) {
            cx = canvas.getWidth() >> 1;
            cy = canvas.getHeight() >> 1;
        }
        double rad = pieMenu.getRadius();
        double twist = pieMenu.getTwist();
        double step = PieMenu.TAU / numberOfSlots;

        double iconSize = grabbedIcon != null && grabbedIcon.size > 0 ? grabbedIcon.size : 0;
        if (iconSize <= 0) {
            for (int i = 0; i < numberOfSlots; ++i) {
                PieMenu.Icon ic = (PieMenu.Icon) pieMenu.icons.get(i);
                if (ic != null && ic.size > 0) {
                    iconSize = ic.size;
                    break;
                }
            }
        }
        if (iconSize <= 0) {
            iconSize = actionSize;
        }
        float r = (float) (iconSize * 0.5);

        if (sourceSlot >= 0 && sourceSlot < numberOfSlots) {
            double a = twist + sourceSlot * step;
            int sx = cx + (int) Math.round(rad * Math.cos(a));
            int sy = cy + (int) Math.round(rad * Math.sin(a));
            canvas.drawCircle(sx, sy, r, paintSlotHighlight);
        }
    }

    private void drawPlaceholders(
            Canvas canvas,
            CanvasPieMenu pieMenu,
            boolean hasGrabbedIcon,
            int hoveredSlot,
            int sourceSlot,
            boolean isDisplacedOccupied) {
        if (pieMenu == null || !pieMenu.isRigid()) {
            return;
        }
        int numberOfSlots = pieMenu.icons.size();
        int targetAlpha = paintDropZone.getAlpha();
        double baseSize = actionSize;
        for (int j = 0; j < numberOfSlots; ++j) {
            PieMenu.Icon ic = (PieMenu.Icon) pieMenu.icons.get(j);
            if (ic != null && ic.size > 0) {
                baseSize = ic.size;
                break;
            }
        }
        float baseR = (float) (baseSize * 0.5);

        for (int i = 0; i < numberOfSlots; ++i) {
            PieMenu.Icon icon = (PieMenu.Icon) pieMenu.icons.get(i);
            if (icon != null && icon.isGhost) {
                if (hasGrabbedIcon) {
                    if (i == sourceSlot) {
                        continue;
                    }
                }
                float r;
                int x, y;
                if (icon instanceof CanvasPieMenu.CanvasIcon) {
                    CanvasPieMenu.CanvasIcon ci = (CanvasPieMenu.CanvasIcon) icon;
                    r = (float) (ci.getSmoothedSize() * 0.5);
                    x = ci.getSmoothedX();
                    y = ci.getSmoothedY();
                } else {
                    r = (float) (icon.size * 0.5);
                    x = icon.x;
                    y = icon.y;
                }
                if (r > 1) {
                    float ratio = baseR > 0 ? Math.min(1.0f, Math.max(0.0f, r / baseR)) : 1.0f;
                    paintDropZone.setAlpha(Math.round(targetAlpha * ratio));
                    canvas.drawCircle(x, y, r, paintDropZone);
                    paintDropZone.setAlpha(targetAlpha);
                }
            }
        }
    }

    private boolean contains(Rect rect, Point point) {
        return distSq(rect.centerX(), rect.centerY(), point.x, point.y) <
                actionSizeSq;
    }

    private static float distSq(int ax, int ay, int bx, int by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }
}
