package blasd.android.intentmaker;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nonnull;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.webkit.MimeTypeMap;

public class ContextBasedIntentFactory implements IIntentFactory {

	@Nonnull
	public static final String GOOGLE_PLAY_INSTALLER_PACKAGE_NAME = "com.android.vending";
	@Nonnull
	public static final String AMAZON_INSTALLER_PACKAGE_NAME = "com.amazon.venezia";

	protected IntentFactory intentFactory;
	protected ContextHelper contextHelper;

	public ContextBasedIntentFactory(@Nonnull IntentFactory intentFactory, @Nonnull ContextHelper intentFactoryHelper) {
		this.intentFactory = intentFactory;
		this.contextHelper = intentFactoryHelper;
	}

	/**
	 * As some markets forbid hosted applications to reference others market
	 * (e.g. Amazon Market), one needs to easily open the market from which an
	 * application has been installed.
	 * 
	 * @param application
	 * @return an {@link Intent} opening the appropriate market for current
	 *         application,
	 * @throws IllegalArgumentException
	 *             if application is null
	 */
	public Intent openMarketForCurrentApplication(Application application) throws IllegalArgumentException {
		if (application == null) {
			throw new IllegalArgumentException("Application is null");
		}

		String currentApplicationPackage = application.getPackageName();

		if (currentApplicationPackage == null) {
			return null;
		} else {
			return openMarket(application, currentApplicationPackage);
		}
	}

	public Intent openMarket(@Nonnull Application application, @Nonnull String targetPackageName) throws NullPointerException {
		String currentApplicationPackageName = application.getPackageName();

		String currentApplicationInstallerPackageName = application.getPackageManager().getInstallerPackageName(currentApplicationPackageName);

		final Intent marketIntent;

		if (GOOGLE_PLAY_INSTALLER_PACKAGE_NAME.equals(currentApplicationInstallerPackageName)) {
			// Installed by GooglePlay: rate in Google Play
			marketIntent = openGooglePlay(targetPackageName);
		} else if (AMAZON_INSTALLER_PACKAGE_NAME.equals(currentApplicationInstallerPackageName)) {
			// Installed by Amazon: rate in Amazon
			// TODO: choose the domain depending on the Locale
			// http://www.amazon.com/gp/mas/dl/android?p=com.example.package&ref=mas_pm_app_name
			marketIntent = intentFactory.openUrlInBuiltInBrowser("http://www.amazon.com/gp/mas/dl/android?p=" + targetPackageName);
		} else {
			intentFactory.doLog("openMarketForCurrentApplication: Unexpected installerName: " + currentApplicationInstallerPackageName);

			// Unknown installerPackageName: fall-back on Google Play
			marketIntent = openGooglePlay(targetPackageName);
		}

		return marketIntent;
	}

	/**
	 * 
	 * @param packageName
	 *            the package identifying uniquely an application
	 * @return an {@link Intent} opening Google Play with direct access to the
	 *         specific application. It privileges the http:// scheme over the
	 *         market:// scheme as Google Play is not available on all devices.
	 *         Moreover, the http:// scheme will offer the user to open the
	 *         GooglePlay application
	 */
	public Intent openGooglePlay(@Nonnull String packageName) {
		// http://developer.android.com/distribute/googleplay/promote/linking.html
		// "In general, you should use http:// format for links on web pages and
		// market:// for links in Android apps."
		// However, Google Play may not be installed on some devices: use the
		// http format for maximum compatbility.
		// For instance, testers on Samsung

		final Uri uri;
		if (contextHelper.isGooglePlayInstalled()) {
			// Use market intent if Google Play is available
			uri = Uri.parse("market://details?id=" + packageName);
		} else {
			// ELse, fallback on Browser intent
			uri = Uri.parse(buildGooglePlayURL(packageName));
		}

		Intent intent = new Intent(Intent.ACTION_VIEW, uri);

		return configureIntent(intent);
	}

	public String buildGooglePlayURL(String packageName) {
		return "http://play.google.com/store/apps/details?id=" + packageName;
	}

	protected Intent configureIntent(Intent intent) {
		return intentFactory.configureIntent(intent);
	}

	public static ContextBasedIntentFactory buildFactory(Context context) {
		return buildFactory(context, null, null);
	}

	public static ContextBasedIntentFactory buildFactory(Context context, Collection<Integer> popupFlags) {
		return buildFactory(context, null, popupFlags);
	}

	public static ContextBasedIntentFactory buildFactory(Context context, CharSequence intentChooserTitle, Collection<Integer> defaultFlags) {
		if (context == null) {
			throw new RuntimeException("Application is null");
		}

		return new ContextBasedIntentFactory(new IntentFactory(intentChooserTitle, defaultFlags), new ContextHelper(context));
	}

	public void startActivity(Activity activity, Intent intent) {
		intentFactory.startActivity(activity, intent);
	}

	protected Uri makeApplicationPrivateTemporaryFile(String extention) throws IOException {
		// http://stackoverflow.com/questions/3425906/creating-temporary-files-in-android
		File outputDir = contextHelper.getAppContext().getCacheDir();
		// This create a temporary file on the internal storage
		File tempFile = File.createTempFile("barcode", "." + extention, outputDir);

		return Uri.fromFile(tempFile);
	}

	/**
	 * 
	 * @param subject
	 * @param text
	 * @param bitmap
	 * @return
	 * @throws IOException
	 * 
	 *             Requires
	 *             {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE}.
	 */
	public Intent sendBitmap(String subject, String text, @Nonnull Bitmap bitmap) throws IOException {
		if (!contextHelper.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			return null;
		}

		// Save as a JPEG by default
		String pathofBmp = Images.Media.insertImage(contextHelper.getAppContext().getContentResolver(), bitmap, "title", text);

		if (pathofBmp == null) {
			// We are probably missing the permission
			return null;
		}

		Uri imageUri = Uri.parse(pathofBmp);

		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, text);

		intent.putExtra(Intent.EXTRA_STREAM, imageUri);

		{
			String extentionLowerCase = "jpg";
			// MIME types prefer lower cases
			if (MimeTypeMap.getSingleton().hasExtension(extentionLowerCase)) {
				intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extentionLowerCase));
			} else {
				intent.setTypeAndNormalize("image/" + extentionLowerCase);
			}
		}

		return configureIntent(intent);
	}

	@Override
	public Intent send(String mimeType, String subject, String text) {
		return intentFactory.send(mimeType, subject, text);
	}
}
