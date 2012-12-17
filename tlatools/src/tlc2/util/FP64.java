// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Last modified on Tue May 25 23:22:20 PDT 1999 by yuanyu
package tlc2.util;

import java.io.IOException;
import java.util.logging.Level;

import sun.misc.Unsafe;
import tlc2.tool.fp.FPSetConfiguration;

/**
 * A 64-bit fingerprint is stored in an instance of the type <code>long</code>.
 * The static methods of <code>FP64</code> are used to initialize 64-bit
 * fingerprints and to extend them.
 * 
 * Written by Allan Heydon and Marc Najork.
 */
@SuppressWarnings({ "restriction", "serial" })
public class FP64 extends GFFingerprint {

	public static class Factory extends FPFactory {
		public Factory(FPSetConfiguration fpConfig) {
			int fpIndex = fpConfig.getFPIndex();
			FP64.Init(fpIndex);
			LOGGER.log(Level.FINEST, "Instantiated FP128 factory with index {0}", fpIndex);
		}

		/* (non-Javadoc)
		 * @see tlc2.util.Fingerprint.FPFactory#newFingerprint()
		 */
		public Fingerprint newFingerprint() {
			return new FP64();
		}

		public Fingerprint newFingerprint(sun.misc.Unsafe unsafe, long posLower) {
			long lower = unsafe.getAddress(posLower);
			if (lower == 0L) {
				return null;
			} else {
				return new FP64(lower);
			}
		}
		
		/* (non-Javadoc)
		 * @see tlc2.util.Fingerprint.FPFactory#newFingerprint(tlc2.util.BufferedRandomAccessFile)
		 */
		public Fingerprint newFingerprint(java.io.RandomAccessFile raf) throws IOException {
			return new FP64().read(raf);
		}
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(java.lang.String)
	 */
	public Fingerprint extend(String s) {
		
		final int mask = 0xFF;
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			IrredPoly = ((IrredPoly >>> 8) ^ (ByteModTable_7[(((int) c) ^ ((int) IrredPoly))
					& mask]));
		}
		
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(byte[])
	 */
	public Fingerprint extend(byte[] bytes) {
		return extend(bytes, 0, bytes.length);
	}
	
	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(byte[], int, int)
	 */
	public Fingerprint extend(byte[] bytes, int start, int len) {

		int end = start + len;
		for (int i = start; i < end; i++) {
			IrredPoly = (IrredPoly >>> 8) ^ ByteModTable_7[(bytes[i] ^ (int) IrredPoly) & 0xFF];
		}
		
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(char)
	 */
	public Fingerprint extend(char c) {
			
		IrredPoly = ((IrredPoly >>> 8) ^ (ByteModTable_7[(((int) c) ^ ((int) IrredPoly)) & 0xFF]));
	
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(byte)
	 */
	public Fingerprint extend(byte b) {
			
		IrredPoly = ((IrredPoly >>> 8) ^ (ByteModTable_7[(b ^ ((int) IrredPoly)) & 0xFF]));
		
		return this;
	}

	/*
	 * Extend the fingerprint <code>fp</code> by an integer <code>x</code>.
	 */
	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(int)
	 */
	public FP64 extend(int x) {
			
		for (int i = 0; i < 4; i++) {
			byte b = (byte) (x & 0xFF);
			IrredPoly = ((IrredPoly >>> 8) ^ (ByteModTable_7[(b ^ ((int) IrredPoly)) & 0xFF]));
			x = x >>> 8;
		}

		return this;
	}
	
	/**
	 * 8 Bytes are what is needed to store a 64bit fingerprint (impressive, huh?) 
	 */
	public static final int BYTES = (Long.SIZE) / Byte.SIZE;
	
	/*
	 * This is the table used for computing fingerprints. The ByteModTable could
	 * be hardwired. Note that since we just extend a byte at a time, we need
	 * just "ByteModeTable[7]".
	 */
	private static long[] ByteModTable_7;

	private static int index;
	
	// Initialization code
	private static void Init(int n) {
		index = n;
		
		ByteModTable_7 = getByteModTable(Polys[index]);
	}
	
	/* This is the irreducible polynomial used as seeds */
	protected long IrredPoly;
	
	protected FP64() {
		IrredPoly = Polys[index];
	}
	
	private FP64(long l) {
		IrredPoly = l;
	}

	public long[] getIrredPoly() {
		return new long[] {IrredPoly};
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FP128 " + (isOnDisk() == true ? "(disk) " : "") + "[IrredPolyLower=" + IrredPoly + " (" +Long.toBinaryString(IrredPoly) + ")]";
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#getIndex(long)
	 */
	public int getIndex(final long mask) {
		return (int) (IrredPoly & mask);
	}

	private Fingerprint read(final java.io.RandomAccessFile raf) throws IOException {
		IrredPoly = raf.readLong();
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#write(tlc2.util.BufferedRandomAccessFile)
	 */
	public void write(final java.io.RandomAccessFile raf) throws IOException {
		raf.writeLong(IrredPoly);
	}

	public void write(Unsafe u, long posLower) {
		u.putAddress(posLower, IrredPoly);
	}
	
	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#longValue()
	 */
	public long longValue() {
		return IrredPoly;
	}

	public boolean isOnDisk()  {
		if ((IrredPoly & 0x8000000000000000L) < 0) {
			return true;
		}
		return false;
	}

	public void setIsOnDisk() {
		// set msb to 1 to indicate fp is on disk
		this.IrredPoly = IrredPoly | 0x8000000000000000L;
	}

	public FP64 zeroMSB() {
		IrredPoly = IrredPoly & 0x7FFFFFFFFFFFFFFFL;
		return this;
	}
	
	public long getLower() {
		return IrredPoly;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (IrredPoly ^ (IrredPoly >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FP64))
			return false;
		FP64 other = (FP64) obj;
		if (IrredPoly != other.IrredPoly)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final Fingerprint other) {
		if (other instanceof FP64) {
			FP64 fp = (FP64) other;
			// zero msb of higher part which is 1 or 0 depending on disk state
			return /* Long. */compare(
					IrredPoly & 0x7FFFFFFFFFFFFFFFL,
					fp.IrredPoly & 0x7FFFFFFFFFFFFFFFL);
		}
		throw new IllegalArgumentException();
	}
	
	/**
	 * @see Long#compare(long, long) in Java 1.7
	 */
	private int compare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	/**
	 * @param a
	 * @param b
	 * @return The maximum of both fingerprints.
	 */
	public static FP64 max(final FP64 a, final FP64 b) {
		if (a.compareTo(b) < 0) {
			return b;
		} else {
			return a;
		}
	}
}
