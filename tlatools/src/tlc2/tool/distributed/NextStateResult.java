// Copyright (c) 2012 Microsoft Corporation.  All rights reserved.
package tlc2.tool.distributed;

import java.io.Serializable;
import java.util.List;

import tlc2.tool.TLCStateVec;
import tlc2.util.Fingerprint;

@SuppressWarnings("serial")
public class NextStateResult implements Serializable {

	private final long computationTime;
	private final long statesComputed;
	private final TLCStateVec[] nextStates;
	private final List<List<Fingerprint>> nextFingerprints;
	
	public NextStateResult(TLCStateVec[] nextStates, List<List<Fingerprint>> nextFingerprints, 
			long computationTime, long statesComputed) {
		// We send the states _and_ their fingerprints to prevent the server from
		// doing the (expensive) fingerprint calculation again.
		this.nextStates = nextStates;
		this.nextFingerprints = nextFingerprints;
		
		this.computationTime = computationTime;
		this.statesComputed = statesComputed;
	}
	
	public long getStatesComputedDelta() {
		return statesComputed - nextStates.length;
	}

	public long getComputationTime() {
		return computationTime;
	}

	public List<List<Fingerprint>> getNextFingerprints() {
		return nextFingerprints;
	}

	public TLCStateVec[] getNextStates() {
		return nextStates;
	}
}
