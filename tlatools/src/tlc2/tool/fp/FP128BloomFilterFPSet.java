// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import tlc2.util.BitVector;
import tlc2.util.FP128;
import tlc2.util.Fingerprint;

public class FP128BloomFilterFPSet extends FPSet {

	private final BitSet bitSet;
	private final ReentrantReadWriteLock lock;
	private final int m;
	private final int k;
	private long size;
	
	protected FP128BloomFilterFPSet(FPSetConfiguration fpSetConfig) throws RemoteException {
		super(fpSetConfig);
		k = 1;
		m = (1 << 31) - 1; // mersenne prime
		bitSet = new BitSet(m); // ~270mb
		lock = new ReentrantReadWriteLock();
		size = 0L;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#init(int, java.lang.String, java.lang.String)
	 */
	@Override
	public void init(int numThreads, String metadir, String filename) throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#put(long)
	 */
	@Override
	public boolean put(long fp) throws IOException {
		throw new UnsupportedOperationException("Not applicable for FP128 FPSet");
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#contains(long)
	 */
	@Override
	public boolean contains(long fp) throws IOException {
		throw new UnsupportedOperationException("Not applicable for FP128 FPSet");
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#put(tlc2.util.Fingerprint)
	 */
	@Override
	public boolean put(final Fingerprint fp) throws IOException {
		final int[] f = getIndices(fp); 

		boolean result = true;

		lock.writeLock().lock();
		try {
			for (int i = 0; i < f.length; i++) {
				int bitIndex = f[i];
				if(!bitSet.get(bitIndex)) {
					bitSet.set(bitIndex);
					result = false;
				}
			}
			if (!result) {
				size++;
			}
		} finally {
			lock.writeLock().unlock();
		}
		return result;
	}

	private int[] getIndices(final Fingerprint fp) {
		final FP128 fp128 = (FP128) fp;

		int x = (int) (fp128.getLower() % m);
		int y = (int) (fp128.getHigher() % m);
		
		final int[] f = new int[k];
		f[0] = x < 0 ? x * -1 : x;
		
		// Enhanced Double Hashing
		for (int i = 1; i < k; i++) {
			x = (x + y) % m;
			y = (y + i) % m;
			f[i] = x < 0 ? x * -1 : x;
		}
		return f;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#contains(tlc2.util.Fingerprint)
	 */
	@Override
	public boolean contains(final Fingerprint fp) throws IOException {
		final int[] f = getIndices(fp);

		lock.writeLock().lock();
		try {
			for (int i = 0; i < f.length; i++) {
				int bitIndex = f[i];
				if(!bitSet.get(bitIndex)) {
					return false;
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#putBlock(java.util.List)
	 */
	@Override
	public BitVector putBlock(List<Fingerprint> fpv) throws IOException {
		final BitVector vec = new BitVector(fpv.size());
		for (int i = 0; i < fpv.size(); i++) {
			vec.set(i, !this.put(fpv.get(i)));
		}
		return vec;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#containsBlock(java.util.List)
	 */
	@Override
	public BitVector containsBlock(List<Fingerprint> fpv) throws IOException {
		final BitVector vec = new BitVector(fpv.size());
		for (int i = 0; i < fpv.size(); i++) {
			vec.set(i, !this.contains(fpv.get(i)));
		}
		return vec;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#exit(boolean)
	 */
	@Override
	public void exit(boolean cleanup) throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#checkFPs()
	 */
	@Override
	public double checkFPs() throws IOException {
		// Not applicable for BloomFilter
		return -1;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#beginChkpt()
	 */
	@Override
	public void beginChkpt() throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#commitChkpt()
	 */
	@Override
	public void commitChkpt() throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recover()
	 */
	@Override
	public void recover() throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recoverFP(long)
	 */
	@Override
	public void recoverFP(long fp) throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#prepareRecovery()
	 */
	@Override
	public void prepareRecovery() throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#completeRecovery()
	 */
	@Override
	public void completeRecovery() throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#beginChkpt(java.lang.String)
	 */
	@Override
	public void beginChkpt(String filename) throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#commitChkpt(java.lang.String)
	 */
	@Override
	public void commitChkpt(String filename) throws IOException {
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#recover(java.lang.String)
	 */
	@Override
	public void recover(String filename) throws IOException {
	}
}
