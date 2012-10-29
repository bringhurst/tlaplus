package tlc2.util;

import java.io.IOException;
import java.io.ObjectOutputStream;

public interface Fingerprint {

	/**
	 * Extend the fingerprint <code>fp</code> by the characters of
	 * <code>s</code>.
	 */
	public abstract Fingerprint extend(String s);

	public abstract Fingerprint extend(byte[] bytes);

	/**
	 * Extend the fingerprint <code>fp</code> by the bytes in the array
	 * <code>bytes</code>.
	 */
	public abstract Fingerprint extend(byte[] bytes, int start, int len);

	/**
	 * Extend the fingerprint <code>fp</code> by a character <code>c</code>.
	 */
	public abstract Fingerprint extend(char c);

	/**
	 * Extend the fingerprint <code>fp</code> by a byte <code>c</code>.
	 */
	public abstract Fingerprint extend(byte b);

	/*
	 * Extend the fingerprint <code>fp</code> by an integer <code>x</code>.
	 */
	public abstract Fingerprint extend(int x);

	public abstract int getIndex(long mask);

	public abstract void write(BufferedRandomAccessFile raf) throws IOException;

	public abstract void write(ObjectOutputStream oos) throws IOException;

}