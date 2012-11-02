// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import java.io.IOException;

import junit.framework.TestCase;
import tlc2.util.FP128;

public class FP128BloomFilterFPSetTest extends TestCase {

	
	/**
	 * Test method for {@link tlc2.tool.fp.FP128BloomFilterFPSet#put(tlc2.util.Fingerprint)}.
	 * @throws IOException 
	 */
	public void testPutFingerprint() throws IOException {
		final FP128BloomFilterFPSet fpSet = new FP128BloomFilterFPSet(new FPSetConfiguration());

		FP128 fp128 = new DummyFP128(1L, 1L);
		assertFalse(fpSet.contains(fp128));
		assertFalse(fpSet.put(fp128));
		assertTrue(fpSet.contains(fp128));
		assertTrue(fpSet.put(fp128));
		assertTrue(fpSet.put(fp128));
		
		fp128 = new DummyFP128(2L, 2L);
		assertFalse(fpSet.contains(fp128));
		assertFalse(fpSet.put(fp128));

		fp128 = new DummyFP128(3L, 3L);
		assertFalse(fpSet.contains(fp128));
		assertFalse(fpSet.put(fp128));
	}

	/**
	 * Test method for {@link tlc2.tool.fp.FP128BloomFilterFPSet#contains(tlc2.util.Fingerprint)}.
	 */
	public void testContainsFingerprint() {
		fail("Not yet implemented");
	}
}
