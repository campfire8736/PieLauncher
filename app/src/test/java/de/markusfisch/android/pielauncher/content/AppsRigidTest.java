package de.markusfisch.android.pielauncher.content;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Process;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class AppsRigidTest {

	private Bitmap mockBitmap;

	@Before
	public void setUp() {
		mockBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
	}

	private Apps.AppIcon createIcon(String name) {
		try {
			Constructor<Apps.AppIcon> c = Apps.AppIcon.class.getDeclaredConstructor(
					ComponentName.class, String.class, Bitmap.class, android.os.UserHandle.class);
			c.setAccessible(true);
			return c.newInstance(new ComponentName("com.test", name), name, mockBitmap, Process.myUserHandle());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testConvertToRigidFollowsBitReversal() {
		Apps.AppIcon a = createIcon("A");
		Apps.AppIcon b = createIcon("B");
		Apps.AppIcon c = createIcon("C");

		List<PieItem> menu = new ArrayList<>();
		menu.add(a);
		menu.add(b);
		menu.add(c);

		Apps.convertToRigid(menu);

		// 3 apps -> S = 4. Bit reversal slots:
		// i=0 -> Slot 0
		// i=1 -> Slot 2
		// i=2 -> Slot 1
		// Slot 3 is empty (ghost)
		assertEquals(4, menu.size());
		assertEquals(a, menu.get(0));
		assertEquals(c, menu.get(1));
		assertEquals(b, menu.get(2));
		assertTrue(Apps.isGhost(menu.get(3)));
	}

	@Test
	public void testConvertToStandardRemovesNulls() {
		Apps.AppIcon a = createIcon("A");
		Apps.AppIcon b = createIcon("B");

		List<PieItem> menu = new ArrayList<>();
		menu.add(a);
		menu.add(Apps.AppIcon.createGhost());
		menu.add(b);
		menu.add(Apps.AppIcon.createGhost());

		Apps.convertToStandard(menu);

		assertEquals(2, menu.size());
		assertEquals(a, menu.get(0));
		assertEquals(b, menu.get(1));
	}

	@Test
	public void testExpandRigidCapacityPreservesAngles() {
		Apps.AppIcon a = createIcon("A");
		Apps.AppIcon b = createIcon("B");
		Apps.AppIcon c = createIcon("C");
		Apps.AppIcon d = createIcon("D");

		List<PieItem> menu = new ArrayList<>();
		menu.add(a); // Slot 0
		menu.add(b); // Slot 1
		menu.add(c); // Slot 2
		menu.add(d); // Slot 3

		Apps.expandRigidCapacity(menu);

		// From 4 to 8: slot s becomes 2s
		assertEquals(8, menu.size());
		assertEquals(a, menu.get(0));
		assertTrue(Apps.isGhost(menu.get(1)));
		assertEquals(b, menu.get(2));
		assertTrue(Apps.isGhost(menu.get(3)));
		assertEquals(c, menu.get(4));
		assertTrue(Apps.isGhost(menu.get(5)));
		assertEquals(d, menu.get(6));
		assertTrue(Apps.isGhost(menu.get(7)));
	}

	@Test
	public void testShrinkRigidCapacityOnlyWhenAllEven() {
		Apps.AppIcon a = createIcon("A");
		Apps.AppIcon c = createIcon("C");

		List<PieItem> menu = new ArrayList<>();
		menu.add(a);    // Slot 0 (even)
		menu.add(Apps.AppIcon.createGhost()); // Slot 1
		menu.add(c);    // Slot 2 (even)
		menu.add(Apps.AppIcon.createGhost()); // Slot 3

		Apps.shrinkRigidCapacityIfNeeded(menu);

		// Both are even slots, so it shrinks from 4 to 2:
		assertEquals(2, menu.size());
		assertEquals(a, menu.get(0));
		assertEquals(c, menu.get(1));

		// Now test with an odd slot occupied:
		Apps.AppIcon b = createIcon("B");
		List<PieItem> menuWithOdd = new ArrayList<>();
		menuWithOdd.add(a); // Slot 0
		menuWithOdd.add(b); // Slot 1 (odd)
		menuWithOdd.add(Apps.AppIcon.createGhost());
		menuWithOdd.add(Apps.AppIcon.createGhost());

		Apps.shrinkRigidCapacityIfNeeded(menuWithOdd);

		// Must NOT shrink to 2 because slot 1 is odd and would shift angle
		assertEquals(4, menuWithOdd.size());
		assertEquals(a, menuWithOdd.get(0));
		assertEquals(b, menuWithOdd.get(1));
	}

	@Test
	public void testUpdateSelectionInRigidMenuPreservesSlots() {
		Apps.AppIcon a = createIcon("A");
		Apps.AppIcon b = createIcon("B");
		Apps.AppIcon c = createIcon("C");
		Apps.AppIcon d = createIcon("D");

		List<PieItem> menu = new ArrayList<>();
		menu.add(a); // Slot 0
		menu.add(b); // Slot 1
		menu.add(c); // Slot 2
		menu.add(Apps.AppIcon.createGhost()); // Slot 3

		// Deselect B, keep A and C, add D
		Set<LauncherItemKey> selectedKeys = new HashSet<>();
		selectedKeys.add(new LauncherItemKey(a.componentName, a.userHandle));
		selectedKeys.add(new LauncherItemKey(c.componentName, c.userHandle));
		selectedKeys.add(new LauncherItemKey(d.componentName, d.userHandle));

		List<Apps.AppIcon> allAvailable = new ArrayList<>();
		allAvailable.add(a);
		allAvailable.add(b);
		allAvailable.add(c);
		allAvailable.add(d);

		Apps.updateSelectionInRigidMenu(menu, selectedKeys, allAvailable);

		// A and C must remain in Slot 0 and Slot 2
		assertEquals(4, menu.size());
		assertEquals(a, menu.get(0));
		assertEquals(c, menu.get(2));
		// D fills the first free slot in priority order: Slot 1 (which was freed by B)
		assertEquals(d, menu.get(1));
		assertTrue(Apps.isGhost(menu.get(3)));
	}

	@Test
	public void testUpdateSelectionInRigidMenu_WithDistinctInstances_DoesNotDuplicateApps() {
		Apps.AppIcon aOld = createIcon("A");
		Apps.AppIcon aNew = createIcon("A"); // Distinct object instance
		Apps.AppIcon b = createIcon("B");

		List<PieItem> menu = new ArrayList<>();
		menu.add(aOld);
		menu.add(Apps.AppIcon.createGhost());

		Set<LauncherItemKey> selectedKeys = new HashSet<>();
		selectedKeys.add(new LauncherItemKey(aNew.componentName, aNew.userHandle));
		selectedKeys.add(new LauncherItemKey(b.componentName, b.userHandle));

		List<Apps.AppIcon> allAvailable = new ArrayList<>();
		allAvailable.add(aNew);
		allAvailable.add(b);

		Apps.updateSelectionInRigidMenu(menu, selectedKeys, allAvailable);

		// Size must be 2, slot 0 = A, slot 1 = B. A must NOT be duplicated!
		assertEquals(2, menu.size());
		assertEquals(aOld, menu.get(0));
		assertEquals(b, menu.get(1));
	}
}
