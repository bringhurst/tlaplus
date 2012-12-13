// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp.fp128;

import tlc2.tool.fp.FPSetConfiguration;
import tlc2.util.FP128;

@SuppressWarnings("serial")
public class FP128SetConfiguration extends FPSetConfiguration {

	public FP128SetConfiguration() {
		super();
		ratio = 1.;
		implementation = OffHeapDiskFPSet.class.getName();
	}

	/* (non-Javadoc)
	 * @see tlc2.tool.fp.FPSetConfiguration#getMemoryInFingerprintCnt()
	 */
	public long getMemoryInFingerprintCnt() {
		return (long) Math.floor(getMemoryInBytes() / FP128.BYTES);
	}
}
