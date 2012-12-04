// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Last modified on Tue May 25 23:22:20 PDT 1999 by yuanyu
package tlc2.util;

import java.io.IOException;

import sun.misc.Unsafe;

/**
 * A 128-bit fingerprint is stored in an instance of the type <code>long</code>.
 * The static methods of <code>FP64</code> are used to initialize 64-bit
 * fingerprints and to extend them.
 * 
 * Written by Allan Heydon and Marc Najork.
 */
@SuppressWarnings("restriction")
public class FP128 extends Fingerprint {

	public static class Factory extends FPFactory {
		/* (non-Javadoc)
		 * @see tlc2.util.Fingerprint.FPFactory#newFingerprint()
		 */
		public Fingerprint newFingerprint() {
			return new FP128();
		}

		public Fingerprint newFingerprint(sun.misc.Unsafe unsafe, long posLower, long posHigher) {
			long lower = unsafe.getAddress(posLower);
			long higher = unsafe.getAddress(posHigher);
			if (lower == 0L && higher == 0L) {
				return null;
			} else {
				return new FP128(lower, higher);
			}
		}
		
		/* (non-Javadoc)
		 * @see tlc2.util.Fingerprint.FPFactory#newFingerprint(tlc2.util.BufferedRandomAccessFile)
		 */
		public Fingerprint newFingerprint(java.io.RandomAccessFile raf) throws IOException {
			return new FP128().read(raf);
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
			IrredPolyLower = ((IrredPolyLower >>> 8) ^ (ByteModTable_7Lower[(((int) c) ^ ((int) IrredPolyLower))
					& mask]));
			IrredPolyHigher = ((IrredPolyHigher >>> 8) ^ (ByteModTable_7Higher[(((int) c) ^ ((int) IrredPolyHigher))
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
			IrredPolyLower = (IrredPolyLower >>> 8) ^ ByteModTable_7Lower[(bytes[i] ^ (int) IrredPolyLower) & 0xFF];
			IrredPolyHigher = (IrredPolyHigher >>> 8) ^ ByteModTable_7Higher[(bytes[i] ^ (int) IrredPolyHigher) & 0xFF];
		}
		
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(char)
	 */
	public Fingerprint extend(char c) {
			
		// lower 64 bit
		IrredPolyLower = ((IrredPolyLower >>> 8) ^ (ByteModTable_7Lower[(((int) c) ^ ((int) IrredPolyLower)) & 0xFF]));
		// higher 64 bit
		IrredPolyHigher = ((IrredPolyHigher >>> 8) ^ (ByteModTable_7Higher[(((int) c) ^ ((int) IrredPolyHigher)) & 0xFF]));
	
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(byte)
	 */
	public Fingerprint extend(byte b) {
			
		// lower 64 bit
		IrredPolyLower = ((IrredPolyLower >>> 8) ^ (ByteModTable_7Lower[(b ^ ((int) IrredPolyLower)) & 0xFF]));
		// higher 64 bit
		IrredPolyHigher = ((IrredPolyHigher >>> 8) ^ (ByteModTable_7Higher[(b ^ ((int) IrredPolyHigher)) & 0xFF]));
		
		return this;
	}

	/*
	 * Extend the fingerprint <code>fp</code> by an integer <code>x</code>.
	 */
	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#extend(int)
	 */
	public FP128 extend(int x) {
			
		for (int i = 0; i < 4; i++) {
			byte b = (byte) (x & 0xFF);
			IrredPolyLower = ((IrredPolyLower >>> 8) ^ (ByteModTable_7Lower[(b ^ ((int) IrredPolyLower)) & 0xFF]));
			IrredPolyHigher = ((IrredPolyHigher >>> 8) ^ (ByteModTable_7Higher[(b ^ ((int) IrredPolyHigher)) & 0xFF]));
			x = x >>> 8;
		}

		return this;
	}

	/** Unlikely fingerprint? */
	private static final long Zero = 0L;

	/*
	 * This file provides procedures that construct fingerprints of strings of
	 * bytes via operations in GF[2^64]. GF[64] is represented as the set
	 * polynomials of degree 64 with coefficients in Z(2), modulo an irreducible
	 * polynomial P of degree 64. The computer internal representation is a 64
	 * bit long word.
	 * 
	 * Let g(S) be the string obtained from S by prepending the byte 0x80 and
	 * appending eight 0x00 bytes. Let f(S) be the polynomial associated to the
	 * string g(S) viewed as a polynomial with coefficients in the field Z(2).
	 * The fingerprint of S simply the value f(S) modulo P.
	 * 
	 * The irreducible polynomial p used as a modulus is
	 * 
	 * 3 7 11 13 16 19 20 24 26 28 1 + x + x + x + x + x + x + x + x + x + x
	 * 
	 * 29 30 36 37 38 41 42 45 46 48 + x + x + x + x + x + x + x + x + x + x
	 * 
	 * 50 51 52 54 56 57 59 61 62 64 + x + x + x + x + x + x + x + x + x + x
	 * 
	 * IrredPoly is its representation.
	 */

	// implementation constants
	private static final long One = 0x8000000000000000L;
	private static final long X63 = 0x1L;

	private static final long[] Polys = { 0x911498AE0E66BAD6L, 0xda8a0ba66dae0181L, 0xc02f176b8f268d9fL,
			0xd617bb1220fc7812L, 0xc6fd951ad34f9f74L, 0xdd1897bd991704d4L, 0xf5394c541cbfd343L, 0xb1dded37b5c7b8f7L,
			0xb713ff61039dc632L, 0xdfb340cb2fb03d43L, 0xbc3e7e4c5ecb76a3L, 0xdbb4b1349cd7058aL, 0xf53e9dcb9e915cdfL,
			0xca5f58e90dd01848L, 0x80e7ff4406891aa1L, 0xab541bf881fa8571L, 0xbf274e07ac5499d5L, 0x939b1ea933040a4eL,
			0xb791a595448d75b1L, 0x8bf88d6ef85563a2L, 0xecb33ec339513a53L, 0xfa2d3e722db5208aL, 0xb4e2058aac479d24L,
			0xafbd6474e7213b82L, 0x98c1694d14ffaeefL, 0xe188fb5c0a125e24L, 0xfa71cc3865487d80L, 0x891135f7c1c94569L,
			0xcf77cdd16d22e3e6L, 0xeb5e3a1d2e2bb4b5L, 0x92f7f5b69cd00c55L, 0xa9fbbe40ca3b9ae9L, 0x84a7b33d85295bdeL,
			0xebd4680dbb6fdee2L, 0xa31fc46a0583b4d0L, 0xa792c94f15de3e49L, 0xd9d60a9feff4521aL, 0x9227ba31dfdda04bL,
			0xfb4c89c607ce162dL, 0xa89b3b2e01479cc0L, 0xb35a0c2a28b89f7dL, 0x91d0b700b99d9ec2L, 0xf0646bbda05020b0L,
			0xcb5d5f63ce043056L, 0xd276b6a04f42a1b8L, 0xbc1a7a7dbfeb47a3L, 0xe138acff7a963036L, 0xed860223c1557ee7L,
			0x9b2491e980150ab6L, 0xe7e03dd8a5b4e59eL, 0xaaa3f5eac516783cL, 0xfe78cc267a724180L, 0xc22519a21edfac64L,
			0xcdb2941933fec60fL, 0xc5f485551ef38aa1L, 0xa19293d250bd3335L, 0xa4d4c215a50b7afeL, 0xc1155176406a5070L,
			0xecadaa8200e123ecL, 0xbacfe629d58b2f08L, 0xded991082148cb42L, 0x9a0ccbe5deaa88dbL, 0x9a83246f342061bbL,
			0xb71482842297ab05L, 0xf2407eeb997592bdL, 0xb7b43d4b5c4c4bccL, 0xb339a2568221ffe7L, 0xdb4b6b379446ef9eL,
			0xe43c205bb5c0b2b6L, 0xe8e1d141f19d6db0L, 0xd19e8710a4ea1c86L, 0x9704cecfa8d6d07bL, 0xb0a35716162c3f26L,
			0xa2a68c0cf56390acL, 0xe4a74bc601c95b46L, 0xe668fff675595e56L, 0xe0ea77ebe06fabbbL, 0xa8bb94f585279523L,
			0xbeb667b42b684f21L, 0xd7b65410189e28afL, 0x85722037beffc5b9L, 0xe7e7c5f773426204L, 0xa5fd0cc8e060c6f4L,
			0xb8f91ee9065dcf95L, 0xb4047008040f8b50L, 0xecb9ab6291c8cfccL, 0xe08bb9b70caad6dfL, 0xd6e086a301d95d56L,
			0xf6a808f5f3fb9da3L, 0xfa74b8a8ef86fbc5L, 0xa0b6b33ba9e6381cL, 0x8c78703427873dbbL, 0xc5516ea423011021L,
			0xb075cce8528ae7e2L, 0x92e4a37979e2b13bL, 0xafc9ab000ed81026L, 0xf1873f2a861a518aL, 0xf885d6b35770192cL,
			0xd2a82f27f71f5f7eL, 0xd5e7a4dd8beb2d9fL, 0xbab9e7e65dc23e0fL, 0xd5fc877c0bdf5b85L, 0xab428169b9f31c02L,
			0xb7b1351f9266d3eaL, 0xfad564914328f635L, 0x9623ecb1000db9bdL, 0x88d371ec6644c892L, 0xd0a71270573e271cL,
			0xbd200d763f9d81b4L, 0x8484fd96374bf2eaL, 0xe0d0810749432294L, 0x8ebe9dadc88658baL, 0xf6268c58993ae542L,
			0xd6cc88fed0d359c9L, 0xbcaddb8d40a16690L, 0x92817c6db421cbf6L, 0xac63721120a371b5L, 0xd6cc137fdced0820L,
			0xf1c5fbaebb617bd7L, 0xac35d78c765237a5L, 0xbd0a471fa9a23116L, 0x943a7031b946a5aeL, 0xe4e83520cb1aaebbL,
			0xe92a4b73246dd124L, 0xafc1e070787c4c86L, 0xdfe84d42cf06286bL, 0x8b29ec962e4b964bL, 0x807eb5de812ede0fL,
			0xa3cd71299c8b3bfdL, 0x845b8031ef886f35L, 0x91f5a5fa9c5515a5L };

	public static final int numPolys = Polys.length;
	
	/*
	 * This is the table used for computing fingerprints. The ByteModTable could
	 * be hardwired. Note that since we just extend a byte at a time, we need
	 * just "ByteModeTable[7]".
	 */
	private static long[] ByteModTable_7Lower;
	private static long[] ByteModTable_7Higher;

	public static int indexLower;
	private static int indexHigher;
	
	// Initialization code
	public static void Init(int n) {
		indexLower = n;
		indexHigher = indexLower + 1 % numPolys;
		
		ByteModTable_7Lower = getByteModTable(Polys[indexLower]);
		ByteModTable_7Higher = getByteModTable(Polys[indexHigher]);
	}

	public static void Init(long[] polys) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private static long[] getByteModTable(long polynominal) {
		// Maximum power needed == 127-7*8 == 127 - 56 == 71
		int plength = 72;
		long[] PowerTable = new long[plength];

		long t = One;
		for (int i = 0; i < plength; i++) {
			PowerTable[i] = t;
			// System.out.println("pow[" + i + "] = " + Long.toHexString(t));

			// t = t * x
			long mask = ((t & X63) != 0) ? polynominal : 0;
			t = (t >>> 1) ^ mask;
		}

		// Just need the 7th iteration of the ByteModTable initialization code
		long[] byteModTbl = new long[256];
		for (int j = 0; j <= 255; j++) {
			long v = Zero;
			for (int k = 0; k <= 7; k++) {
				if ((j & (1L << k)) != 0) {
					v ^= PowerTable[127 - (7 * 8) - k];
				}
			}
			byteModTbl[j] = v;
		}
		return byteModTbl;
	}
	
	/* These are the irreducible polynomials used as seeds => 128bit */
	protected long IrredPolyLower;
	protected long IrredPolyHigher;
	
	protected FP128() {
		IrredPolyLower = Polys[indexLower];
		IrredPolyHigher = Polys[indexHigher];
	}
	
	private FP128(long lower, long higher) {
		IrredPolyLower = lower;
		IrredPolyHigher = higher;
	}

	public long[] getIrredPoly() {
		return new long[] {IrredPolyLower, IrredPolyHigher};
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (IrredPolyHigher ^ (IrredPolyHigher >>> 32));
		result = prime * result + (int) (IrredPolyLower ^ (IrredPolyLower >>> 32));
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
		if (!(obj instanceof FP128))
			return false;
		FP128 other = (FP128) obj;
		if (IrredPolyHigher != other.IrredPolyHigher)
			return false;
		if (IrredPolyLower != other.IrredPolyLower)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final Fingerprint other) {
		if (other instanceof FP128) {
			FP128 fp = (FP128) other;
			int compareTo = Long.valueOf(IrredPolyLower).compareTo(fp.IrredPolyLower);
			if (compareTo != 0) {
				return compareTo;
			} else {
				return Long.valueOf(IrredPolyHigher).compareTo(fp.IrredPolyHigher);
			}
		} 
		throw new IllegalArgumentException(); 
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FP128 [IrredPolyLower=" + IrredPolyLower + ", IrredPolyHigher="
				+ IrredPolyHigher + "]";
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#getIndex(long)
	 */
	public int getIndex(final long mask) {
		return (int) ((IrredPolyLower ^ IrredPolyHigher) & mask);
	}

	private Fingerprint read(final java.io.RandomAccessFile raf) throws IOException {
		IrredPolyLower = raf.readLong();
		IrredPolyHigher = raf.readLong();
		onDisk = true;
		return this;
	}

	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#write(tlc2.util.BufferedRandomAccessFile)
	 */
	public void write(final java.io.RandomAccessFile raf) throws IOException {
		raf.writeLong(IrredPolyLower);
		raf.writeLong(IrredPolyHigher);
	}

	public void write(Unsafe u, long posLower, long posHigher) {
		u.putAddress(posLower, IrredPolyLower);
		u.putAddress(posHigher, IrredPolyHigher);
	}
	
	/* (non-Javadoc)
	 * @see tlc2.util.Fingerprint#longValue()
	 */
	public long longValue() {
		return IrredPolyLower ^ IrredPolyHigher;
//		throw new UnsupportedOperationException("Not applicable for 128bit fingerprints");
	}
	
	//TDOO implement once flushing to disk is supported
	private boolean onDisk = false;

	public boolean isOnDisk()  {
		// onDisk state could be saved in OffHeapDiskFPSet. The upper n bits
		// used for indexing into the hash table can easily be recovered from
		// the index in which the fingerprint is found.
		return onDisk;
	}

	public void setIsOnDisk() {
		this.onDisk = true;
	}
	
	public long getLower() {
		return IrredPolyLower;
	}

	public long getHigher() {
		return IrredPolyHigher;
	}
//
//	public static FP128 zeroMSB(Fingerprint fp) {
//		final FP128 fp128 = (FP128) fp;
//		fp128.IrredPolyHigher = fp128.IrredPolyHigher & 0x7FFFFFFFFFFFL;
//		return fp128;
//	}

//	/**
//	 * @param fp
//	 * @return The index bits for the given fp
//	 */
//	public static long getIndex(FP128 fp) {
//		return fp.IrredPolyHigher & 0x7FFFFFFFFFFFL;
//	}

	public static FP128 max(final FP128 a, final FP128 b) {
		if (a.compareTo(b) < 0) {
			return b;
		} else {
			return a;
		}
	}
}
