package blasd.android.intentmaker;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

public class TestIntentFactoryWithContext {
	@Nonnull
	public static final String DEFAULT_PACKAGE = "some.reallyCool.package";

	@Test
	public void testGooglePlayURL() {
		Context c = Mockito.mock(ContextWrapper.class);

		IntentMakerWithContext iff = IntentMakerWithContext.makeFactory(c);

		Assert.assertEquals("http://play.google.com/store/apps/details?id=" + DEFAULT_PACKAGE, iff.buildGooglePlayURL(DEFAULT_PACKAGE));
	}

	// Ignore as failing since Android is stubbed with RuntimeException
	@Ignore
	@Test
	public void testNullInFlags() {
		Context c = Mockito.mock(ContextWrapper.class);

		IntentMakerWithContext iff = IntentMakerWithContext.makeFactory(c);

		Intent i = iff.openGooglePlay(DEFAULT_PACKAGE);

		Assert.assertEquals("http://play.google.com/store/apps/details?id=some.package", i.getAction());
		Assert.assertEquals("http://play.google.com/store/apps/details?id=some.package", i.getDataString());
	}
}
