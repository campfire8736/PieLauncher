package de.markusfisch.android.pielauncher.widget.state;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;
import de.markusfisch.android.pielauncher.graphics.PieMenu;

public class PieEditor {

    public interface Listener {
        void invalidate();

        void storeMenu();

        void releaseIcon();
    }

    private final Listener listener;
    private final CanvasPieMenu<Apps.AppIcon> pieMenu;

    private int lastCenterX;
    private int lastCenterY;

    private final List<Apps.AppIcon> ungrabbedIcons = new ArrayList<>();
    private Apps.AppIcon grabbedIcon;
    private long grabbedIconAt;
    private int lastInsertAt;
    private int grabbedSourceSlot = -1;
    private int lastHoveredSlot = -1;
    private Apps.AppIcon displacedHovered;
    private Apps.AppIcon displacedSource;
    private double twist;
    private Point touch;

    private final List<Apps.AppIcon> backup = new ArrayList<>();

    public PieEditor(Listener listener, CanvasPieMenu<Apps.AppIcon> pieMenu) {
        this.listener = listener;
        this.pieMenu = pieMenu;
    }

    public void beginEdit(List<Apps.AppIcon> currentIcons) {
        backup.clear();
        backup.addAll(currentIcons);
    }

    public void commit() {
        backup.clear();
    }

    public void rollback(List<Apps.AppIcon> currentIcons) {
        resetDisplacedApp();
        currentIcons.clear();
        currentIcons.addAll(backup);
        backup.clear();
        grabbedIcon = null;
        lastHoveredSlot = -1;
        displacedHovered = null;
        displacedSource = null;
    }

    private boolean neverDropped = false;

    public void setNeverDropped(boolean neverDropped) {
        this.neverDropped = neverDropped;
    }

    public boolean isNeverDropped() {
        return neverDropped;
    }

    public boolean isMenuChanged(List<? extends PieMenu.Icon> currentIcons) {
        if (pieMenu.isRigid()) {
            return !backup.equals(currentIcons);
        }
        int size = backup.size();
        if (size != currentIcons.size()) {
            return true;
        }
        if (size == 0) {
            return false;
        }
        PieMenu.Icon icon = backup.get(0);
        int i;
        for (i = 0; i < size; ++i) {
            if (currentIcons.get(i) == icon) {
                break;
            }
        }
        if (i >= size) {
            return true;
        }
        for (int j = 1; j < size; ++j) {
            if (backup.get(j) != currentIcons.get(++i % size)) {
                return true;
            }
        }
        return false;
    }

    public void setTouch(Point touch) {
        this.touch = touch;
    }

    public void setTwist(double twist) {
        this.twist = twist;
    }

    public Apps.AppIcon getGrabbedIcon() {
        return grabbedIcon;
    }

    public long getGrabbedIconAt() {
        return grabbedIconAt;
    }

    public int getLastInsertAt() {
        return lastInsertAt;
    }

    public List<Apps.AppIcon> getUngrabbedIcons() {
        return ungrabbedIcons;
    }

    public boolean hasGrabbedIcon() {
        return grabbedIcon != null;
    }

    public void editIcon(Apps.AppIcon icon) {
        grabbedIcon = icon;
        grabbedIconAt = android.os.SystemClock.uptimeMillis();
        ungrabbedIcons.clear();
        ungrabbedIcons.addAll(pieMenu.icons);

        if (pieMenu.isRigid()) {
            grabbedSourceSlot = pieMenu.icons.indexOf(grabbedIcon);
            lastHoveredSlot = -1;
            displacedHovered = null;
            displacedSource = null;
            if (grabbedSourceSlot >= 0) {
                Apps.AppIcon ghost = Apps.AppIcon.createGhost();
                ghost.size = grabbedIcon.size;
                ghost.x = grabbedIcon.x;
                ghost.y = grabbedIcon.y;
                ghost.setStartPosition(grabbedIcon.x, grabbedIcon.y);
                pieMenu.icons.set(grabbedSourceSlot, ghost);
            } else if (grabbedIcon != null) {
                Apps.ensureRigidCapacityForAdd(pieMenu.icons);
            }
            listener.invalidate();
            return;
        }

        if (grabbedIcon != null) {
            ungrabbedIcons.remove(grabbedIcon);
            lastInsertAt = -1;
        } else {
            pieMenu.icons.clear();
            pieMenu.icons.addAll(ungrabbedIcons);
        }
        listener.invalidate();
    }

    public void resetGrabbedItem() {
        if (grabbedIcon == null) {
            return;
        }
        if (pieMenu.isRigid()) {
            resetDisplacedApp();
            if (grabbedSourceSlot >= 0 && grabbedSourceSlot < pieMenu.icons.size()) {
                grabbedIcon.setStartPosition(grabbedIcon.x, grabbedIcon.y);
                pieMenu.icons.set(grabbedSourceSlot, grabbedIcon);
            }
            grabbedIcon = null;
            lastHoveredSlot = -1;
            displacedHovered = null;
            displacedSource = null;
            pieMenu.calculate(lastCenterX, lastCenterY);
            pieMenu.updateSmoothing();
            listener.invalidate();
            return;
        }
        pieMenu.icons.clear();
        pieMenu.icons.addAll(ungrabbedIcons);
        pieMenu.icons.add(grabbedIcon);
        grabbedIcon = null;
        pieMenu.calculate(lastCenterX, lastCenterY);
        listener.invalidate();
    }

    public void storeMenu() {
        listener.storeMenu();
    }

    public void commitGrabbedItem() {
        if (grabbedIcon == null) {
            return;
        }
        if (pieMenu.isRigid()) {
            commitRigidGrabbedItem();
            return;
        }
        listener.storeMenu();
        grabbedIcon = null;
        pieMenu.calculate(lastCenterX, lastCenterY);
        listener.invalidate();
    }

    private void commitRigidGrabbedItem() {
        int targetSlot = lastHoveredSlot;
        if (targetSlot >= 0 && targetSlot < pieMenu.icons.size() && targetSlot != grabbedSourceSlot) {
            Apps.AppIcon targetIcon = pieMenu.icons.get(targetSlot);
            Apps.AppIcon sourceIcon = grabbedSourceSlot >= 0 ? pieMenu.icons.get(grabbedSourceSlot) : null;
            if (targetIcon != null && sourceIcon != null) {
                displacedHovered = null;
                displacedSource = null;
                pieMenu.icons.set(grabbedSourceSlot, targetIcon);
                pieMenu.icons.set(targetSlot, grabbedIcon);
            } else {
                resetDisplacedApp();
                pieMenu.icons.set(targetSlot, grabbedIcon);
                if (grabbedSourceSlot >= 0) {
                    pieMenu.icons.set(grabbedSourceSlot, Apps.AppIcon.createGhost());
                }
            }
        } else {
            resetDisplacedApp();
            if (grabbedSourceSlot >= 0 && grabbedSourceSlot < pieMenu.icons.size()) {
                pieMenu.icons.set(grabbedSourceSlot, grabbedIcon);
            }
        }
        Apps.shrinkRigidCapacityIfNeeded(pieMenu.icons);
        grabbedIcon.setStartPosition(grabbedIcon.x, grabbedIcon.y);
        grabbedIcon = null;
        lastHoveredSlot = -1;
        displacedHovered = null;
        displacedSource = null;
        int cx = lastCenterX > 0 ? lastCenterX : pieMenu.getCenterX();
        int cy = lastCenterY > 0 ? lastCenterY : pieMenu.getCenterY();
        pieMenu.calculate(cx, cy);
        pieMenu.updateSmoothing();
        listener.storeMenu();
        listener.invalidate();
    }

    public void removeGrabbedIcon() {
        resetDisplacedApp();
        grabbedIcon = null;
        grabbedSourceSlot = -1;
        lastHoveredSlot = -1;
        displacedHovered = null;
        displacedSource = null;
        if (pieMenu.isRigid()) {
            Apps.shrinkRigidCapacityIfNeeded(pieMenu.icons);
        }
        int cx = lastCenterX > 0 ? lastCenterX : pieMenu.getCenterX();
        int cy = lastCenterY > 0 ? lastCenterY : pieMenu.getCenterY();
        pieMenu.calculate(cx, cy);
        pieMenu.updateSmoothing();
        listener.storeMenu();
        listener.invalidate();
    }

    public void calculateEditablePie(int centerX, int centerY) {
        if (grabbedIcon == null || touch == null) {
            return;
        }
        lastCenterX = centerX;
        lastCenterY = centerY;

        if (pieMenu.isRigid()) {
            calculateRigidEditablePie(centerX, centerY);
            return;
        }

        int endIndex = ungrabbedIcons.size();
        double step = PieMenu.TAU / (endIndex + 1);
        double angle = PieMenu.getPositiveAngle(Math.atan2(
                touch.y - centerY,
                touch.x - centerX) - twist + step * .5);
        int insertAt = Math.min(endIndex, (int) Math.floor(angle / step));
        if (insertAt != lastInsertAt) {
            if (lastInsertAt == 0 && insertAt == endIndex) {
                Collections.rotate(ungrabbedIcons, 1);
            } else if (lastInsertAt == endIndex && insertAt == 0) {
                Collections.rotate(ungrabbedIcons, -1);
            }

            pieMenu.icons.clear();
            pieMenu.icons.addAll(ungrabbedIcons);
            pieMenu.icons.add(insertAt, grabbedIcon);
            pieMenu.updateSmoothing();

            if (lastInsertAt < 0) {
                pieMenu.calculate(centerX, centerY);
                grabbedIcon.x = touch.x;
                grabbedIcon.y = touch.y;
                pieMenu.initSmoothing();
            }
            lastInsertAt = insertAt;
        }

        pieMenu.calculate(touch.x, touch.y);
        grabbedIcon.x = touch.x;
        grabbedIcon.y = touch.y;
    }

    private void calculateRigidEditablePie(int centerX, int centerY) {
        grabbedIcon.x = touch.x;
        grabbedIcon.y = touch.y;

        int numberOfSlots = pieMenu.icons.size();
        if (numberOfSlots < 1) {
            return;
        }
        double rad = pieMenu.getRadius();
        double centeredY = touch.y - centerY;
        double centeredX = touch.x - centerX;
        double cursorRadius = Math.sqrt(centeredY * centeredY + centeredX * centeredX);
        double infieldRadius = rad / 2f;
        double outfieldRadius = rad * 1.5;

        int hoveredSlot = -1;
        double step = PieMenu.TAU / numberOfSlots;
        if (cursorRadius >= infieldRadius && cursorRadius <= outfieldRadius) {
            double cursorAngle = Math.atan2(centeredY, centeredX);
            double angleOffset = PieMenu.getPositiveAngle(cursorAngle - twist + step * .5);
            hoveredSlot = (int) Math.floor(angleOffset / step) % numberOfSlots;
        }

        if (hoveredSlot != lastHoveredSlot) {
            resetDisplacedApp();
            if (hoveredSlot >= 0 && hoveredSlot != grabbedSourceSlot && grabbedSourceSlot >= 0) {
                Apps.AppIcon targetIcon = pieMenu.icons.get(hoveredSlot);
                Apps.AppIcon sourceIcon = pieMenu.icons.get(grabbedSourceSlot);
                if (targetIcon != null && sourceIcon != null) {
                    double aSource = twist + grabbedSourceSlot * step;
                    double aHovered = twist + hoveredSlot * step;
                    targetIcon.x = centerX + (int) Math.round(rad * Math.cos(aSource));
                    targetIcon.y = centerY + (int) Math.round(rad * Math.sin(aSource));
                    sourceIcon.x = centerX + (int) Math.round(rad * Math.cos(aHovered));
                    sourceIcon.y = centerY + (int) Math.round(rad * Math.sin(aHovered));
                    displacedHovered = targetIcon;
                    displacedSource = sourceIcon;
                }
            }
            pieMenu.updateSmoothing();
            lastHoveredSlot = hoveredSlot;
            listener.invalidate();
        }
    }

    private void resetDisplacedApp() {
        if (displacedHovered != null && lastHoveredSlot >= 0 && lastHoveredSlot < pieMenu.icons.size()) {
            double step = PieMenu.TAU / pieMenu.icons.size();
            double aHovered = twist + lastHoveredSlot * step;
            int cx = lastCenterX > 0 ? lastCenterX : pieMenu.getCenterX();
            int cy = lastCenterY > 0 ? lastCenterY : pieMenu.getCenterY();
            displacedHovered.x = cx + (int) Math.round(pieMenu.getRadius() * Math.cos(aHovered));
            displacedHovered.y = cy + (int) Math.round(pieMenu.getRadius() * Math.sin(aHovered));
            displacedHovered = null;
        }
        if (displacedSource != null && grabbedSourceSlot >= 0 && grabbedSourceSlot < pieMenu.icons.size()) {
            double step = PieMenu.TAU / pieMenu.icons.size();
            double aSource = twist + grabbedSourceSlot * step;
            int cx = lastCenterX > 0 ? lastCenterX : pieMenu.getCenterX();
            int cy = lastCenterY > 0 ? lastCenterY : pieMenu.getCenterY();
            displacedSource.x = cx + (int) Math.round(pieMenu.getRadius() * Math.cos(aSource));
            displacedSource.y = cy + (int) Math.round(pieMenu.getRadius() * Math.sin(aSource));
            displacedSource = null;
        }
    }
}
