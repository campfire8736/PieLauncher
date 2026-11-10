package de.markusfisch.android.pielauncher.widget.renderer;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Process;
import android.view.ContextThemeWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import de.markusfisch.android.pielauncher.R;

import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.widget.state.EditorAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import de.markusfisch.android.pielauncher.widget.state.Fader;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class EditorRendererTest {

    @Mock
    private Canvas mockCanvas;

    @Mock
    private Fader mockFaderEdit;

    private EditorRenderer editorRenderer;
    private Bitmap mockBitmap;
    private Rect dummyRect;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        editorRenderer = new EditorRenderer(context);
        mockBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        dummyRect = new Rect(0, 0, 50, 50);
        editorRenderer.setActionSize(100);
        editorRenderer.iconSplitPie = mockBitmap;
        editorRenderer.iconChangeTwist = mockBitmap;
        editorRenderer.iconChangeIconScale = mockBitmap;
        editorRenderer.iconChangeRadius = mockBitmap;
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
                    new ComponentName("com.example", "TestActivity"),
                    label,
                    mockBitmap,
                    Process.myUserHandle()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct AppIcon", e);
        }
    }

    private boolean invokeDraw(Fader fader, Apps.AppIcon grabbedIcon) {
        Paint paint = new Paint();
        de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
                new de.markusfisch.android.pielauncher.graphics.CanvasPieMenu();
        return editorRenderer.draw(
                mockCanvas,
                fader,
                pieMenu,
                grabbedIcon,
                android.os.SystemClock.uptimeMillis() - 300L,
                null,
                300f,
                false, false);
    }

    @Test
    public void testDraw_FaderEditInvisible_DoesNotDraw() {
        when(mockFaderEdit.isVisible()).thenReturn(false);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(0f);

        invokeDraw(mockFaderEdit, null);

        verify(mockCanvas, never()).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
    }

    @Test
    public void testDraw_FaderEditVisible_NoGrabbedIcon_DrawsDefaultActions() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        invokeDraw(mockFaderEdit, null);

        // Without a grabbed icon, 6 action icons are drawn (twist, scale, radius, add, preferences, done)
        verify(mockCanvas, times(6)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        verify(mockCanvas, times(2)).drawRoundRect(any(RectF.class), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testLayoutCalculatesTopAndBottomBars() {
        editorRenderer.layout(1080, 1920, 100, 10f, 1f, 16, 48);
        assertTrue(editorRenderer.topBarRect.left > 0);
        assertTrue(editorRenderer.topBarRect.right < 1080);
        assertTrue(editorRenderer.topBarRect.top > 0);
        assertTrue(editorRenderer.topBarRect.bottom > editorRenderer.topBarRect.top);
        assertTrue(editorRenderer.bottomBarRect.left > 0);
        assertTrue(editorRenderer.bottomBarRect.right < 1080);
        assertTrue(editorRenderer.bottomBarRect.bottom > editorRenderer.bottomBarRect.top);
    }

    @Test
    public void testDraw_FaderEditVisible_WithGrabbedIcon_DrawsGrabbedIconActionsAndCircles() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        // With a grabbed icon, 3 action icons are drawn (remove, edit/hide, details) plus the grabbed icon under cursor
        verify(mockCanvas, times(4)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        verify(mockCanvas, atLeastOnce()).drawCircle(anyFloat(), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testDraw_WithGrabbedShortcut_DrawsTwoActions() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon shortcutIcon = createTestAppIcon("Test Shortcut");
        shortcutIcon.shortcutId = "shortcut_123";
        shortcutIcon.shortcutPackage = "com.example.app";

        invokeDraw(mockFaderEdit, shortcutIcon);

        // With a grabbed shortcut, 2 action icons are drawn (remove, show app info) plus the grabbed icon = 3 bitmaps
        verify(mockCanvas, times(3)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        assertEquals(2, editorRenderer.getActiveGrabbedActions().size());
        assertEquals("remove_from_pie", editorRenderer.getActiveGrabbedActions().get(0).getId());
        assertEquals("app_info", editorRenderer.getActiveGrabbedActions().get(1).getId());
    }

    @Test
    public void testDraw_WithGrabbedDrawerIcon_DrawsOneAction() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon drawerIcon = createTestAppIcon("Drawer");
        // Simulate drawer package name
        java.lang.reflect.Field drawerPackageField;
        try {
            drawerPackageField = Apps.class.getDeclaredField("drawerPackageName");
            drawerPackageField.setAccessible(true);
            drawerPackageField.set(de.markusfisch.android.pielauncher.app.PieLauncherApp.apps, drawerIcon.componentName.getPackageName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        invokeDraw(mockFaderEdit, drawerIcon);

        // With a drawer icon, only 1 action icon is drawn (remove) plus the grabbed icon = 2 bitmaps
        verify(mockCanvas, times(2)).drawBitmap(any(Bitmap.class), nullable(Rect.class), any(Rect.class), any(Paint.class));
        assertEquals(1, editorRenderer.getActiveGrabbedActions().size());
        assertEquals("remove_from_pie", editorRenderer.getActiveGrabbedActions().get(0).getId());

        try {
            drawerPackageField.set(de.markusfisch.android.pielauncher.app.PieLauncherApp.apps, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDraw_WithGrabbedIcon_DoesNotDrawBottomBar() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");
        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas, never()).drawRoundRect(
                org.mockito.ArgumentMatchers.eq(new RectF(editorRenderer.bottomBarRect)),
                anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testDraw_RigidPie_WithGrabbedIcon_DrawsSlotHighlight() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
                new de.markusfisch.android.pielauncher.graphics.CanvasPieMenu();
        pieMenu.setRigid(true);
        pieMenu.icons.add(createTestAppIcon("App 1"));
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        pieMenu.calculate(500, 500);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        editorRenderer.draw(
                mockCanvas,
                mockFaderEdit,
                pieMenu,
                grabbedIcon,
                android.os.SystemClock.uptimeMillis() - 300L,
                null,
                300f,
                false,
                false,
                1,
                0,
                false);

        // Verify circle is drawn for slot highlight and drop zone
        verify(mockCanvas, atLeastOnce()).drawCircle(anyFloat(), anyFloat(), anyFloat(), any(Paint.class));
    }

    @Test
    public void testDraw_RigidPie_WithGrabbedIcon_OnlyDrawsActualEmptySpacesAsGhosts() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);

        de.markusfisch.android.pielauncher.graphics.CanvasPieMenu pieMenu =
                new de.markusfisch.android.pielauncher.graphics.CanvasPieMenu();
        pieMenu.setRigid(true);
        pieMenu.set(500, 500, 200, 0, 1f);
        // Slot 0: ghost (sourceSlot, origin of grabbed icon) -> at (700, 500)
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        // Slot 1: ghost (hoveredSlot, currently targeted and disappeared) -> at (500, 700)
        Apps.AppIcon hoveredGhost = Apps.AppIcon.createGhost();
        pieMenu.icons.add(hoveredGhost);
        // Slot 2: ghost (actual empty space!) -> at (300, 500)
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        // Slot 3: ghost (actual empty space!) -> at (500, 300)
        pieMenu.icons.add(Apps.AppIcon.createGhost());
        pieMenu.calculate(500, 500);

        // Hovered ghost has target size 0 and has animated to 0
        hoveredGhost.size = 0;
        hoveredGhost.initSmoothing();

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        editorRenderer.draw(
                mockCanvas,
                mockFaderEdit,
                pieMenu,
                grabbedIcon,
                android.os.SystemClock.uptimeMillis() - 300L,
                null,
                300f,
                false,
                false,
                1, // hoveredSlot
                0, // sourceSlot
                false);

        // Slot 2 (300, 500) and Slot 3 (500, 300) MUST have circles drawn (actual empty spaces)
        verify(mockCanvas).drawCircle(org.mockito.ArgumentMatchers.eq(300f), org.mockito.ArgumentMatchers.eq(500f), anyFloat(), any(Paint.class));
        verify(mockCanvas).drawCircle(org.mockito.ArgumentMatchers.eq(500f), org.mockito.ArgumentMatchers.eq(300f), anyFloat(), any(Paint.class));

        // Slot 1 (500, 700) is hoveredSlot, so it must NOT have any circle drawn
        verify(mockCanvas, never()).drawCircle(org.mockito.ArgumentMatchers.eq(500f), org.mockito.ArgumentMatchers.eq(700f), anyFloat(), any(Paint.class));

        // Slot 0 (700, 500) is sourceSlot, which has exactly ONE circle drawn (the white slot highlight, NOT a ghost circle)
        verify(mockCanvas, times(1)).drawCircle(org.mockito.ArgumentMatchers.eq(700f), org.mockito.ArgumentMatchers.eq(500f), anyFloat(), any(Paint.class));
    }

    @Test
    public void testGetActionAt_WithGrabbedIcon() {
        // Assume default rect size of 100x100 for testing
        editorRenderer.iconStartRect.set(0, 0, 100, 100);
        editorRenderer.iconCenterRect.set(100, 0, 200, 100);
        editorRenderer.iconEndRect.set(200, 0, 300, 100);
        editorRenderer.iconChangeTwistRect.set(0, 100, 100, 200);
        editorRenderer.iconChangeIconScaleRect.set(100, 100, 200, 200);
        editorRenderer.iconChangeRadiusRect.set(200, 100, 300, 200);

        editorRenderer.setActionSize(100);
        boolean hasGrabbedIcon = true;
        assertEquals(EditorAction.BACK_OR_REMOVE_ICON, editorRenderer.getActionAt(new Point(50, 50), hasGrabbedIcon));
        assertEquals(EditorAction.EDIT_SETTINGS_OR_UNDO, editorRenderer.getActionAt(new Point(150, 50), hasGrabbedIcon));
        assertEquals(EditorAction.END_EDIT_MODE, editorRenderer.getActionAt(new Point(250, 50), hasGrabbedIcon));

        // Change rects are ignored when holding an icon
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(50, 150), hasGrabbedIcon));
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(150, 150), hasGrabbedIcon));
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(250, 150), hasGrabbedIcon));

        // Outside everything
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(400, 400), hasGrabbedIcon));
    }

    @Test
    public void testGetActionAt_WithoutGrabbedIcon() {
        // Assume default rect size of 100x100 for testing
        editorRenderer.iconStartRect.set(0, 0, 100, 100);
        editorRenderer.iconCenterRect.set(100, 0, 200, 100);
        editorRenderer.iconEndRect.set(200, 0, 300, 100);
        editorRenderer.iconChangeTwistRect.set(0, 100, 100, 200);
        editorRenderer.iconChangeIconScaleRect.set(100, 100, 200, 200);
        editorRenderer.iconChangeRadiusRect.set(200, 100, 300, 200);

        editorRenderer.setActionSize(100);
        boolean hasGrabbedIcon = false;
        assertEquals(EditorAction.BACK_OR_REMOVE_ICON, editorRenderer.getActionAt(new Point(50, 50), hasGrabbedIcon));
        assertEquals(EditorAction.EDIT_SETTINGS_OR_UNDO, editorRenderer.getActionAt(new Point(150, 50), hasGrabbedIcon));
        assertEquals(EditorAction.END_EDIT_MODE, editorRenderer.getActionAt(new Point(250, 50), hasGrabbedIcon));

        assertEquals(EditorAction.CHANGE_TWIST, editorRenderer.getActionAt(new Point(50, 150), hasGrabbedIcon));
        assertEquals(EditorAction.CHANGE_ICON_SCALE, editorRenderer.getActionAt(new Point(150, 150), hasGrabbedIcon));
        assertEquals(EditorAction.CHANGE_RADIUS, editorRenderer.getActionAt(new Point(250, 150), hasGrabbedIcon));

        // Outside everything
        assertEquals(EditorAction.NONE, editorRenderer.getActionAt(new Point(400, 400), hasGrabbedIcon));
    }

    @Test
    public void testDraw_InPortraitWithGrabbedIcon_DrawsDragToOrderTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1080);
        when(mockCanvas.getHeight()).thenReturn(1920);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");
        editorRenderer.setTouch(new Point(500, 500));

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas).drawText(
                org.mockito.ArgumentMatchers.eq("Drag to arrange, circle to change order"),
                org.mockito.ArgumentMatchers.eq(540f),
                org.mockito.ArgumentMatchers.anyFloat(),
                any(Paint.class));
    }

    @Test
    public void testDraw_InPortraitWithGrabbedIcon_HoveringStartRect_DrawsRemoveIconTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1080);
        when(mockCanvas.getHeight()).thenReturn(1920);

        editorRenderer.iconStartRect.set(100, 100, 200, 200);
        editorRenderer.setActionSize(100);
        editorRenderer.setTouch(new Point(150, 150));

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas).drawText(
                org.mockito.ArgumentMatchers.eq("Remove icon from pie menu"),
                org.mockito.ArgumentMatchers.eq(540f),
                org.mockito.ArgumentMatchers.anyFloat(),
                any(Paint.class));
    }

    @Test
    public void testDraw_InLandscape_DoesNotDrawTip() {
        when(mockFaderEdit.isVisible()).thenReturn(true);
        when(mockFaderEdit.get(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyFloat())).thenReturn(1.0f);
        when(mockCanvas.getWidth()).thenReturn(1920);
        when(mockCanvas.getHeight()).thenReturn(1080);

        Apps.AppIcon grabbedIcon = createTestAppIcon("Grabbed App");

        invokeDraw(mockFaderEdit, grabbedIcon);

        verify(mockCanvas, never()).drawText(
                any(String.class),
                anyFloat(),
                anyFloat(),
                org.mockito.ArgumentMatchers.eq(editorRenderer.paintText));
    }

    @Test
    public void testLayout_InPortrait_DistributesButtonsHorizontally() {
        int width = 1080;
        int height = 1920;
        int controlsPadding = 80;

        editorRenderer.layout(width, height, controlsPadding, 12f, 2f, 4, 48);

        // Primary buttons (start, center, end) along bottom
        assertEquals(editorRenderer.iconStartRect.top, editorRenderer.iconCenterRect.top);
        assertEquals(editorRenderer.iconCenterRect.top, editorRenderer.iconEndRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconStartRect.left < editorRenderer.iconCenterRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconCenterRect.left < editorRenderer.iconEndRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconStartRect.top > height / 2);

        // Secondary buttons (twist, scale, radius) along top
        assertEquals(editorRenderer.iconChangeTwistRect.top, editorRenderer.iconChangeIconScaleRect.top);
        assertEquals(editorRenderer.iconChangeIconScaleRect.top, editorRenderer.iconChangeRadiusRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeTwistRect.left < editorRenderer.iconChangeIconScaleRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeIconScaleRect.left < editorRenderer.iconChangeRadiusRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeTwistRect.top < height / 2);

        org.junit.Assert.assertTrue(editorRenderer.getActionSize() > 0);
    }

    @Test
    public void testLayout_InLandscape_DistributesButtonsVertically() {
        int width = 1920;
        int height = 1080;
        int controlsPadding = 80;

        editorRenderer.layout(width, height, controlsPadding, 12f, 2f, 4, 48);

        // Primary buttons along right edge
        assertEquals(editorRenderer.iconStartRect.left, editorRenderer.iconCenterRect.left);
        assertEquals(editorRenderer.iconCenterRect.left, editorRenderer.iconEndRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconStartRect.top < editorRenderer.iconCenterRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconCenterRect.top < editorRenderer.iconEndRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconStartRect.left > width / 2);

        // Secondary buttons along left edge
        assertEquals(editorRenderer.iconChangeTwistRect.left, editorRenderer.iconChangeIconScaleRect.left);
        assertEquals(editorRenderer.iconChangeIconScaleRect.left, editorRenderer.iconChangeRadiusRect.left);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeTwistRect.top < editorRenderer.iconChangeIconScaleRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeIconScaleRect.top < editorRenderer.iconChangeRadiusRect.top);
        org.junit.Assert.assertTrue(editorRenderer.iconChangeTwistRect.left < width / 2);

        org.junit.Assert.assertTrue(editorRenderer.getActionSize() > 0);
    }

    @Test
    public void testActionIconsAreWhiteInLightAndDarkThemes() {
        android.content.Context appContext = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        android.content.Context lightContext = new ContextThemeWrapper(appContext, R.style.HomeTheme_Light);
        EditorRenderer lightRenderer = new EditorRenderer(lightContext);

        assertBitmapIsWhite("light iconRemove", lightRenderer.iconRemove);
        assertBitmapIsWhite("light iconEdit", lightRenderer.iconEdit);
        assertBitmapIsWhite("light iconHide", lightRenderer.iconHide);
        assertBitmapIsWhite("light iconDetails", lightRenderer.iconDetails);

        android.content.Context darkContext = new ContextThemeWrapper(appContext, R.style.HomeTheme);
        EditorRenderer darkRenderer = new EditorRenderer(darkContext);

        assertBitmapIsWhite("dark iconRemove", darkRenderer.iconRemove);
        assertBitmapIsWhite("dark iconEdit", darkRenderer.iconEdit);
        assertBitmapIsWhite("dark iconHide", darkRenderer.iconHide);
        assertBitmapIsWhite("dark iconDetails", darkRenderer.iconDetails);
    }

    private static void assertBitmapIsWhite(String name, Bitmap bitmap) {
        org.junit.Assert.assertNotNull(name + " should not be null", bitmap);
        boolean hasOpaquePixel = false;
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 50) {
                    hasOpaquePixel = true;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    assertEquals(name + " Red channel should be 255 for white icon", 255, r);
                    assertEquals(name + " Green channel should be 255 for white icon", 255, g);
                    assertEquals(name + " Blue channel should be 255 for white icon", 255, b);
                }
            }
        }
        assertTrue(name + " bitmap should have visible pixels", hasOpaquePixel);
    }
}
