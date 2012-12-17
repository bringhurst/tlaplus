// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import java.io.Serializable;

import tlc2.output.EC;
import tlc2.tool.fp.fp128.OffHeapDiskFPSet;
import tlc2.util.FP128;
import tlc2.util.FP64;
import tlc2.util.Fingerprint;
import util.Assert;
import util.TLCRuntime;

@SuppressWarnings("serial")
public class FPSetConfiguration implements Serializable {
	
	protected int fpBits = 0;
	protected long memoryInBytes = -1L;
	protected double ratio;
	protected String setImplementation;
	private int fpSize = FPSet.LongSize;
	private int fpIndex = 0;
	private Class<? extends Fingerprint> fpImplementation = FP64.class;

	public FPSetConfiguration() {
		// By default allocate 25% of memory for fingerprint storage
		this(.25d);
	}
	
	public FPSetConfiguration(Double aRatio) {
		this.ratio = aRatio;
		// Read the implementation class from the System properties instead of
		// the cmd line. Right now I'm reluctant to expose the impl class as a 
		// cmd line parameter and carry it forth forever.
		this.setImplementation = System.getProperty(FPSetFactory.IMPL_PROPERTY,
				this.getImplementationDefault());
	}

	protected String getImplementationDefault() {
		return FPSetFactory.getImplementationDefault();
	}

	public boolean allowsNesting() {
		return getFpBits() > 0;
	}
	
	public int getFpBits() {
		return fpBits;
	}
	
	public long getMemoryInBytes() {
		final TLCRuntime instance = TLCRuntime.getInstance();
		
		// Here the user has given a ratio of available memory to 
		// use for fingerprint storage
		if (FPSetFactory.allocatesOnHeap(setImplementation)) {
			// If a user has set memory explicitly, we pass this value to 
			// getFPMemSize(double) which sanitizes the value.
			if (memoryInBytes > 0) {
				return instance.getFPMemSize(memoryInBytes * ratio);
			} else {
				return instance.getFPMemSize(ratio);
			}
		} else {
			// Right now the only consumer of non-heap memory is fingerprint
			// storage. Thus, it makes little sense to allocate less than
			// all non-heap memory dedicated to the VM process. Until this changes,
			// we override ratio to devote all non-heap mem to fingerprint storage.
			//
			// TODO Respect ratio once other TLC data structures start using
			// non-heap memory
			return (long) (instance.getNonHeapPhysicalMemory()/* *ratio */);
		}
	}
	
	public long getMemoryInFingerprintCnt() {
		//TODO Replace FPSet.LongSize with fingerprint length
		
		return (long) Math.floor(getMemoryInBytes() / fpSize);
	}

	public int getMultiFPSetCnt() {
		return 1 << getFpBits();
	}
	
	public int getPrefixBits() {
		return getFpBits();
	}
	
	public void setRatio(double aRatio) {
		// Allowing aRatio to be 0.0 makes little sense semantically, but we
		// accept it anyway and let TLCRuntime deal with it.
		Assert.check(aRatio >= 0 && aRatio <= 1, EC.GENERAL);
		this.ratio = aRatio;
	}

	public void setFpBits(int fpBits) {
		Assert.check(FPSet.isValid(fpBits), EC.GENERAL);
		this.fpBits = fpBits;
	}

	public double getRatio() {
		return ratio;
	}

	/**
	 * @deprecated DO NOT USE, will be removed once -fpmem cmd line parameter
	 *             only accepts a ratio rather than an absolute memory value
	 * @param fpMemSize
	 */
	public void setMemory(long fpMemSize) {
		Assert.check(fpMemSize >= 0, EC.GENERAL);
		this.memoryInBytes = fpMemSize;
	}
	
	public String getImplementation() {
		return setImplementation;
	}

	public void setFPImplementation(Class<? extends Fingerprint> class1) {
		// 64bit case is per se the default, hence only handle 128bit case
		if (class1.isAssignableFrom(FP128.class)) {
			fpSize = FP128.BYTES;

			// 128bit fingerprints are only supported by an OffHeapDiskFPSet at
			// the moment. Thus, we set ratio and implementation explicitly
			// here. Eventually, when more 128bit FPSets descend into TLC, ratio
			// and impl shouldn't be set here anymore.
			ratio = 1.;
			setImplementation = OffHeapDiskFPSet.class.getName();
			fpImplementation = class1;
		}
	}

	public Class<? extends Fingerprint> getFPImplementation() {
		return fpImplementation ;
	}

	public void setFPIndex(int fpIndex) {
		this.fpIndex = fpIndex;
	}
	
	public int getFPIndex() {
		return this.fpIndex;
	}
}
