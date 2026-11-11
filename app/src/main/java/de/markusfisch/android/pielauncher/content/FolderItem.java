package de.markusfisch.android.pielauncher.content;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

public class FolderItem extends PieItem {
	public final String id;
	public String label;
	public final List<PieItem> items = new ArrayList<>();

	public FolderItem(String id, String label, Bitmap icon) {
		super(icon);
		this.id = id;
		this.label = label;
	}

	public FolderItem(String id, String label, Bitmap icon, List<PieItem> items) {
		super(icon);
		this.id = id;
		this.label = label;
		if (items != null) {
			this.items.addAll(items);
		}
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
