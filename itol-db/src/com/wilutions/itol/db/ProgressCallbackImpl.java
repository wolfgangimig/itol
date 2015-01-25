package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgressCallbackImpl implements ProgressCallback {

	private final ProgressCallback parent;
	protected final String name;
	protected volatile String[] params;
	protected volatile double total;
	protected volatile double childSum;

	public ProgressCallbackImpl(String name) {
		this(null, name);
	}

	public ProgressCallbackImpl(ProgressCallback parent, String name) {
		this.parent = parent;
		this.name = name;
		if (parent != null) {
			parent.setParams(new String[0]);
		}
	}

	@Override
	public void setParams(String... params) {
		this.params = params;
		if (parent != null) {
			List<String> paramList = new ArrayList<String>(Arrays.asList(params));
			if (name != null && name.length() != 0) {
				paramList.add(0, name);
			}
			String[] parentParams = paramList.toArray(new String[paramList.size()]);
			parent.setParams(parentParams);
		}
	}

	@Override
	public void setProgress(double current) {
		double currentSum = childSum + current;
		StringBuilder line = new StringBuilder();
		line.append(name);
		if (params != null && params.length != 0) {
			line.append(" ");
			line.append(Arrays.toString(params));
		}

		if (parent != null && total > 0) {
			parent.setProgress(currentSum);
		}
	}

	@Override
	public void setTotal(double total) {
		this.total = total;
	}

	@Override
	public boolean isCancelled() {
		boolean ret = false;
		if (parent != null) {
			ret = parent.isCancelled();
		}
		return ret;
	}

	@Override
	public ProgressCallback createChild(String name) {
		return new ProgressCallbackImpl(this, name);
	}

	@Override
	public void setFinished() {
		childSum = 0;
		setProgress(total);
		if (parent != null && total > 0) {
			parent.childFinished(total);
		}
	}

	@Override
	public void childFinished(double total) {
		childSum += total;
	}

	public static void main(String[] args) {
		ProgressCallback p1 = new ProgressCallbackImpl("p1");
		p1.setTotal(100);

		ProgressCallback c1 = p1.createChild("c1");
		c1.setTotal(50);

		ProgressCallback c11 = c1.createChild("c11");
		c11.setTotal(10);
		ProgressCallback c12 = c1.createChild("c12");
		c12.setTotal(15);
		ProgressCallback c13 = c1.createChild("c13");
		c13.setTotal(25);

		ProgressCallback c2 = p1.createChild("c2");
		c2.setTotal(40);

		ProgressCallback c21 = c2.createChild("c21");
		c21.setTotal(10);
		ProgressCallback c22 = c2.createChild("c22");
		c22.setTotal(30);

		c1.setProgress(0); // 0
		c11.setProgress(0); // 0
		c11.setProgress(1); // 1
		c11.setProgress(2); // 2
		c11.setFinished(); // 10
		c12.setProgress(0); // 10
		c12.setProgress(10);// 20
		c12.setFinished(); // 25
		c13.setProgress(0); // 25
		c13.setProgress(20);// 45
		c13.setProgress(25);// 50
		c13.setFinished(); // 50
		c1.setFinished(); // 50

		c2.setProgress(0); // 50
		c21.setProgress(9); // 59
		c21.setFinished(); // 60
		c22.setProgress(0); // 60
		c22.setProgress(29);// 89
		c22.setFinished(); // 90
		c2.setFinished(); // 90

	}
}
