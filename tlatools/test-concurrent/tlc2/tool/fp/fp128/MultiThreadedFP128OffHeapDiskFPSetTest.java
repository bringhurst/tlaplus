// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import tlc2.tool.fp.FPSet;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.tool.fp.MultiThreadedFPSetTest;
import tlc2.tool.fp.fp128.generator.FP128FingerPrintGenerator;

public class MultiThreadedFP128OffHeapDiskFPSetTest extends MultiThreadedFPSetTest {
	/**
	 * Test filling a {@link FPSet} with random fingerprints using multiple
	 * threads in ordered batches
	 */
	public void testMaxFPSetSizeRndBatched() throws IOException, InterruptedException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		fail();
	}
	
	/**
	 * Test filling a {@link FPSet} with random fingerprints using multiple
	 * threads in ordered LongVecs using putBlock/containsBlock
	 */
	public void testMaxFPSetSizeRndBlock() throws IOException, InterruptedException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		fail();
	}
	
	/**
	 * Test filling a {@link FPSet} with max int + 2L random using multiple
	 * threads
	 */
	public void testMaxFPSetSizeRnd() throws IOException, InterruptedException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		doTest(FP128FingerPrintGenerator.class);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.AbstractFPSetTest#getFPSet(long)
	 */
	@Override
	protected FPSet getFPSet(final FPSetConfiguration fpSetConfig) throws IOException {
		return new OffHeapDiskFPSet(new FPSetConfiguration(1.0d));
	}
}
