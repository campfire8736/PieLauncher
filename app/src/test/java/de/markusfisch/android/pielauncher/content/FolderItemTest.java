package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.markusfisch.android.pielauncher.app.PieLauncherApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FolderItemTest {

	private Context context;

	@Before
	public void setUp() {
		context = ApplicationProvider.getApplicationContext();
		FolderManager.clearCache();
	}

	@After
	public void tearDown() {
		FolderManager.clearCache();
	}

	@Test
	public void testFolderItemBasics() {
		Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
		FolderItem folder = new FolderItem("f1", "My Folder", bitmap);

		assertEquals("folder:f1", folder.getItemKey(context));
		assertEquals("My Folder", folder.getLabel());
		assertTrue(folder.launch(context));

		List<PieItemAction> actions = folder.getEditActions(context, false);
		assertEquals(2, actions.size());
		assertEquals(DeleteFolderAction.ID, actions.get(0).getId());
		assertEquals("Delete folder", actions.get(0).getTip(context));
		assertEquals(EditFolderAction.ID, actions.get(1).getId());
		assertEquals("Edit folder", actions.get(1).getTip(context));
	}

	@Test
	public void testFolderManagerCreateAndResolve() {
		FolderItem folder = FolderManager.createNewFolder(context, new HashMap<>());
		assertNotNull(folder);
		assertNotNull(folder.id);
		assertEquals("folder:" + folder.id, folder.getItemKey(context));

		// Resolve via PieItemRegistry
		PieItem restored = PieItemRegistry.restoreItem(context, "folder:" + folder.id, new HashMap<>());
		assertNotNull(restored);
		assertTrue(restored instanceof FolderItem);
		assertEquals(folder, restored);
	}

	@Test
	public void testDeleteFolderActionDelegatesToListener() {
		FolderItem folder = new FolderItem("f2", "Folder 2", null);
		DeleteFolderAction action = new DeleteFolderAction();

		final FolderItem[] deleted = new FolderItem[1];
		PieItemAction.ActionListener listener = new PieItemAction.ActionListener() {
			@Override public void removeFromPie(PieItem item) {}
			@Override public void changeIcon(PieItem item) {}
			@Override public void hideApp(Apps.AppIcon item) {}
			@Override public void requestAppInfo(Apps.AppIcon item) {}
			@Override public void removePinnedShortcut(Apps.AppIcon item) {}
			@Override public void deleteFolder(FolderItem f) { deleted[0] = f; }
			@Override public void editFolder(FolderItem f) {}
			@Override public void endEditMode() {}
		};

		action.perform(context, folder, listener);
		assertEquals(folder, deleted[0]);
	}

	@Test
	public void testEditFolderActionDelegatesToListener() {
		FolderItem folder = new FolderItem("f2", "Folder 2", null);
		EditFolderAction action = new EditFolderAction();

		final FolderItem[] edited = new FolderItem[1];
		PieItemAction.ActionListener listener = new PieItemAction.ActionListener() {
			@Override public void removeFromPie(PieItem item) {}
			@Override public void changeIcon(PieItem item) {}
			@Override public void hideApp(Apps.AppIcon item) {}
			@Override public void requestAppInfo(Apps.AppIcon item) {}
			@Override public void removePinnedShortcut(Apps.AppIcon item) {}
			@Override public void deleteFolder(FolderItem f) {}
			@Override public void editFolder(FolderItem f) { edited[0] = f; }
			@Override public void endEditMode() {}
		};

		action.perform(context, folder, listener);
		assertEquals(folder, edited[0]);
	}

	@Test
	public void testFolderMetadataAndColorPersistence() {
		int testColor = 0xffff5722;
		FolderItem folder = FolderManager.createNewFolder(context, "custom_f", "Work Apps", testColor, null);
		assertNotNull(folder);
		assertEquals("Work Apps", folder.getLabel());
		assertEquals(testColor, folder.color);
		assertNotNull(folder.bitmap);

		// Clear memory cache to force reloading from database
		FolderManager.clearCache();

		FolderItem restored = FolderManager.getOrCreateFolder(context, "custom_f", new HashMap<>());
		assertNotNull(restored);
		assertEquals("Work Apps", restored.getLabel());
		assertEquals(testColor, restored.color);
		assertNotNull(restored.bitmap);

		// Update metadata
		int newColor = 0xff4caf50;
		FolderManager.updateFolderMetadata(context, restored, "Productivity", newColor);
		assertEquals("Productivity", restored.getLabel());
		assertEquals(newColor, restored.color);

		FolderManager.clearCache();
		FolderItem reRestored = FolderManager.getOrCreateFolder(context, "custom_f", new HashMap<>());
		assertEquals("Productivity", reRestored.getLabel());
		assertEquals(newColor, reRestored.color);
	}
}
