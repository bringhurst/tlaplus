// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;
import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.util.FP128;
import tlc2.util.Fingerprint;

public class OffHeapDiskFPSetTest extends TestCase {

	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testPutFP128() throws IOException {

		final long seed = 15041980L;
		final Random rnd = new Random(seed);
	
		// FPSet under test
		FPSetConfiguration fpSetConfiguration = new FPSetConfiguration();
		fpSetConfiguration.setFPImplementation(FP128.class);
		Fingerprint.FPFactory.init(fpSetConfiguration);
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(fpSetConfiguration);
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestPutFP128.fp");
		
		final long limit = Integer.MAX_VALUE;
		
		// Fill FPSet with random fingerprints
		for (long l = 0; l < limit; l++) {
			final FP128 fp128 = new DummyFP128(rnd.nextLong(), rnd.nextLong());
			assertFalse(fpSet.put(fp128));
			assertTrue(fpSet.contains(fp128));
		}
		
		// Quickly verify size of fpSet
		assertEquals(limit, fpSet.size());

		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		rnd.setSeed(seed);
		for (long l = 0; l <limit; l++) {
			assertTrue(fpSet.contains(new DummyFP128(rnd.nextLong(), rnd.nextLong())));
		}
		
		assertTrue(fpSet.checkInvariant());
	}
}
