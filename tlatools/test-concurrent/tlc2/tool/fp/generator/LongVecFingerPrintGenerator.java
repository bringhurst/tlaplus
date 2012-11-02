// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSet;
import tlc2.tool.fp.MultiThreadedFPSetTest;
import tlc2.util.BitVector;
import tlc2.util.Fingerprint;

public class LongVecFingerPrintGenerator extends FingerPrintGenerator {

	private static final int batch = 1024;
	
	public LongVecFingerPrintGenerator(MultiThreadedFPSetTest test, int id, FPSet fpSet, CountDownLatch latch, long seed, long insertions) {
		super(test, id, fpSet, latch, seed, insertions);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		List<Fingerprint> predecessors = new ArrayList<Fingerprint>(batch);
		boolean initialized = false;
		while (fpSet.size() < insertions) {
			try {
				// Make sure set still contains predecessors
				if (initialized) {
					final BitVector bitVector = fpSet.containsBlock(predecessors);
					MultiThreadedFPSetTest.assertTrue(bitVector.trueCnt() == batch);
				}

				// Fill new fingerprints and sort them
				for (int i = 0; i < batch; i++) {
					predecessors.add(i, new DummyFP128(rnd.nextLong(), 0L));
				}
				initialized = true;
				
				Fingerprint[] array = predecessors.toArray(new Fingerprint[predecessors.size()]);
				Arrays.sort(array);
				predecessors.clear();
				predecessors.addAll(Arrays.asList(array));

				// Add sorted batch to fpset
				final BitVector bitVector = fpSet.putBlock(predecessors);
				puts += bitVector.trueCnt();
				collisions += (batch - bitVector.trueCnt());

				// First producer prints stats
				if (id == 0) {
					test.printInsertionSpeed(fpSet.size());
				}

			} catch (IOException e) {
				e.printStackTrace();
				MultiThreadedFPSetTest.fail("Unexpected");
			}
		}
		latch.countDown();
	}
}