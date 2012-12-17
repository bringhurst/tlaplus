// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import java.io.IOException;
import java.rmi.RemoteException;

import tlc2.tool.fp.FPSet;
import tlc2.tool.fp.FPSetConfiguration;
import tlc2.tool.fp.MSBMultiFPSet;
import tlc2.util.FP128;
import tlc2.util.Fingerprint;

@SuppressWarnings("serial")
public class FP128MSBMultiFPSet extends MSBMultiFPSet {

	public FP128MSBMultiFPSet(FPSetConfiguration fpSetConfiguration)
			throws RemoteException {
		super(fpSetConfiguration);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#put(tlc2.util.Fingerprint)
	 */
	public boolean put(Fingerprint fp) throws IOException {
		final FP128 fp128 = (FP128) fp;
		long higher = fp128.getHigher();
		FPSet fpSet = getFPSet(higher);
		if (fpSet == null) {
			throw new NullPointerException();
		}
		return fpSet.put(fp128);
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSet#contains(tlc2.util.Fingerprint)
	 */
	public boolean contains(Fingerprint fp) throws IOException {
		final FP128 fp128 = (FP128) fp;
		return getFPSet(fp128.getHigher()).contains(fp128);
	}
}
