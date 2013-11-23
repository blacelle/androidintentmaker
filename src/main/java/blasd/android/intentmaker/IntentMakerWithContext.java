package blasd.android.intentmaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nonnull;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

/**
 * This is a kind of advanced {@link IntentMaker}, with additional
 * {@link Intent} factories requiring a {@link Context}
 * 
 * @author BLA
 * 
 */
public class IntentMakerWithContext implements IIntentMaker {

	/**
	 * The installer package name used by Google Play
	 */
	@Nonnull
	public static final String GOOGLE_PLAY_INSTALLER_PACKAGE_NAME = "com.android.vending";

	/**
	 * The installer package name used by Amazon
	 */
	@Nonnull
	public static final String AMAZON_INSTALLER_PACKAGE_NAME = "com.amazon.venezia";

	protected final IntentMaker intentFactory;
	protected final ContextProviderForIntentFactory contextHelper;

	/**
	 * In some situations,
	 * {@link PackageManager#getInstallerPackageName(String)} will return null.
	 * It is the case if installed through the debugger, or when deployed by
	 * Amazon application tester.
	 */
	protected static String defaultPackageName = GOOGLE_PLAY_INSTALLER_PACKAGE_NAME;

	public IntentMakerWithContext(@Nonnull IntentMaker intentFactory, @Nonnull ContextProviderForIntentFactory intentFactoryHelper) {
		this.intentFactory = intentFactory;
		this.contextHelper = intentFactoryHelper;
	}

	public static void setDefaultInstallerPackageName(String defaultPackageName) {
		IntentMakerWithContext.defaultPackageName = defaultPackageName;
	}

	/**
	 * As some markets forbid hosted applications to reference others market
	 * (e.g. Amazon Market), one needs to easily open the market from which an
	 * application has been installed.
	 * 
	 * @return an {@link Intent} opening the appropriate market for current
	 *         application.
	 * @throws IllegalArgumentException
	 *             if application is null
	 */
	public Intent openMarketForCurrentApplication() throws IllegalArgumentException {
		String currentApplicationPackage = contextHelper.getAppContext().getPackageName();

		if (currentApplicationPackage == null) {
			return null;
		} else {
			return openMarketForTargetPackageName(currentApplicationPackage);
		}
	}

	/**
	 * Make an Intent to open the most appropriate market for given application
	 * 
	 * @param application
	 * @param targetPackageName
	 * @return
	 * @throws NullPointerException
	 */
	public Intent openMarketForTargetPackageName(@Nonnull String targetPackageName) throws NullPointerException {
		String currentApplicationPackageName = contextHelper.getAppContext().getPackageName();

		String currentApplicationInstallerPackageName = contextHelper.getAppContext().getPackageManager()
				.getInstallerPackageName(currentApplicationPackageName);

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
			if (currentApplicationInstallerPackageName != null) {
				intentFactory.doLog("openMarketForCurrentApplication: Unexpected installerName: " + currentApplicationInstallerPackageName);
			}

			if (GOOGLE_PLAY_INSTALLER_PACKAGE_NAME.equals(defaultPackageName)) {
				// Fallback on Google Play
				marketIntent = openGooglePlay(targetPackageName);
			} else if (AMAZON_INSTALLER_PACKAGE_NAME.equals(defaultPackageName)) {
				// Fallback on Amazon
				marketIntent = intentFactory.openUrlInBuiltInBrowser("http://www.amazon.com/gp/mas/dl/android?p=" + targetPackageName);
			} else {
				// Invalid fallback: Fallback on Google Play
				marketIntent = openGooglePlay(targetPackageName);
			}
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

	public static IntentMakerWithContext makeFactory(Context context) {
		return makeFactory(context, null, null);
	}

	public static IntentMakerWithContext makeFactory(Context context, Collection<Integer> popupFlags) {
		return makeFactory(context, null, popupFlags);
	}

	public static IntentMakerWithContext makeFactory(Context context, CharSequence intentChooserTitle, Collection<Integer> defaultFlags) {
		if (context == null) {
			throw new RuntimeException("Application is null");
		}

		return new IntentMakerWithContext(new IntentMaker(intentChooserTitle, defaultFlags), new ContextProviderForIntentFactory(context));
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
		Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));

		Uri imageUri;
		if (contextHelper.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			// If we are allowed to write the external storage, write in the
			// Shared image folder
			// Save as a JPEG by default
			String pathofBmp = Images.Media.insertImage(contextHelper.getAppContext().getContentResolver(), bitmap, "title", text);

			imageUri = Uri.parse(pathofBmp);
		} else {
			// http://developer.android.com/reference/android/support/v4/content/FileProvider.html
			File cacheFOlder = contextHelper.getAppContext().getCacheDir();

			// TODO: shared_history should be a parameter
			File folder = new File(contextHelper.getAppContext().getFilesDir(), "shared_history");

			// Write in the application cache
			File pathofBmp = insertImage(contextHelper.getAppContext().getContentResolver(), bitmap, "title", text, folder);

			// http://stackoverflow.com/questions/3004713/get-content-uri-from-file-path-in-android
			// Works after Android 2.2
			// imageUri = Uri.fromFile(pathofBmp);

			imageUri = FileProvider.getUriForFile(contextHelper.getAppContext(), "net.blasd.fileprovider", pathofBmp);

			// Alloy the target application to read this URL
			// intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}

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

	protected static final File insertImage(ContentResolver cr, Bitmap source, String title, String description, File cacheDir) {
		// http://stackoverflow.com/questions/7540386/android-saving-and-loading-a-bitmap-in-cache-from-diferent-activities
		try {
			cacheDir.mkdirs();
			File f = new File(cacheDir, "shared.jpg");
			FileOutputStream out = new FileOutputStream(f);
			source.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();

			return f;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Intent send(String mimeType, String subject, String text) {
		return intentFactory.send(mimeType, subject, text);
	}
}
