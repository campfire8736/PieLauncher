package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class FolderItem extends PieItem {
	public static final int DEFAULT_COLOR = 0xffffffff;

	public final String id;
	public String label;
	public int color;
	public final List<PieItem> items = new ArrayList<>();

	public FolderItem(String id, String label, Bitmap icon) {
		this(id, label, DEFAULT_COLOR, icon);
	}

	public FolderItem(String id, String label, int color, Bitmap icon) {
		super(icon);
		this.id = id;
		this.label = label;
		this.color = color;
	}

	public FolderItem(String id, String label, Bitmap icon, List<PieItem> items) {
		this(id, label, DEFAULT_COLOR, icon, items);
	}

	public FolderItem(String id, String label, int color, Bitmap icon, List<PieItem> items) {
		super(icon);
		this.id = id;
		this.label = label;
		this.color = color;
		if (items != null) {
			this.items.addAll(items);
		}
	}

	public void updateMetadata(Context context, String newLabel, int newColor) {
		this.label = newLabel;
		this.color = newColor;
		this.bitmap = FolderManager.createFolderIcon(context, newColor);
	}

	@Override
	public String getItemKey(Context context) {
		return "folder:" + id;
	}

	@Override
	public String getLabel() {
		return label != null ? label : "Folder";
	}

	@Override
	public boolean launch(Context context) {
		return true;
	}

	@Override
	public List<PieItemAction> getEditActions(Context context, boolean hasPacks) {
		List<PieItemAction> actions = new ArrayList<>();
		actions.add(new DeleteFolderAction());
		actions.add(new EditFolderAction());
		return actions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FolderItem)) {
			return false;
		}
		FolderItem folder = (FolderItem) o;
		return id != null ? id.equals(folder.id) : folder.id == null;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
