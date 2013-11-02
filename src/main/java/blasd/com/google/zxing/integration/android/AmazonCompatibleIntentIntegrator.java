package blasd.com.google.zxing.integration.android;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import blasd.android.intentmaker.ContextBasedIntentFactory;
import blasd.android.intentmaker.IIntentFactory;
import blasd.android.intentmaker.IntentFactory;

/**
 * Extends BarcodeScanner {@link IntentIntegrator} in order to be compatible
 * with Amazon store
 * 
 * @author BLA
 * 
 */
public class AmazonCompatibleIntentIntegrator extends IntentIntegrator {
	/**
	 * The package of a barcode scanner application available in both Google
	 * Play and Amazon. The package used by ZXing on Google Play is used by an
	 * application since 2 years on Amazon
	 */
	protected static final String BS_STAR_PACKAGE = "blasd." + BS_PACKAGE;

	public AmazonCompatibleIntentIntegrator(@Nonnull Activity activity) {
		super(activity);

		// Add the BarCode Scanner package which is available for free in both
		// GooglePlay and Amazon
		List<String> targetApplications = new ArrayList<String>(TARGET_ALL_KNOWN);
		targetApplications.add(0, BS_STAR_PACKAGE);

		setTargetApplications(targetApplications);
	}

	/**
	 * If no application can handle the BarcodeScanner intent, we propose the
	 * user to download a compatible application
	 */
	@Override
	protected AlertDialog showDownloadDialog() {
		AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
		downloadDialog.setTitle(title);
		downloadDialog.setMessage(message);
		downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				String packageName = targetApplications.get(0);

				ContextBasedIntentFactory factory = ContextBasedIntentFactory.buildFactory(activity.getApplication(), "Download Barcode Scanner",
						IntentFactory.POPUP_FLAGS);

				@SuppressWarnings("null")
				Intent intent = factory.openMarket(activity.getApplication(), packageName);

				factory.startActivity(activity, intent);
			}
		});
		downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
			}
		});
		return downloadDialog.show();
	}
}
