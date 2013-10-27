package blasd.android.intentmaker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class IntentFactory {

	@Nonnull
	public static final String GOOGLE_PLAY_INSTALLER_PACKAGE_NAME = "com.android.vending";
	@Nonnull
	public static final String AMAZON_INSTALLER_PACKAGE_NAME = "com.amazon.venezia";

	@Nonnull
	protected final Set<Integer> defaultFlags;

	/**
	 * If not null, {@link Intent} are wrapped by a call to
	 * Intent.createChooser(i)
	 */
	@Nullable
	protected final CharSequence defaultIntentChooserTitle;

	/**
	 * Holds the flags Intent.FLAG_ACTIVITY_CLEAR_TOP and
	 * Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
	 * 
	 * It should be used for intents opening a popup activity (e.g. a transient
	 * activity like an image attachment in a email application, or google play
	 * when requesting the user to vote for current application)
	 */
	@SuppressWarnings("null")
	@Nonnull
	public static final Collection<Integer> POPUP_FLAGS = Collections.unmodifiableList(Arrays.asList(Intent.FLAG_ACTIVITY_CLEAR_TOP,
			Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));

	/**
	 * Media {@link Intent} can be used to record 3 kinds of media
	 * 
	 * http://developer.android.com/reference/android/provider/MediaStore.html
	 * 
	 * @author BLA
	 * 
	 */
	public enum MediaType {
		IMAGE, AUDIO, VIDEO,
	};

	/**
	 * Media {@link Intent} can be used to record to 2 kinds of storages
	 * 
	 * @author BLA
	 * 
	 */
	public enum MediaLocation {
		INTERNAL, EXTERNAL,
	};

	/**
	 * By default, {@link Intent} are created without any flags and are wrapped
	 * in an {@link Intent#createChooser(i)}
	 */
	@SuppressWarnings("null")
	public IntentFactory() {
		this.defaultFlags = Collections.emptySet();
		this.defaultIntentChooserTitle = "Pick Application";
	}

	public IntentFactory(int... defaultFlags) {
		this(null, defaultFlags);
	}

	public IntentFactory(CharSequence intentChooserTitle, int... defaultFlags) {
		this.defaultFlags = new HashSet<Integer>();

		// The ellipse is allowed to be null
		if (defaultFlags != null) {
			for (int i : defaultFlags) {
				this.defaultFlags.add(i);
			}
		}

		this.defaultIntentChooserTitle = intentChooserTitle;
	}

	public IntentFactory(@Nonnull Collection<Integer> defaultFlags) {
		this(null, defaultFlags);
	}

	public IntentFactory(CharSequence intentChooserTitle, @Nonnull Collection<Integer> defaultFlags) {
		// Shallow copy
		this.defaultFlags = new HashSet<Integer>(defaultFlags);

		this.defaultIntentChooserTitle = intentChooserTitle;
	}

	/**
	 * 
	 * @param additionalFlag
	 * @return true if this set did not already contain the specified element
	 */
	public boolean addDefaultFlag(int additionalFlag) {
		return this.defaultFlags.add(additionalFlag);
	}

	/**
	 * 
	 * @param intent
	 *            the {@link Intent} to configure
	 * @param intentChooserTitle
	 *            a nullable {@link CharSequence}. If not null, the intent is
	 *            wrapped in an Intent.createChooser
	 * @return
	 */
	public Intent configureIntent(@Nullable Intent intent, @Nullable CharSequence intentChooserTitle) {
		if (intent == null) {
			return null;
		} else {
			// Add the default flags
			for (Integer flag : defaultFlags) {
				intent.addFlags(flag);
			}

			// Wrap in a chooser
			if (intentChooserTitle != null) {
				intent = Intent.createChooser(intent, intentChooserTitle);
			}

			return intent;
		}
	}

	/**
	 * generates an intent for selecting media <br>
	 * <br>
	 * (when type == <b>MediaType.VIDEO</b>, picker doesn't work. <br>
	 * instead, it looks for a video player to play selected one.)
	 * 
	 * @param title
	 * @param type
	 * @param location
	 * @return
	 */
	public Intent captureMediaContent(MediaType type, MediaLocation location) {
		final Uri uri;

		switch (type) {
		case IMAGE:
			if (location == MediaLocation.INTERNAL) {
				uri = android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI;
			} else {
				uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
			}
			break;
		case AUDIO:
			if (location == MediaLocation.INTERNAL) {
				uri = android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
			} else {
				uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			}
			break;
		case VIDEO:
			if (location == MediaLocation.INTERNAL) {
				uri = android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI;
			} else {
				uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
			}
			break;

		default:
			// Unexpected case as we went through the enum
			return null;
		}

		Intent intent = new Intent(Intent.ACTION_PICK, uri);

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * 
	 * @param type
	 * @return an {@link Intent} to retrieve a media content
	 */
	public Intent getMediaContentIntent(MediaType type) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		if (type == MediaType.IMAGE) {
			intent.setType("image/*");
		} else if (type == MediaType.AUDIO) {
			intent.setType("audio/*");
		} else if (type == MediaType.VIDEO) {
			intent.setType("video/*");
		}
		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * generates an intent for taking photos/videos that saves result to given
	 * uri
	 * 
	 * @param type
	 *            either one of <b>MediaType.IMAGE</b> or <b>MediaType.VIDEO</b>
	 * @param outputUri
	 * @return null if error
	 */
	public Intent getCameraIntent(@Nonnull MediaType type, @Nonnull Uri outputUri) {
		Intent intent = null;
		if (type == MediaType.IMAGE) {
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
		} else if (type == MediaType.VIDEO) {
			intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
		}
		return intent;
	}

	/**
	 * As some markets forbid hosted applications to reference others market
	 * (e.g. Amazon Market), one needs to easily open the market from which an
	 * application has been installed.
	 * 
	 * @param application
	 * @return an {@link Intent} opening the appropriate market for current
	 *         application,
	 * @throws NullPointerException
	 *             if application is null
	 */
	public Intent openMarketForCurrentApplication(@Nonnull Application application) throws NullPointerException {
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
			marketIntent = openUrlInBuiltInBrowser("http://www.amazon.com/gp/mas/dl/android?p=" + targetPackageName);
		} else {
			doLog("openMarketForCurrentApplication: Unexpected installerName: " + currentApplicationInstallerPackageName);

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
		// http format for maximum compatbility
		Uri uri = Uri.parse("http://play.google.com/store/apps/details?id=" + packageName);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * 
	 * @param packageName
	 *            the package identifying uniquely an application
	 * @return an {@link Intent} opening Google Play with direct access to the
	 *         specific application. It uses the market:// scheme; however the
	 *         Intent will wail if the Google play is not present on the device
	 */
	public Intent openGooglePlayWithMarketScheme(@Nonnull String packageName) {
		// http://developer.android.com/distribute/googleplay/promote/linking.html
		Uri uri = Uri.parse("market://details?id=" + packageName);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * 
	 * @param url
	 * @return an Intent opening the input URL in the android built-in browser
	 */
	public Intent openUrlInBuiltInBrowser(@Nonnull String url) {
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			// http://stackoverflow.com/questions/2201917/how-can-i-open-a-url-in-androids-web-browser-from-my-application
			// Add the adequate prefix
			// TODO: Should we rather check with URI and add a schema if none if
			// found by the URI check?
			url = "http://" + url;
		}

		// Open the native browser
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * 
	 * @param mailTarget
	 *            the mail address of the destination
	 * @param extraSubject
	 *            the subject of the mail. This could be left to null
	 * @return an {@link Intent} opening a Mail Sender application
	 */
	public Intent openEMailSender(@Nonnull String mailTarget, String extraSubject) {
		// http://stackoverflow.com/questions/8701634/send-email-intent
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mailTarget, null));

		intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	/**
	 * {@link Intent} type defined in
	 * com.google.zxing.client.android.Intents.Scan.ACTION
	 */
	public static final String BARCODE_SCANNER_SCAN = "com.google.zxing.client.android.SCAN";

	public Intent openBarcodeScanner(@Nonnull String mailTarget, String extraSubject) {
		// http://stackoverflow.com/questions/8701634/send-email-intent
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mailTarget, null));

		intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);

		return configureIntent(intent, defaultIntentChooserTitle);
	}

	protected void doLog(@Nonnull String string) {
		// TODO Auto-generated method stub

	}
}