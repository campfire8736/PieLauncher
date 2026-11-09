package de.markusfisch.android.pielauncher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

import de.markusfisch.android.pielauncher.R;
import de.markusfisch.android.pielauncher.content.Apps;
import de.markusfisch.android.pielauncher.content.LauncherItemKey;

public class PickPieAppsAdapter extends ArrayAdapter<Apps.AppIcon> {
	private final Set<LauncherItemKey> selectedKeys;

	public PickPieAppsAdapter(
			Context context,
			List<Apps.AppIcon> apps,
			Set<LauncherItemKey> selectedKeys) {
		super(context, 0, apps);
		this.selectedKeys = selectedKeys;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater
					.from(parent.getContext())
					.inflate(R.layout.item_pick_pie_app, parent, false);
		}
		ViewHolder holder = getViewHolder(convertView);
		Apps.AppIcon appIcon = getItem(position);
		if (appIcon != null) {
			holder.nameView.setText(appIcon.label);
			holder.iconView.setImageBitmap(appIcon.bitmap);
			LauncherItemKey key = new LauncherItemKey(
					appIcon.componentName, appIcon.userHandle);
			boolean isSelected = selectedKeys.contains(key);
			holder.checkBox.setChecked(isSelected);
			holder.iconView.setAlpha(isSelected ? .5f : 1f);
		}
		return convertView;
	}

	private ViewHolder getViewHolder(View view) {
		ViewHolder holder;
		if ((holder = (ViewHolder) view.getTag()) == null) {
			holder = new ViewHolder();
			holder.iconView = view.findViewById(R.id.icon);
			holder.nameView = view.findViewById(R.id.name);
			holder.checkBox = view.findViewById(R.id.selected);
			view.setTag(holder);
		}
		return holder;
	}

	private static final class ViewHolder {
		private ImageView iconView;
		private TextView nameView;
		private CheckBox checkBox;
	}
}
