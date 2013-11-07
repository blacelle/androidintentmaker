package blasd.android.intentmaker;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class TestIntentFactory {
	@Test
	public void testNullInFlags() {
		IntentMaker iff = new IntentMaker(Collections.<Integer> singletonList(null));

		Assert.assertTrue(iff.defaultFlags.isEmpty());
	}
}
