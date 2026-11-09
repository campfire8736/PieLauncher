package de.markusfisch.android.pielauncher.widget.state;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Process;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.graphics.CanvasPieMenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class PieEditorTest {

    @Mock
    private PieEditor.Listener mockListener;

    private PieEditor pieEditor;
    private CanvasPieMenu<Apps.AppIcon> pieMenu;
    private Bitmap mockBitmap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        pieMenu = new CanvasPieMenu<>();
        pieEditor = new PieEditor(mockListener, pieMenu);
        mockBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    }

    private Apps.AppIcon createTestAppIcon(String label) {
        try {
            java.lang.reflect.Constructor<Apps.AppIcon> constructor =
                    Apps.AppIcon.class.getDeclaredConstructor(
                            ComponentName.class,
                            String.class,
                            Bitmap.class,
                            android.os.UserHandle.class
                    );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    new ComponentName("com.test", "Activity"),
                    label,
                    mockBitmap,
                    Process.myUserHandle()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AppIcon", e);
        }
    }

    @Test
    public void testEditIcon_SetsGrabbedIconAndInvalidates() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);

        assertTrue(pieEditor.hasGrabbedIcon());
        assertEquals(icon, pieEditor.getGrabbedIcon());
        assertFalse(pieEditor.getUngrabbedIcons().contains(icon));
        verify(mockListener, times(1)).invalidate();
    }

    @Test
    public void testResetGrabbedItem_RestoresGrabbedIcon() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);
        pieEditor.resetGrabbedItem();

        assertFalse(pieEditor.hasGrabbedIcon());
        assertTrue(pieMenu.icons.contains(icon));
        verify(mockListener, times(2)).invalidate(); // 1 for editIcon, 1 for resetGrabbedItem
    }

    @Test
    public void testCalculateEditablePie_UpdatesIconPosition() {
        Apps.AppIcon icon = createTestAppIcon("Test App");
        pieMenu.icons.add(icon);

        pieEditor.editIcon(icon);
        pieEditor.setTwist(0f);
        pieEditor.setTouch(new Point(200, 200));

        pieEditor.calculateEditablePie(100, 100);

        assertEquals(200, icon.x);
        assertEquals(200, icon.y);
    }

    @Test
    public void testCalculateEditablePie_MagnifiesIconsNearTouch() {
        Apps.AppIcon icon1 = createTestAppIcon("Icon 1");
        Apps.AppIcon icon2 = createTestAppIcon("Icon 2");
        Apps.AppIcon icon3 = createTestAppIcon("Icon 3");
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);
        pieMenu.icons.add(icon3);

        pieMenu.set(100, 100, 80, 0.0, 1f);
        pieEditor.editIcon(icon1);
        pieEditor.setTwist(0f);

        // Move touch away from center
        pieEditor.setTouch(new Point(180, 100));
        pieEditor.calculateEditablePie(100, 100);

        double minSize = Double.MAX_VALUE;
        double maxSize = Double.MIN_VALUE;
        for (Object obj : pieMenu.icons) {
            Apps.AppIcon ic = (Apps.AppIcon) obj;
            if (ic.size < minSize) minSize = ic.size;
            if (ic.size > maxSize) maxSize = ic.size;
        }

        assertTrue("Icons should have varying sizes based on proximity to touch, but min=" + minSize + " max=" + maxSize,
                maxSize > minSize);
    }

    @Test
    public void testTransactionLifecycle() {
        Apps.AppIcon icon1 = createTestAppIcon("Test App 1");
        Apps.AppIcon icon2 = createTestAppIcon("Test App 2");
        java.util.List<Apps.AppIcon> initialIcons = new java.util.ArrayList<>();
        initialIcons.add(icon1);
        initialIcons.add(icon2);

        // Begin edit
        pieEditor.beginEdit(initialIcons);

        // Modify pieMenu directly (simulating an edit)
        pieMenu.icons.clear();
        pieMenu.icons.add(icon1);

        // Rollback should restore the original list
        pieEditor.rollback(pieMenu.icons);
        assertEquals(2, pieMenu.icons.size());
        assertTrue(pieMenu.icons.contains(icon2));

        // Begin edit again
        pieEditor.beginEdit(initialIcons);

        // Modify pieMenu directly
        pieMenu.icons.clear();
        pieMenu.icons.add(icon1);

        // Commit should leave the backup empty but keep pieMenu as is
        pieEditor.commit();
        assertEquals(1, pieMenu.icons.size());
        assertFalse(pieMenu.icons.contains(icon2));
    }

    @Test
    public void testRigidEditIconLiftsIconLeavingSlotNull() {
        pieMenu.setRigid(true);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);

        pieEditor.editIcon(icon1);

        assertTrue(pieEditor.hasGrabbedIcon());
        assertEquals(icon1, pieEditor.getGrabbedIcon());
        // Source slot should now be ghost
        assertTrue(pieMenu.icons.get(0).isGhost);
        assertEquals(icon2, pieMenu.icons.get(1));
    }

    @Test
    public void testRigidHoverSwapPreviewAnimation() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1); // Slot 0
        pieMenu.icons.add(icon2); // Slot 1
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1); // Grab icon 1 from Slot 0

        // Hover over Slot 1 (angle PI, x=200, y=500)
        pieEditor.setTouch(new Point(200, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Icon 2 should be displaced to Slot 0's coordinates (angle 0 -> 700, 500)
        assertEquals(700, icon2.x);
        assertEquals(500, icon2.y);

        // Hover away back to Slot 0
        pieEditor.setTouch(new Point(800, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Icon 2 should be restored to its home Slot 1 coordinates (angle PI -> 300, 500)
        assertEquals(300, icon2.x);
        assertEquals(500, icon2.y);
    }

    @Test
    public void testRigidCommitSwapsOccupiedSlots() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1); // Slot 0
        pieMenu.icons.add(icon2); // Slot 1
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1);
        // Hover over Slot 1
        pieEditor.setTouch(new Point(200, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Release over Slot 1
        pieEditor.commitGrabbedItem();

        assertFalse(pieEditor.hasGrabbedIcon());
        // Icon 2 is now in Slot 0, Icon 1 is now in Slot 1
        assertEquals(icon2, pieMenu.icons.get(0));
        assertEquals(icon1, pieMenu.icons.get(1));
    }

    @Test
    public void testRigidCommitMovesIntoEmptySlot() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        // 4 slots: [icon1, icon2, ghost, ghost]
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1); // From Slot 0

        // Hover over Slot 2 (angle PI, x=200, y=500) which is empty
        pieEditor.setTouch(new Point(200, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Release
        pieEditor.commitGrabbedItem();

        // Slot 0 is now empty (ghost), Slot 2 has icon1, Slot 1 has icon2
        assertTrue(pieMenu.icons.get(0).isGhost);
        assertEquals(icon2, pieMenu.icons.get(1));
        assertEquals(icon1, pieMenu.icons.get(2));
    }

    @Test
    public void testRigidIsMenuChangedDetectsSwappedSlots() {
        pieMenu.setRigid(true);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        java.util.List<Apps.AppIcon> initial = new java.util.ArrayList<>();
        initial.add(icon1);
        initial.add(icon2);

        pieEditor.beginEdit(initial);

        java.util.List<Apps.AppIcon> swapped = new java.util.ArrayList<>();
        swapped.add(icon2);
        swapped.add(icon1);

        assertTrue(pieEditor.isMenuChanged(swapped));
    }

    @Test
    public void testRigidSelectAndReleaseWithoutMoving_PreservesAllAppsAndSlots() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        Apps.AppIcon icon3 = createTestAppIcon("App 3");
        // S = 4: [icon1, icon2, icon3, ghost]
        pieMenu.icons.add(icon1); // Slot 0
        pieMenu.icons.add(icon2); // Slot 1
        pieMenu.icons.add(icon3); // Slot 2
        pieMenu.icons.add(Apps.AppIcon.createGhost()); // Slot 3
        pieMenu.calculate(500, 500);

        // Tap on icon 1 at slot 0 (angle 0 -> 700, 500)
        pieEditor.editIcon(icon1);
        pieEditor.setTouch(new Point(700, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Release immediately without moving
        pieEditor.commitGrabbedItem();

        assertEquals(4, pieMenu.icons.size());
        assertEquals(icon1, pieMenu.icons.get(0));
        assertEquals(icon2, pieMenu.icons.get(1));
        assertEquals(icon3, pieMenu.icons.get(2));
        assertTrue(pieMenu.icons.get(3).isGhost);
    }

    @Test
    public void testRigidDragOutsidePieMenu_RestoresToSourceSlot() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1); // Slot 0
        pieMenu.icons.add(icon2); // Slot 1
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1);
        // Drag far outside pie menu (radius is 200, infield is 100, outfield is 300)
        // Pointing at y = 1000 (distance = 500 > outfield)
        pieEditor.setTouch(new Point(500, 1000));
        pieEditor.calculateEditablePie(500, 500);

        pieEditor.commitGrabbedItem();

        assertEquals(icon1, pieMenu.icons.get(0));
        assertEquals(icon2, pieMenu.icons.get(1));
    }

    @Test
    public void testRigidRemoveGrabbedIcon() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1);
        pieEditor.removeGrabbedIcon();

        assertFalse(pieEditor.hasGrabbedIcon());
        // Slot 0 is ghost
        assertTrue(pieMenu.icons.get(0).isGhost);
        assertEquals(icon2, pieMenu.icons.get(1));
    }

    @Test
    public void testRigidReleaseAnimatesFromTouchPosition() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon icon2 = createTestAppIcon("App 2");
        pieMenu.icons.add(icon1);
        pieMenu.icons.add(icon2);
        pieMenu.calculate(500, 500);

        pieEditor.editIcon(icon1);
        // Drag to (400, 500)
        pieEditor.setTouch(new Point(400, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Commit release
        pieEditor.commitGrabbedItem();

        // Icon 1 should start its smoothing animation from the release touch position (400, 500)
        assertEquals(400, icon1.getSmoothedX());
        assertEquals(500, icon1.getSmoothedY());
    }

    @Test
    public void testRigidHoverOverEmptySlotAnimatesGhostDisappearance() {
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        Apps.AppIcon icon1 = createTestAppIcon("App 1");
        Apps.AppIcon ghost1 = Apps.AppIcon.createGhost();
        pieMenu.icons.add(icon1); // Slot 0 (angle 0 -> 700, 500)
        pieMenu.icons.add(ghost1); // Slot 1 (angle PI -> 300, 500)
        pieMenu.calculate(500, 500);

        double baseGhostSize = ghost1.size;
        assertTrue(baseGhostSize > 0);

        pieEditor.editIcon(icon1); // Slot 0 now has ghost0

        // Hover over Slot 1 (angle PI, x=200, y=500) which is ghost1
        pieEditor.setTouch(new Point(200, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Ghost 1 does NOT swap coordinates to Slot 0; it stays at Slot 1 (300, 500)
        assertEquals(300, ghost1.x);
        assertEquals(500, ghost1.y);
        // Ghost 1 target size becomes 0 to animate disappearance
        assertEquals(0.0, ghost1.size, 0.001);

        // Hovered slot is 1 (under the cursor)
        assertEquals(1, pieEditor.getLastHoveredSlot());
        assertEquals(0, pieEditor.getGrabbedSourceSlot());
        assertFalse(pieEditor.isDisplacedOccupied());

        // Hover away back to Slot 0
        pieEditor.setTouch(new Point(800, 500));
        pieEditor.calculateEditablePie(500, 500);

        // Ghost 1 restores its base size to animate reappearance, staying at (300, 500)
        assertEquals(baseGhostSize, ghost1.size, 0.001);
        assertEquals(300, ghost1.x);
        assertEquals(500, ghost1.y);
        assertEquals(0, pieEditor.getLastHoveredSlot());
    }
}
