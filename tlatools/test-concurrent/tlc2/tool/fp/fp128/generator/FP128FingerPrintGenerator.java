// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128.generator;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import tlc2.tool.fp.DummyFP128;
import tlc2.tool.fp.FPSet;
import tlc2.tool.fp.MultiThreadedFPSetTest;
import tlc2.tool.fp.generator.FingerPrintGenerator;

public class FP128FingerPrintGenerator extends FingerPrintGenerator implements Runnable {

	public FP128FingerPrintGenerator(MultiThreadedFPSetTest test, int id,
			FPSet fpSet, CountDownLatch latch, long seed, long insertions) {
		super(test, id, fpSet, latch, seed, insertions);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		DummyFP128 predecessor = null;
		while (fpSet.size() < insertions) {
			try {
				// make sure set still contains predecessor
				if (predecessor != null) {
					MultiThreadedFPSetTest.assertTrue(fpSet.contains(predecessor));
				}

				predecessor = new DummyFP128(rnd.nextLong(), rnd.nextLong());

				boolean put = fpSet.put(predecessor);
				if (put == false) {
					puts++;
				} else {
					collisions++;
				}

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