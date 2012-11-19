// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.util;

import java.io.IOException;
import java.io.Serializable;

public abstract class Fingerprint implements Serializable, Comparable<Fingerprint> {

	/**
	 * Extend the fingerprint <code>fp</code> by the characters of
	 * <code>s</code>.
	 */
	public abstract Fingerprint extend(final String s);

	/**
	 * Extend the fingerprint <code>fp</code> by the bytes in the array
	 * <code>bytes</code>.
	 * 
	 * @see Fingerprint#extend(byte[], int, int)
	 */
	public abstract Fingerprint extend(final byte[] bytes);

	/**
	 * Extend the fingerprint <code>fp</code> by the bytes in the array
	 * <code>bytes</code>.
	 */
	public abstract Fingerprint extend(final byte[] bytes, int start, int len);

	/**
	 * Extend the fingerprint <code>fp</code> by a character <code>c</code>.
	 */
	public abstract Fingerprint extend(final char c);

	/**
	 * Extend the fingerprint <code>fp</code> by a byte <code>c</code>.
	 */
	public abstract Fingerprint extend(final byte b);

	/**
	 * Extend the fingerprint <code>fp</code> by an integer <code>x</code>.
	 */
	public abstract Fingerprint extend(final int x);

	public abstract int getIndex(long mask);

	public abstract void write(java.io.RandomAccessFile raf) throws IOException;

	/**
	 * @return true iff the {@link Fingerprint} has been flushed to disk
	 */
	public abstract boolean isOnDisk();
	
	/**
	 * Return the fingerprint represented as a long value. It depends on the
	 * underlying implementation, if the fingerprint has a long representation.
	 * Throws a runtime exception otherwise.
	 */
	public abstract long longValue();

	public static abstract class FPFactory {
		public static FPFactory getInstance() {
			return new FP128.Factory();
		}

		public abstract Fingerprint newFingerprint();

		public abstract Fingerprint newFingerprint(final java.io.RandomAccessFile raf) throws IOException;
	}
}