package blasd.android.intentmaker;

import java.util.List;

import javax.annotation.Nonnull;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class ContextHelper {
	@Nonnull
	protected final Context context;

	public ContextHelper(@Nonnull Context context) {
		this.context = context;
	}

	public Context getAppContext() {
		return context;
	}

	/**
	 * 
	 * @return true if the network is connected or connecting
	 * 
	 * @see ConnectivityManager
	 * @see {@link NetworkInfo#isConnectedOrConnecting()}
	 * 
	 *      Requires {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
	 */
	public boolean isConnectedOrConnecting() {
		ConnectivityManager cm = (ConnectivityManager) getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@link Context#checkPermission(String, int, int)}
	 */
	public boolean checkPermission(String permission) {
		return PackageManager.PERMISSION_GRANTED == getAppContext().checkCallingOrSelfPermission(permission);
	}

	/**
	 * Equivalent of GooglePlayServicesUtil.isGooglePlayServicesAvailable, but
	 * without the dependency
	 * 
	 * @return true if the GOogle Play application is available
	 */
	public boolean isGooglePlayInstalled() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://search?q=foo"));
		PackageManager pm = getAppContext().getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);

		if (list == null || list.isEmpty()) {
			// No application managing market Intent
			return false;
		} else {
			// At least one application managing market intent
			return true;
		}
	}
}
