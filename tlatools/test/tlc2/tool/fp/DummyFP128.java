// Copyright (c) 2012 Markus Alexander Kuppe. All rights reserved.
package tlc2.tool.fp;

import tlc2.util.FP128;

@SuppressWarnings("serial")
public class DummyFP128 extends FP128 {

	public DummyFP128(long lower, long higher) {
		super();
		IrredPolyLower = lower;
		IrredPolyHigher = higher;
	}

	public DummyFP128(long l) {
		this(l, 0L);
	}
}
