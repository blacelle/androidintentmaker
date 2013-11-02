package blasd.android.intentmaker;

import android.content.Intent;

public interface IIntentFactory {

	Intent send(String mimeType, String subject, String text);
}
