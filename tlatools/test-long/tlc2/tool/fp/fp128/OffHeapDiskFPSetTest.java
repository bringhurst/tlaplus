// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;
import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.util.FP128;

public class OffHeapDiskFPSetTest extends TestCase {

	private final Random rnd = new Random(15041980L);
	private final long limit = Integer.MAX_VALUE;

	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testPutFP128() throws IOException {
		
		// Verification set
		final Set<FP128> fps = new HashSet<FP128>();
		
		// FPSet under test
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new FPSetConfiguration());
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "Test.fp");
		
		// Fill FPSet with random fingerprints
		for (long l = 0; l < limit; l++) {
			final FP128 fp128 = new DummyFP128(rnd.nextLong(), rnd.nextLong());
			fps.add(fp128);
			assertFalse(fpSet.put(fp128));
			assertTrue(fpSet.contains(fp128));
		}
		
		// Quickly verify size of fpSet
		assertEquals(limit, fpSet.size());

		// Verify all FP128 are correctly stored in fpSet
		final Iterator<FP128> iterator = fps.iterator();
		while (iterator.hasNext()) {
			assertTrue(fpSet.contains(iterator.next()));
		}
	}
}
