// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import tlc2.tool.fp.FPSetConfiguration;
import tlc2.tool.fp.NoBackupFP128FPSet;
import tlc2.util.BitVector;
import tlc2.util.FP128;
import tlc2.util.Fingerprint;

@SuppressWarnings("serial")
public class FP128BloomFilterFPSet extends NoBackupFP128FPSet {

	private static final int k = 3;

	private final long[] bitSet;
	private final ReentrantReadWriteLock lock;
	private final long m;
	
	private long size;
	
	protected FP128BloomFilterFPSet(FPSetConfiguration fpSetConfig) throws RemoteException {
		super(fpSetConfig);
		
		m = 4L * ((1 << 31) - 1);
		// wmake sure bitshift can be used as replacement for mod operation
		assert Long.bitCount(m) == 1;
		
		bitSet = new long[bitsToWords(m)];
		lock = new ReentrantReadWriteLock();
		size = 0L;
	}

	/**
	 * @param bits The amount of bits to hold.
	 * @return The number of 64bit longs it takes to hold <code>bits</code> 
	 */
	private int bitsToWords(long bits) {
		return (int) (bits / Long.SIZE);
	}

	private boolean get(long bitIndex) {
		// Index for the corresponding long in bitSet[]
		final int i = (int) (bitIndex >> 6); // div 64

		// Index for the corresponding bit in bitSet[i]
		final int bit = (int) bitIndex & 0x3f; // mod 64
		final long mask = 1L << bit;
		
		return (bitSet[i] & mask) != 0;
	}

	private void set(long bitIndex) {
		// Index for the corresponding long in bitSet[]
		final int i = (int) (bitIndex >> 6); // div 64

		// Index for the corresponding bit in bitSet[i]
		final int bit = (int) bitIndex & 0x3f; // mod 64
		final long mask = 1L << bit;

		bitSet[i] |= mask;
	}
	
	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#put(tlc2.util.Fingerprint)
	 */
	@Override
	public boolean put(final Fingerprint fp) throws IOException {
		final long[] f = getIndices(fp); 

		boolean result = true;

		lock.writeLock().lock();
		try {
			for (int i = 0; i < f.length; i++) {
				long bitIndex = f[i];
				if(!get(bitIndex)) {
					set(bitIndex);
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

	private long[] getIndices(final Fingerprint fp) {
		final FP128 fp128 = (FP128) fp;

		long x = fp128.getLower() % m;
		long y = fp128.getHigher() % m;
		
		final long[] f = new long[k];
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
		final long[] f = getIndices(fp);

		lock.writeLock().lock();
		try {
			for (int i = 0; i < f.length; i++) {
				long bitIndex = f[i];
				if(!get(bitIndex)) {
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
	 * @see tlc2.tool.fp.FPSet#checkFPs()
	 */
	@Override
	public double checkFPs() throws IOException {
		// Intuitively, the higher the 1bit count in bitSet is, the more likely is a collision.
		long occupied = 0L;
		for(long l = 0L; l < m; l++) {
			occupied += get(l) ? 1 : 0;
		}
		return occupied / m;
	}
}
