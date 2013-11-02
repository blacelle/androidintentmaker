package blasd.android.intentmaker;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class TestIntentFactory {
	@Test
	public void testNullInFlags() {
		IntentFactory iff = new IntentFactory(Collections.<Integer> singletonList(null));

		Assert.assertTrue(iff.defaultFlags.isEmpty());
	}
}
