// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;
import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.util.FP128;

public class OffHeapDiskFPSetTest extends TestCase {

	private final Random rnd = new Random(15041980L);

	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testSimple() throws IOException {
		// Verification set
		final Set<FP128> fps = new TreeSet<FP128>();
		
		final int fp128Cnt = 32;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestSimple.fp");

		// Spread the fingerprints evenly across fingerprint space
		long foo = Long.MAX_VALUE / (fp128Cnt / 4L);
		
		long high = Long.MAX_VALUE;
		for (int i = 0; i < fp128Cnt * 2; i++) {
			DummyFP128 fp = new DummyFP128(0, high);
			fps.add(fp);
			fpSet.put(fp);
			high -= foo;
		}
		
		assertEquals(fp128Cnt * 2, fpSet.size());
		
		for (FP128 fp128 : fps) {
			assertTrue("Expecting fp: " + fp128, fpSet.contains(fp128));
		}
	}
	
	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testPutFP128() throws IOException {
		
		// The largest fingeprint
		FP128 largest = new DummyFP128(Long.MIN_VALUE, Long.MIN_VALUE);
		
		// Verification set
		final Set<FP128> fps = new HashSet<FP128>();
		
		// FPSet under test
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new FPSetConfiguration());
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "Test.fp");
		
		final long limit = Integer.MAX_VALUE;
		
		// Fill FPSet with random fingerprints
		for (long l = 0; l < limit; l++) {
			final FP128 fp128 = new DummyFP128(rnd.nextLong(), rnd.nextLong());
			
			// keep largest fingerprint to verify in getLast
			if (fp128.compareTo(largest) == 1) {
				largest = fp128;
			}
			
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
	
	@SuppressWarnings("serial")
	private class ExplicitFPSetConfiguration extends FPSetConfiguration {

		public ExplicitFPSetConfiguration(int fp128Cnt) {
			memoryInBytes = fp128Cnt * 8 * 2;
		}

		@Override
		public long getMemoryInFingerprintCnt() {
			return memoryInBytes / 8 / 2;
		}
	}
}
