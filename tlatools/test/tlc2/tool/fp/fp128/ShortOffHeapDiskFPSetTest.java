// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.util.BufferedRandomAccessFile;
import tlc2.util.FP128;

public class ShortOffHeapDiskFPSetTest extends TestCase {

	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testSimple() throws IOException {
		
		final int fp128Cnt = 32;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestSimple.fp");

		// Spread the fingerprints evenly across fingerprint space
		final long frob = Long.MAX_VALUE / (fp128Cnt / 2L);
		
		long high = Long.MAX_VALUE;
		for (int i = 0; i < fp128Cnt; i++) {
			DummyFP128 fp = new DummyFP128(0, high);
			//System.out.println(i + " adding: " + fp);
			fpSet.put(fp);
			high -= frob;
		}
		
		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		high = Long.MAX_VALUE;
		for (long l = 0; l < fp128Cnt; l++) {
			assertTrue(fpSet.contains(new DummyFP128(0, high)));
			high -= frob;
		}
		// Check that we have found all elements and that those are _all_
		// elements in the set
		assertEquals(fp128Cnt, fpSet.size());
	}
	
	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testSimpleExplicitFlush() throws IOException {

		final int fp128Cnt = 32;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestSimpleExplicitFlush.fp");

		// Spread the fingerprints evenly across fingerprint space
		final long frob = Long.MAX_VALUE / (fp128Cnt / 2L);
		
		long high = Long.MAX_VALUE;
		for (int i = 0; i < fp128Cnt -1; i++) {
			DummyFP128 fp = new DummyFP128(0, high);
			fpSet.put(fp);
			high -= frob;
		}
		// Set forceFlush and add one more fp to have the memory flushed to disk
		fpSet.forceFlush();
		DummyFP128 fp = new DummyFP128(0, high);
		fpSet.put(fp);
		
		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		high = Long.MAX_VALUE;
		for (long l = 0; l < fp128Cnt; l++) {
			assertTrue(fpSet.contains(new DummyFP128(0, high)));
			high -= frob;
		}
		// Check that we have found all elements and that those are _all_
		// elements in the set
		assertEquals(fp128Cnt, fpSet.size());
	}
	
	/**
	 * Test method for {@link tlc2.tool.fp.fp128.OffHeapDiskFPSet#put(tlc2.util.FP128)}.
	 * @throws IOException 
	 */
	public void testSimple2() throws IOException {
		final int fp128Cnt = 1024;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestSimple2.fp");

		// Spread the fingerprints evenly across fingerprint space
		final long frob = Long.MAX_VALUE / (fp128Cnt / 8L);
		
		long high = Long.MAX_VALUE;
		int i = 0;
		for (; i < fp128Cnt * 4; i++) {
			DummyFP128 fp = new DummyFP128(0, high);
			fpSet.put(fp);
			high -= frob;
		}
		
		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		high = Long.MAX_VALUE;
		for (long l = 0; l < fp128Cnt * 4; l++) {
			assertTrue(fpSet.contains(new DummyFP128(0, high)));
			high -= frob;
		}
		// Check that we have found all elements and that those are _all_
		// elements in the set
		assertEquals(fp128Cnt * 4, fpSet.size());
	}
	
	public void testHighZero() throws IOException {
		final int fp128Cnt = 1024;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestHighZero.fp");

		// Spread the fingerprints evenly across fingerprint space
		final long frob = Long.MAX_VALUE / (fp128Cnt / 2L);
		
		long lower = Long.MAX_VALUE;
		int i = 0;
		for (; i < fp128Cnt; i++) {
			DummyFP128 fp = new DummyFP128(lower, 0);
			fpSet.put(fp);
			lower -= frob;
		}
		
		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		lower = Long.MAX_VALUE;
		for (long l = 0; l < fp128Cnt; l++) {
			assertTrue(fpSet.contains(new DummyFP128(lower, 0)));
			lower -= frob;
		}
		// Check that we have found all elements and that those are _all_
		// elements in the set
		assertEquals(fp128Cnt, fpSet.size());
	}
	
	public void testUnevenSize() throws IOException {
		final int fp128Cnt = 1023;
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new ExplicitFPSetConfiguration(fp128Cnt));
		fpSet.init(1, System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "TestUnevenSize.fp");

		// Spread the fingerprints evenly across fingerprint space
		final long frob = Long.MAX_VALUE / (fp128Cnt / 2L);
		
		long lower = Long.MAX_VALUE;
		int i = 0;
		for (; i < fp128Cnt; i++) {
			DummyFP128 fp = new DummyFP128(lower, 0);
			fpSet.put(fp);
			lower -= frob;
		}
		
		// Verify all FP128 are correctly stored in fpSet. This is possible
		// since both rnds share the same seed.
		lower = Long.MAX_VALUE;
		for (long l = 0; l < fp128Cnt; l++) {
			assertTrue(fpSet.contains(new DummyFP128(lower, 0)));
			lower -= frob;
		}
		// Check that we have found all elements and that those are _all_
		// elements in the set
		assertEquals(fp128Cnt, fpSet.size());
	}
	
	public void testGetLast() throws IOException {
		final OffHeapDiskFPSet fpSet = new OffHeapDiskFPSet(new FPSetConfiguration(1.0d));

		final String filename = System.currentTimeMillis() + "TestGetLast.fp";
		final String metaDir = System.getProperty("java.io.tmpdir");
		fpSet.init(1, metaDir, filename);
		
		// Cause the subsequent put to immediately flush to disk
		fpSet.forceFlush();
		
		// Add a single fp which is going to be assigned to the highest hash bucket
		final FP128 fp = new DummyFP128(Long.MAX_VALUE, Long.MAX_VALUE);
		assertFalse(fpSet.put(fp));
		
		assertTrue(fpSet.contains(fp));
		
		final BufferedRandomAccessFile raf = new BufferedRandomAccessFile(new File(
				metaDir + File.separator + filename + ".fp"), "r");
		try {
			FP128 fingerprint = (FP128) FP128.Factory.getInstance().newFingerprint(raf);
			assertEquals(fp, fingerprint);
		} finally {
			raf.close();
		}
	}
	
	@SuppressWarnings("serial")
	private class ExplicitFPSetConfiguration extends FPSetConfiguration {

		public ExplicitFPSetConfiguration(int fp128Cnt) {
			memoryInBytes = fp128Cnt * FP128.BYTES;
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.FPSetConfiguration#getMemoryInFingerprintCnt()
		 */
		@Override
		public long getMemoryInFingerprintCnt() {
			return memoryInBytes / FP128.BYTES;
		}
	}
}
