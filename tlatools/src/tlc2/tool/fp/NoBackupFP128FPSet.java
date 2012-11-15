// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * - Hashing becomes less expensive compared to memory (Moore's law)
 * - Extending hashing to 128,256,... requires code change vs. n times 64bit hashing
 * - 0(1) for storage and distribution
 */
@SuppressWarnings("serial")
public abstract class NoBackupFP128FPSet extends FPSet {

	protected NoBackupFP128FPSet(FPSetConfiguration fpSetConfig)
			throws RemoteException {
		super(fpSetConfig);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#init(int, java.lang.String, java.lang.String)
	 */
	@Override
	public void init(int numThreads, String metadir, String filename)
			throws IOException {
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
	 * @see tlc2.tool.fp.FPSet#exit(boolean)
	 */
	@Override
	public void exit(boolean cleanup) throws IOException {
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
