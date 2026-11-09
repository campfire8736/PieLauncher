package de.markusfisch.android.pielauncher.preference;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;

class SystemSettings {
	private final ContentResolver contentResolver;

	private float animatorDurationScale = 1f;

	public SystemSettings(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
		contentResolver.registerContentObserver(
				Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
				false,
				new ContentObserver(null) {
					@Override
					public void onChange(boolean selfChange) {
						onAnimatorScaleChanged();
					}
				});
		onAnimatorScaleChanged();
	}

	public float getAnimatorDurationScale() {
		return animatorDurationScale;
	}

	private void onAnimatorScaleChanged() {
		animatorDurationScale = Settings.Global.getFloat(contentResolver,
				Settings.Global.ANIMATOR_DURATION_SCALE,
				animatorDurationScale);
	}
}
