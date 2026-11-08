package de.markusfisch.android.pielauncher.graphics;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PieMenuTest {

	private static class TestIcon extends PieMenu.Icon {}

	private static TestIcon createGhost() {
		TestIcon icon = new TestIcon();
		icon.isGhost = true;
		return icon;
	}

	private PieMenu<TestIcon> pieMenu;

	@Before
	public void setUp() {
		pieMenu = new PieMenu<>();
		pieMenu.set(500, 500, 200, 0, 1f);
	}

	@Test
	public void testStandardModeCalculatesEquidistantSlices() {
		pieMenu.setRigid(false);
		pieMenu.icons.add(new TestIcon());
		pieMenu.icons.add(new TestIcon());
		pieMenu.icons.add(new TestIcon());

		pieMenu.calculate(500, 500);

		assertEquals(3, pieMenu.icons.size());
		// Center of first icon at twist=0 (angle 0)
		TestIcon icon0 = pieMenu.icons.get(0);
		assertEquals(700, icon0.x);
		assertEquals(500, icon0.y);
	}

	@Test
	public void testRigidModeTwoSlots() {
		pieMenu.setRigid(true);
		TestIcon icon0 = new TestIcon();
		TestIcon icon1 = new TestIcon();
		pieMenu.icons.add(icon0);
		pieMenu.icons.add(icon1);

		pieMenu.calculate(500, 500);

		// Slot 0 at angle 0: (500 + 200, 500) = (700, 500)
		assertEquals(700, icon0.x);
		assertEquals(500, icon0.y);

		// Slot 1 at angle PI: (500 - 200, 500) = (300, 500)
		assertEquals(300, icon1.x);
		assertEquals(500, icon1.y);
	}

	@Test
	public void testRigidModeFourSlotsWithEmptySlot() {
		pieMenu.setRigid(true);
		TestIcon icon0 = new TestIcon();
		TestIcon icon1 = new TestIcon();
		TestIcon icon2 = new TestIcon();
		// 4 slots: Slot 0, Slot 1, Slot 2 occupied; Slot 3 empty (ghost)
		pieMenu.icons.add(icon0);
		pieMenu.icons.add(icon1);
		pieMenu.icons.add(icon2);
		pieMenu.icons.add(createGhost());

		pieMenu.calculate(500, 500);

		// Slot 0: angle 0 (700, 500)
		assertEquals(700, icon0.x);
		assertEquals(500, icon0.y);

		// Slot 1: angle PI/2 (90 deg, bottom in screen coords): (500, 700)
		assertEquals(500, icon1.x);
		assertEquals(700, icon1.y);

		// Slot 2: angle PI (180 deg): (300, 500)
		assertEquals(300, icon2.x);
		assertEquals(500, icon2.y);
	}

	@Test
	public void testRigidModeDeadZoneHitTesting() {
		pieMenu.setRigid(true);
		TestIcon icon0 = new TestIcon();
		pieMenu.icons.add(icon0);
		pieMenu.icons.add(createGhost()); // Slot 1 is empty (ghost)

		// Cursor pointing toward Slot 0 (angle 0)
		pieMenu.calculate(800, 500);
		assertEquals(0, pieMenu.getSelectedIcon());

		// Cursor pointing toward Slot 1 (angle PI, x=200, y=500)
		pieMenu.calculate(200, 500);
		assertEquals(-1, pieMenu.getSelectedIcon()); // Empty slot dead zone!

		// Cursor in center infield (cursorRadius < 100)
		pieMenu.calculate(520, 500);
		assertEquals(-1, pieMenu.getSelectedIcon());
	}

	@Test
	public void testRigidModePositionsUnchangedWhenCapacityExpands() {
		pieMenu.setRigid(true);
		// S = 4
		TestIcon iconA = new TestIcon();
		TestIcon iconB = new TestIcon();
		TestIcon iconC = new TestIcon();
		TestIcon iconD = new TestIcon();
		pieMenu.icons.add(iconA); // Slot 0 (0 deg)
		pieMenu.icons.add(iconB); // Slot 1 (90 deg)
		pieMenu.icons.add(iconC); // Slot 2 (180 deg)
		pieMenu.icons.add(iconD); // Slot 3 (270 deg)

		pieMenu.calculate(500, 500);

		int xA4 = iconA.x, yA4 = iconA.y;
		int xB4 = iconB.x, yB4 = iconB.y;
		int xC4 = iconC.x, yC4 = iconC.y;
		int xD4 = iconD.x, yD4 = iconD.y;

		// Expand to S = 8: slots 0, 2, 4, 6 hold A, B, C, D
		pieMenu.icons.clear();
		pieMenu.icons.add(iconA); // 0
		pieMenu.icons.add(new TestIcon()); // 1 (45 deg)
		pieMenu.icons.add(iconB); // 2 (90 deg)
		pieMenu.icons.add(createGhost());  // 3
		pieMenu.icons.add(iconC); // 4 (180 deg)
		pieMenu.icons.add(createGhost());  // 5
		pieMenu.icons.add(iconD); // 6 (270 deg)
		pieMenu.icons.add(createGhost());  // 7

		pieMenu.calculate(500, 500);

		// Verify existing icons have 100% identical coordinates
		assertEquals(xA4, iconA.x);
		assertEquals(yA4, iconA.y);
		assertEquals(xB4, iconB.x);
		assertEquals(yB4, iconB.y);
		assertEquals(xC4, iconC.x);
		assertEquals(yC4, iconC.y);
		assertEquals(xD4, iconD.x);
		assertEquals(yD4, iconD.y);
	}

	@Test
	public void testRigidModeTwistRotatesAllSlots() {
		pieMenu.setRigid(true);
		pieMenu.setTwist(Math.PI * 0.5); // 90 degree twist

		TestIcon icon0 = new TestIcon();
		TestIcon icon1 = new TestIcon();
		pieMenu.icons.add(icon0);
		pieMenu.icons.add(icon1);

		pieMenu.calculate(500, 500);

		// Slot 0 is now at 90 deg: (500, 700)
		assertEquals(500, icon0.x);
		assertEquals(700, icon0.y);

		// Slot 1 is now at 270 deg: (500, 300)
		assertEquals(500, icon1.x);
		assertEquals(300, icon1.y);
	}
}
