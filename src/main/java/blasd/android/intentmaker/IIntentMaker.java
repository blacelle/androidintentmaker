package blasd.android.intentmaker;

import android.content.Intent;

public interface IIntentMaker {

	Intent send(String mimeType, String subject, String text);
}
