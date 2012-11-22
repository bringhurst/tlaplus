// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.LongBuffer;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import sun.misc.Unsafe;
import tlc2.output.EC;
import tlc2.output.MP;
import tlc2.tool.fp.FPSet;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.tool.fp.FPSetStatistic;
import tlc2.tool.fp.LSBDiskFPSet;
import tlc2.tool.fp.MSBDiskFPSet;
import tlc2.tool.fp.management.DiskFPSetMXWrapper;
import tlc2.util.FP128;
import tlc2.util.FP128.Factory;
import util.Assert;

@SuppressWarnings({ "serial", "restriction" })
public class OffHeapDiskFPSet extends FP128DiskFPSet implements FPSetStatistic {
	
	protected static final double COLLISION_BUCKET_RATIO = .025d;

	private static sun.misc.Unsafe getUnsafe() {
		try {
			final Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (sun.misc.Unsafe) f.get(null);
		} catch (Exception e) {
			throw new RuntimeException(
					"Trying to use Sun VM specific sun.misc.Unsafe implementation but no Sun based VM detected.",
					e);
		}
	}
	
	protected final int bucketCapacity;

	/**
	 * This implementation uses sun.misc.Unsafe instead of a wrapping
	 * java.nio.ByteBuffer due to the fact that the former's allocateMemory
	 * takes a long argument, while the latter is restricted to
	 * Integer.MAX_VALUE as its capacity.<br>
	 * In 2012 this poses a too hard limit on the usable memory, hence we trade
	 * generality for performance.
	 */
	private final Unsafe u;
	
	/**
	 * The base address allocated for fingerprints
	 */
	private final long baseAddress;
	
	/**
	 * Address size (either 4 or 8 bytes) depending on current architecture
	 */
	private final int logAddressSize;

	/**
	 * A bucket containing collision elements which is used as a fall-back if a
	 * bucket is fully used up. Buckets cannot grow as the whole in-memory
	 * data-structure is static and not designed to be resized.
	 * 
	 * <p>
	 * Open addressing - contrary to separate chaining - is not an option for an
	 * {@link OffHeapDiskFPSetTest}, because it does not support the invariant of
	 * monotonic increasing buckets required by the {@link Indexer}. Adhering to
	 * this invariant has the benefit, that only the elements in a bucket have
	 * to be sorted, but they don't change buckets during sort. Thus, a
	 * temporary sort array as in {@link LSBDiskFPSet.LSBFlusher#prepareTable()} is
	 * obsolete halving the memory footprint.
	 * </p>
	 */
	protected CollisionBucket collisionBucket;
	
	/**
	 * The indexer maps a fingerprint to a in-memory bucket and the associated lock
	 */
	private final Indexer indexer;
	
	private final ReadWriteLock csRWLock = new ReentrantReadWriteLock();

	public OffHeapDiskFPSet(final FPSetConfiguration fpSetConfig) throws RemoteException {
		super(fpSetConfig);
		
		final long memoryInFingerprintCnt = fpSetConfig.getMemoryInFingerprintCnt();
		
		// Determine base address which varies depending on machine architecture.
		u = getUnsafe();
		int addressSize = u.addressSize();
		int cnt = -1;
		while (addressSize > 0) {
			cnt++;
			addressSize = addressSize >>> 1; // == (n/2)
		}
		logAddressSize = cnt;

		// Allocate non-heap memory for maxInMemoryCapacity fingerprints
		long bytes = memoryInFingerprintCnt << logAddressSize;
		
		baseAddress = u.allocateMemory(bytes);
		
		// Null memory (could be done in parallel on segments when bottleneck).
		// This is essential as allocateMemory returns uninitialized memory and
		// memInsert/memLockup utilize 0L as a mark for an unused fingerprint slot.
		// Otherwise memory garbage wouldn't be distinguishable from a true fp. 
		for (long i = 0; i < memoryInFingerprintCnt; i++) {
			u.putAddress(log2phy(i), 0L);
		}

		final int csCapacity = (int) (maxTblCnt * COLLISION_BUCKET_RATIO);
		this.collisionBucket = new TreeSetCollisionBucket(csCapacity);
		
		this.flusher = new OffHeapMSBFlusher();
		
		// Move n as many times to the right to calculate moveBy. moveBy is the
		// number of bits the (fp & mask) has to be right shifted to make the
		// logical bucket index.
		long n = (Long.MAX_VALUE >>> fpSetConfig.getPrefixBits()) - (memoryInFingerprintCnt - 1);
		int moveBy = 0;
		while (n >= memoryInFingerprintCnt) {
			moveBy++;
			n = n >>> 1; // == (n/2)
		}
		
		// Calculate Hamming weight of maxTblCnt
		final int bitCount = Long.bitCount(memoryInFingerprintCnt);
		
		// If Hamming weight is 1, the logical index address can be calculated
		// significantly faster by bit-shifting. However, with large memory
		// sizes, only supporting increments of 2^n sizes would waste memory
		// (e.g. either 32GiB or 64Gib). Hence, we check if the bitCount allows
		// us to use bit-shifting. If not, we fall back to less efficient
		// calculations. Additionally we increase the bucket capacity to make
		// use of extra memory. The down side is, that larger buckets mean
		// increased linear search. But linear search on maximally 31 elements
		// still outperforms disk I/0.
		if (bitCount == 1) {
			bucketCapacity = InitialBucketCapacity;
			this.indexer = new BitshiftingIndexer(moveBy, fpSetConfig.getPrefixBits());
		} else {
			// Round maxInMemoryCapacity to next lower 2^n power
			cnt = -1;
			while (bytes > 0) {
				cnt++;
				bytes = bytes >>> 1;
			}
			
			// Extra memory that cannot be addressed by BitshiftingIndexer
			final long extraMem = (memoryInFingerprintCnt * LongSize) - (long) Math.pow(2, cnt);
			
			// Divide extra memory across addressable buckets
			int x = (int) (extraMem / ((n + 1) / InitialBucketCapacity));
			bucketCapacity = InitialBucketCapacity + (x / LongSize) ;
			// Twice InitialBucketCapacity would mean we could have used one
			// more bit for addressing.
			Assert.check(bucketCapacity < (2 * InitialBucketCapacity), EC.GENERAL);

			// non 2^n buckets cannot use a bit shifting indexer
			this.indexer = new Indexer(moveBy, fpSetConfig.getPrefixBits());
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#sizeof()
	 */
	public long sizeof() {
		long size = 44; // approx size of this DiskFPSet object
		size += maxTblCnt * (long) LongSize;
		size += getIndexCapacity() * 4;
		size += getCollisionBucketCnt() * (long) LongSize; // ignoring the internal TreeSet overhead here
		return size;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#needsDiskFlush()
	 */
	protected boolean needsDiskFlush() {
		// Only flush due to collision ratio when primary hash table is at least
		// 25% full. Otherwise a second flush potentially immediately follows a
		// first one, when both values for tblCnt and collision size can be small.
		return (collisionRatioExceeds(COLLISION_BUCKET_RATIO) && loadFactorExceeds(.25d)) 
				|| loadFactorExceeds(1d) || forceFlush;
	}
	
	/**
	 * This limits the (primary) in-memory hash table to grow beyond the given
	 * limit.
	 * 
	 * @param limit
	 *            A limit in the domain [0, 1] which restricts the hash table
	 *            from growing past it.
	 * @return true iff the current hash table load exceeds the given limit
	 */
	private boolean loadFactorExceeds(final double limit) {
		// Base this one the primary hash table only and exclude the
		// collision bucket
		final double d = (this.tblCnt.doubleValue() - collisionBucket.size()) / (double) this.maxTblCnt;
		return d >= limit;
	}

	/**
	 * @param limit A limit the collsionBucket is not allowed to exceed
	 * @return The proportional size of the collision bucket compared to the
	 *         size of the set.
	 */
	private boolean collisionRatioExceeds(final double limit) {
		// Do not use the thread safe getCollisionRatio here to avoid
		// unnecessary locking. put() calls us holding a memory write locking
		// which also blocks writers to collisionBucket.
		final long size = collisionBucket.size();
		// Subtract size from overall tblCnt as it includes the cs size
		// @see put(long)
		final double d = (double) size / (tblCnt.doubleValue() - size);
		return d >= limit;
	}
	
	protected int getLockIndex(FP128 fp) {
		return this.indexer.getLockIndex(fp);
	}
	
	public final boolean put(FP128 fp) throws IOException {
		fp = checkValid(fp);
		
		final Lock readLock = rwLock.getAt(getLockIndex(fp)).readLock();
		readLock.lock();
		// First, look in in-memory buffer
		if (this.memLookup(fp)) {
			readLock.unlock();
			this.memHitCnt.getAndIncrement();
			return true;
		}
		
		// blocks => wait() if disk is being re-written 
		// (means the current thread returns rwLock monitor)
		// Why not return monitor first and then acquire read lock?
		// => prevent deadlock by acquiring threads in same order? 
		
		// next, look on disk
		boolean diskHit = this.diskLookup(fp);
		
		// In event of disk hit, return
		if (diskHit) {
			readLock.unlock();
			this.diskHitCnt.getAndIncrement();
			return true;
		}
		
		readLock.unlock();
		
		// Another writer could write the same fingerprint here if it gets
		// interleaved. This is no problem though, because memInsert again
		// checks existence for fp to be inserted
		
		final Lock w = rwLock.getAt(getLockIndex(fp)).writeLock();
		w.lock();
		
		// if disk lookup failed, add to memory buffer
		if (this.memInsert(fp)) {
			w.unlock();
			this.memHitCnt.getAndIncrement();
			return true;
		}
		
		// test if buffer is full && block until there are no more readers 
		if (needsDiskFlush() && this.flusherChosen.compareAndSet(false, true)) {
			
			// statistics
			growDiskMark++;
			long timestamp = System.currentTimeMillis();
			
			// acquire _all_ write locks
			rwLock.acquireAllLocks();
			
			// flush memory entries to disk
			flusher.flushTable();
			
			// release _all_ write locks
			rwLock.releaseAllLocks();
			
			// reset forceFlush to false
			forceFlush = false;
			
			// finish writing
			this.flusherChosen.set(false);

			long l = System.currentTimeMillis() - timestamp;
			flushTime += l;
			
			LOGGER.log(Level.FINE, "Flushed disk {0} {1}. tine, in {2} sec", new Object[] {
					((DiskFPSetMXWrapper) diskFPSetMXWrapper).getObjectName(), getGrowDiskMark(), l});
		}
		w.unlock();
		return false;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#memLookup(FP128)
	 */
	protected boolean memLookup(FP128 fp) {
		final long position = indexer.getLogicalPosition(fp);
		
		// Linearly search the logical bucket; null is an invalid fp and marks the
		// end of the allocated bucket
		for (int i = 0; i < bucketCapacity; i+=2) {
			FP128 l = getFP128(position, i);
			if (l == null) {
				break;
			} else if (fp.equals(l)) {
				return true;
			}
		}
		
		return csLookup(fp);
	}
	
	/**
	 * Probes {@link OffHeapDiskFPSetTest#collisionBucket} for the given fingerprint.
	 * @param fp
	 * @return true iff fp is in the collision bucket
	 */
	protected boolean csLookup(FP128 fp) {
		try {
			csRWLock.readLock().lock();
			return collisionBucket.contains(fp);
		} finally {
			csRWLock.readLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#memInsert(FP128)
	 */
	protected boolean memInsert(FP128 fp) {
		final long position = indexer.getLogicalPosition(fp);

		int freePosition = -1;
		// Loop unless end of bucket or free slot found
		for (int i = 0; i < bucketCapacity && freePosition == -1; i+=2) {
			FP128 l = getFP128(position, i);
			if (fp.equals(l)) {
				return true;
			} else if (l == null && freePosition == -1) {
				if (i == 0) {
					tblLoad++;
				}
				// empty or disk written slot found, simply insert at _current_ position
				putFP128(position, i, fp);
				this.tblCnt.getAndIncrement();
				return false;
			} else if (l.isOnDisk() && freePosition == -1) {
				// record free (disk written fp) slot
				freePosition = i;
			}
		}

		// index slot overflow, thus add to collisionBucket of write to free
		// position.
		if (freePosition > -1 && !csLookup(fp)) {
			putFP128(position, freePosition, fp);
			this.tblCnt.getAndIncrement();
			return false;
		} else {
			boolean success = csInsert(fp);
			if (success) {
				this.tblCnt.getAndIncrement();
			}
			return !success;
		}
	}
	
	private static final FP128.Factory instance = (Factory) FP128.Factory.getInstance();

	private FP128 getFP128(long position, int index) {
		return (FP128) instance.newFingerprint(u, log2phy(position, index), log2phy(position, index+1));
	}
	
	private void putFP128(long position, int index, FP128 fp) {
		fp.write(u, log2phy(position, index), log2phy(position, index+1));
	}

	/**
	 * Inserts the given fingerprint into the {@link OffHeapDiskFPSetTest#collisionBucket}.
	 * @param fp
	 * @return true iff fp has been added to the collision bucket
	 */
	protected boolean csInsert(FP128 fp) {
		try {
			csRWLock.writeLock().lock();
			return collisionBucket.add(fp);
		} finally {
			csRWLock.writeLock().unlock();
		}
	}

	/**
	 * Converts from logical bucket index numbers and in-bucket position to a
	 * physical memory address.
	 * 
	 * @param bucketNumber
	 * @param inBucketPosition
	 * @return The physical address of the fp slot
	 */
	private long log2phy(long bucketNumber, long inBucketPosition) {
		return log2phy(bucketNumber + inBucketPosition);
	}
	
	/**
	 * Converts from logical addresses to 
	 * physical memory addresses.
	 * 
	 * @param logicalAddress
	 * @return The physical address of the fp slot
	 */
	private long log2phy(long logicalAddress) {
		return baseAddress + (logicalAddress << logAddressSize);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#getTblCapacity()
	 */
	public long getTblCapacity() {
		return maxTblCnt;
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#getCollisionBucketCnt()
	 */
	public long getCollisionBucketCnt() {
		try {
			this.csRWLock.readLock().lock();
			return collisionBucket.size();
		} finally {
			this.csRWLock.readLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.DiskFPSet#getCollisionRatio()
	 */
	public double getCollisionRatio() {
		return (double) getCollisionBucketCnt() / tblCnt.doubleValue();
	}

	public class Indexer {
		protected final long prefixMask;
		/**
		 * Number of bits to right shift bits during index calculation
		 * @see MSBDiskFPSet#moveBy
		 */
		protected final int moveBy;
		/**
		 * Number of bits to right shift bits during index calculation of
		 * striped lock.
		 */
		protected final int lockMoveBy;

		public Indexer(final int moveBy, int prefixBits) {
			// same for lockCnt
			this.prefixMask = 0x7FFFFFFFFFFFFFFFL >>> prefixBits;
			this.moveBy = moveBy;
			this.lockMoveBy = 63 - prefixBits - LogLockCnt;
		}
		
		public int getLockIndex(final FP128 fp) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getLogicalPosition(final FP128 fp) {
			long position = (fp.getHigher() & prefixMask) >> moveBy;
			position = floorToBucket(position);
			Assert.check(0 <= position && position < maxTblCnt, EC.GENERAL);
			return position;
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.DiskFPSet#getLockIndex(long)
		 */
		protected int getLockIndex(long fp) {
			// calculate hash value (just n most significant bits of fp) which is
			// used as an index address
			final long idx = (fp & prefixMask) >> lockMoveBy;
			Assert.check(0 <= idx && idx < lockCnt, EC.GENERAL);
			return (int) idx;
		}

		/**
		 * @param fp
		 * @return The logical bucket position in the table for the given fingerprint.
		 */
		protected long getLogicalPosition(final long fp) {
			// push MSBs for moveBy positions to the right and align with a bucket address
			long position = (fp & prefixMask) >> moveBy;
			position = floorToBucket(position);
			Assert.check(0 <= position && position < maxTblCnt, EC.GENERAL);
			return position;
		}

		public long getNextBucketBasePosition(long logicalPosition) {
			return floorToBucket(logicalPosition + bucketCapacity);
		}
		
		/**
		 * Returns the largest position that
		 * is less than or equal to the argument and is equal to bucket base address.
		 * 
		 * @param logicalPosition
		 * @return
		 */
		private long floorToBucket(long logicalPosition) {
			long d = (long) Math.floor(logicalPosition / bucketCapacity);
			return bucketCapacity * d;
		}

		/**
		 * @param logicalPosition
		 * @return true iff logicalPosition is a multiple of bucketCapacity
		 */
		public boolean isBucketBasePosition(long logicalPosition) {
			return logicalPosition % bucketCapacity == 0;
		}
	}
	
	/**
	 * A {@link BitshiftingIndexer} uses the more efficient AND operation
	 * compared to MODULO and DIV used by {@link Indexer}. Since indexing is
	 * executed on every {@link FPSet#put(long)} or {@link FPSet#contains(long)}
	 * , it is worthwhile to minimize is execution overhead.
	 */
	public class BitshiftingIndexer extends Indexer {
		
		/**
		 * Mask used to round of to a bucket address which is a power of 2.
		 */
		protected final long bucketBaseIdx;

		public BitshiftingIndexer(final int moveBy, final int prefixBits) throws RemoteException {
			super(moveBy, prefixBits);
			this.bucketBaseIdx = 0x7FFFFFFFFFFFFFFFL - (bucketCapacity - 1);
		}
		
		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.Indexer#getLogicalPosition(long)
		 */
		@Override
		protected long getLogicalPosition(final long fp) {
			// push MSBs for moveBy positions to the right and align with a bucket address
			long position = ((fp & prefixMask) >> moveBy)  & bucketBaseIdx; 
			//Assert.check(0 <= position && position < maxTblCnt, EC.GENERAL);
			return position;
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.Indexer#getNextBucketPosition(long)
		 */
		@Override
		public long getNextBucketBasePosition(long logicalPosition) {
			return (logicalPosition + bucketCapacity) & bucketBaseIdx;
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.Indexer#isBucketBase(long)
		 */
		@Override
		public boolean isBucketBasePosition(long logicalPosition) {
			return (logicalPosition & (InitialBucketCapacity - 1)) == 0;
		}
	}
	
	public class OffHeapMSBFlusher extends Flusher {
		
		/* (non-Javadoc)
		 * @see tlc2.tool.fp.DiskFPSet.Flusher#flushTable()
		 */
		@Override
		protected void flushTable() throws IOException {
			super.flushTable();
			
			// garbage old values in collision bucket
			collisionBucket.clear();
		}
		
		/* (non-Javadoc)
		 * @see tlc2.tool.fp.MSBDiskFPSet#mergeNewEntries(java.io.RandomAccessFile, java.io.RandomAccessFile)
		 */
		@Override
		protected void mergeNewEntries(RandomAccessFile inRAF, RandomAccessFile outRAF) throws IOException {
			final long buffLen = tblCnt.get();
			ByteBufferIterator itr = new ByteBufferIterator(u, baseAddress, collisionBucket, buffLen);

			// Precompute the maximum value of the new file
			FP128 maxVal = itr.getLast();
			if (index != null) {
				maxVal = FP128.max(maxVal, index[index.length - 1]);
			}

			int indexLen = calculateIndexLen(buffLen);
			index = new FP128[indexLen];
			index[indexLen - 1] = maxVal;
			currIndex = 0;
			counter = 0;

			// initialize positions in "buff" and "inRAF"
			FP128 value = null; // initialize only to make compiler happy
			boolean eof = false;
			if (fileCnt > 0) {
				try {
					value = (FP128) FP128.Factory.getInstance().newFingerprint(inRAF);
				} catch (EOFException e) {
					eof = true;
				}
			} else {
				eof = true;
			}

			// merge while both lists still have elements remaining
			boolean eol = false;
			FP128 fp = itr.next();
			while (!eof || !eol) {
				if ((value.compareTo(fp) == -1 || eol) && !eof) {
					writeFP(outRAF, value);
					try {
						value = (FP128) FP128.Factory.getInstance().newFingerprint(inRAF);
					} catch (EOFException e) {
						eof = true;
					}
				} else {
					// prevent converting every long to String when assertion holds (this is expensive)
					if (value == fp) {
						//MAK: Commented cause a duplicate does not pose a risk for correctness.
						// It merely indicates a bug somewhere.
						//Assert.check(false, EC.TLC_FP_VALUE_ALREADY_ON_DISK,
						//		String.valueOf(value));
						MP.printWarning(EC.TLC_FP_VALUE_ALREADY_ON_DISK, String.valueOf(value));
					}
					writeFP(outRAF, fp);
					// we used one fp up, thus move to next one
					try {
						fp = itr.next();
					} catch (NoSuchElementException e) {
						// has read all elements?
						Assert.check(!itr.hasNext(), EC.GENERAL);
						eol = true;
					}
				}
			}
			
			// both sets used up completely
			Assert.check(eof && eol, EC.GENERAL);

			// currIndex is amount of disk writes
			Assert.check(currIndex == indexLen - 1, EC.SYSTEM_INDEX_ERROR);

			// maintain object invariants
			fileCnt += buffLen;
		}
	}
	
	/**
	 * A non-thread safe Iterator 
	 */
	public class ByteBufferIterator {

		private final CollisionBucket cs;
		/**
		 * Number of elements in the buffer
		 */
		private long bufferElements;
		/**
		 * Total amount of elements in both the buffer as well as the collisionBucket. 
		 */
		private final long totalElements;
		/**
		 * The logical position is the position inside the {@link LongBuffer} and
		 * thus reflects a fingerprints
		 */
		private long logicalPosition = 0;
		/**
		 * Used to verify that the elements we hand out are strictly monotonic
		 * increasing.
		 */
		private FP128 previous = null;
		/**
		 * Number of elements read with next()
		 */
		private long readElements = 0L;

		private FP128 cache = null;

		public ByteBufferIterator(Unsafe u, long baseAddress, CollisionBucket collisionBucket, long expectedElements) {
			this.logicalPosition = 0L;
			this.totalElements = expectedElements;
			
			// Do calculation before prepareForFlush() potentially empties the cs causing size() to return 0 
			this.bufferElements = expectedElements - collisionBucket.size();
			
			this.cs = collisionBucket;
			this.cs.prepareForFlush();
		}

	    /**
	     * Returns the next element in the iteration.
	     *
	     * @return the next element in the iteration.
	     * @exception NoSuchElementException iteration has no more elements.
	     */
		public FP128 next() {
			FP128 result = null;

			if (cache == null && bufferElements > 0) {
				result = getNextFromBuffer();
				bufferElements--;
			} else {
				result = cache;
				cache = null;
			}

			if (!cs.isEmpty()) {
				FP128 first = cs.first();
				if (result.compareTo(first) == 1 || result == null) {
					cs.remove(first);
					cache = result;
					result = first;
				}
			}
			
			// adhere to the general Iterator contract to fail fast and not hand out
			// meaningless values
			if (result == null) {
				throw new NoSuchElementException();
			}
			
			// hand out strictly monotonic increasing elements
			Assert.check(previous.compareTo(result) == -1, EC.GENERAL);
			previous = result;
			
			// maintain read statistics
			readElements++;
			
			return result;
		}

		private FP128 getNextFromBuffer() {
			sortNextBucket();
			
			FP128 l = getFP128(logicalPosition, 0);
			if (!l.isOnDisk()) {
				putFP128(logicalPosition++, 0, l); //TODO l | 0x8000000000000000L??
//				unsafe.putAddress(log2phy(logicalPosition++), l | 0x8000000000000000L);
				return l;
			}
			
			while (((l = getFP128(logicalPosition, 0)) == null || l.isOnDisk() == true) && logicalPosition < maxTblCnt) {
				// increment position to next bucket
				logicalPosition = indexer.getNextBucketBasePosition(logicalPosition);
				sortNextBucket();
			}
			
			if (!l.isOnDisk()) {
				putFP128(logicalPosition++, 0, l); //TODO l | 0x8000000000000000L??
//				unsafe.putAddress(log2phy(logicalPosition++), l | 0x8000000000000000L);
				return l;
			}
			throw new NoSuchElementException();
		}

		// sort the current logical bucket if we reach the first slot of the
		// bucket
		private void sortNextBucket() {
			if (indexer.isBucketBasePosition(logicalPosition)) {
				FP128[] buffer = new FP128[bucketCapacity / 2];
				int k = 0;
				for (int i = 0; i < bucketCapacity; i+=2) {
					FP128 l = getFP128(logicalPosition + i, 0);
					if (l == null || l.isOnDisk()) {
						break;
					} else {
						buffer[k++] = l;
					}
				}
				if (k > 0) {
					Arrays.sort(buffer, 0, k);
					for (int j = 0; j < k; j++) {
						putFP128(logicalPosition, j, buffer[j]);
					}
				}
			}
		}

	    /**
	     * Returns <tt>true</tt> if the iteration has more elements. (In other
	     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
	     * rather than throwing an exception.)
	     *
	     * @return <tt>true</tt> if the iterator has more elements.
	     */
		public boolean hasNext() {
			// hasNext does not move the indices at all!
			return readElements < totalElements;
		}
		
		/**
		 * @return The last element in the iteration.
	     * @exception NoSuchElementException if iteration is empty.
		 */
		public FP128 getLast() {
			// Remember current position
			final long tmpLogicalPosition = logicalPosition;

			// Calculate last bucket position and have it sorted 
			logicalPosition = maxTblCnt - bucketCapacity;
			sortNextBucket();

			// Reverse the current bucket to obtain last element (More elegantly
			// this could be achieved recursively, but this can cause a
			// stack overflow).
			FP128 l = null;
			while ((l = getFP128(logicalPosition-- + bucketCapacity - 1, 0)) == null || l.isOnDisk()) {
				sortNextBucket();
			}
			
			// Done searching in-memory storage backwards, reset position to
			// original value.
			logicalPosition = tmpLogicalPosition;
			
			// Compare max element found in main in-memory buffer to man
			// element in collisionBucket. Return max of the two.
			if (!cs.isEmpty()) {
				l = FP128.max(cs.last(), l);
			}
			
			// Either return the maximum element or fail fast.
			if (!l.isOnDisk()) {
				return l;
			}
			throw new NoSuchElementException();
		}
	}
	
	public interface CollisionBucket {
		void clear();

		void prepareForFlush();

		void remove(FP128 first);

		FP128 first();
		
		FP128 last();

		boolean isEmpty();

		/**
		 * @param fp
	     * @return {@code true} if this set did not already contain the specified
	     *         fingerprint
		 */
		boolean add(FP128 fp);

		boolean contains(FP128 fp);

		long size();
	}
	
	public class TreeSetCollisionBucket implements CollisionBucket {
		private final TreeSet<FP128> set;

		public TreeSetCollisionBucket(int initialCapacity) {
			this.set = new TreeSet<FP128>();
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#clear()
		 */
		public void clear() {
			set.clear();
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#prepareForFlush()
		 */
		public void prepareForFlush() {
			// no-op
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#remove(long)
		 */
		public void remove(FP128 first) {
			set.remove(first);
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#first()
		 */
		public FP128 first() {
			return set.first();
		}
		
		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#last()
		 */
		public FP128 last() {
			return set.last();
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#isEmpty()
		 */
		public boolean isEmpty() {
			return set.isEmpty();
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#add(long)
		 * 
		 * If this set already contains the element, the call leaves the set
		 * unchanged and returns false.
		 */
		public boolean add(FP128 fp) {
			return set.add(fp);
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#contains(long)
		 */
		public boolean contains(FP128 fp) {
			return set.contains(fp);
		}

		/* (non-Javadoc)
		 * @see tlc2.tool.fp.OffHeapDiskFPSet.CollisionBucket#size()
		 */
		public long size() {
			return set.size();
		}
	}
}
