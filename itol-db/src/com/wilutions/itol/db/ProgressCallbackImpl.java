/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */
package com.wilutions.itol.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class ProgressCallbackImpl implements ProgressCallback {

	private final ProgressCallback parent;
	protected final String name;
	protected volatile String[] params;
	protected volatile double total = 1.0;
	protected volatile double ratio = 1.0;
	protected volatile double current;
	protected volatile Thread lastThread = Thread.currentThread();
	protected volatile boolean cancelled = false;
	protected volatile boolean finished = false;
	protected Timeline fakeProgressTimer;

	public ProgressCallbackImpl() {
		this(null, "");
	}

	public ProgressCallbackImpl(String name) {
		this(null, name);
	}

	public ProgressCallbackImpl(ProgressCallback parent, String name) {
		// System.out.println(name + " start");
		
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
	public void incrProgress(double amount) {
		lastThread = Thread.currentThread();
		if (fakeProgressTimer == null) {
			internalIncrProgress(amount);
		}
	}
	
	protected synchronized void internalIncrProgress(double amount) {
		current += amount;
		// System.out.println(name + " incr " + amount + ", current=" + current);
		if (current >= total) {
			internalSetFinished();
		}
		if (parent != null) {
			double d = amount / total;
			parent.incrProgress(d * parent.getTotal() * ratio);
		}
	}
	
	@Override
	public double getProgress() {
		return current;
	}

	@Override
	public void setTotal(double total) {
		this.total = total;
	}
	
	@Override
	public double getTotal() {
		return total;
	}

	@Override
	public boolean isCancelled() {
		boolean ret = cancelled;
		if (!ret && parent != null) {
			ret = parent.isCancelled();
		}
		return ret;
	}
	
	@Override
	public void cancel() {
		cancelled = true;
		if (lastThread != null) {
			lastThread.interrupt();
		}
		setFinished();
	}

	@Override
	public ProgressCallback createChild(String name, double ratio) {
		ProgressCallbackImpl cb = new ProgressCallbackImpl(this, name);
		cb.ratio = ratio;
		return cb;
	}

	@Override
	public ProgressCallback createChild(String name, double childTotal, double parentTotal) {
		return createChild(name, childTotal / parentTotal);
	}
	
	public ProgressCallback createChild(double ratio) {
		StackTraceElement[] stack = new Exception().getStackTrace();
		StackTraceElement elm = stack[1];
		String name = elm.getFileName() + ":" + elm.getLineNumber(); 
		return createChild(name, ratio);
	}
	
	public ProgressCallback createChild(double childTotal, double parentTotal) {
		StackTraceElement[] stack = new Exception().getStackTrace();
		StackTraceElement elm = stack[stack.length-2];
		String name = elm.getFileName() + ":" + elm.getLineNumber(); 
		return createChild(name, childTotal / parentTotal);
	}


	@Override
	public void setFinished() {
		// System.out.println(name + " finished");
		double amount = total - current;
		if (amount <= 0) return;
		internalIncrProgress(amount);
	}
	
	private synchronized void internalSetFinished() {
		finished = true;
		if (fakeProgressTimer != null) {
			fakeProgressTimer.stop();
		}
	}
	
	@Override
	public void setFakeProgress(boolean v) {
		if (v) {
			setTotal(1);
			fakeProgressTimer = new Timeline(new KeyFrame(Duration.seconds(0.2), new EventHandler<ActionEvent>() {
				double x = 0;

				@Override
				public void handle(ActionEvent event) {
					if (finished) return;
					x += .7;
					double nextVal = (-1 / x + 1);
					internalIncrProgress(nextVal - current);
				}
			}));
			fakeProgressTimer.setCycleCount(Timeline.INDEFINITE);
			fakeProgressTimer.play();
		}
	}

//	public static void main(String[] args) {
//		ProgressCallback p1 = new ProgressCallbackImpl("p1");
//		p1.setTotal(100);
//
//		ProgressCallback c1 = p1.createChild("c1");
//		c1.setTotal(50);
//
//		ProgressCallback c11 = c1.createChild("c11");
//		c11.setTotal(10);
//		ProgressCallback c12 = c1.createChild("c12");
//		c12.setTotal(15);
//		ProgressCallback c13 = c1.createChild("c13");
//		c13.setTotal(25);
//
//		ProgressCallback c2 = p1.createChild("c2");
//		c2.setTotal(40);
//
//		ProgressCallback c21 = c2.createChild("c21");
//		c21.setTotal(10);
//		ProgressCallback c22 = c2.createChild("c22");
//		c22.setTotal(30);
//
//		c1.setProgress(0); // 0
//		c11.setProgress(0); // 0
//		c11.setProgress(1); // 1
//		c11.setProgress(2); // 2
//		c11.setFinished(); // 10
//		c12.setProgress(0); // 10
//		c12.setProgress(10);// 20
//		c12.setFinished(); // 25
//		c13.setProgress(0); // 25
//		c13.setProgress(20);// 45
//		c13.setProgress(25);// 50
//		c13.setFinished(); // 50
//		c1.setFinished(); // 50
//
//		c2.setProgress(0); // 50
//		c21.setProgress(9); // 59
//		c21.setFinished(); // 60
//		c22.setProgress(0); // 60
//		c22.setProgress(29);// 89
//		c22.setFinished(); // 90
//		c2.setFinished(); // 90
//
//	}
}
