package ar.edu.itba.pod.api;

import java.io.Serializable;
import java.util.Arrays;

import ar.edu.itba.pod.util.DoNotChange;
import ar.edu.itba.pod.util.Immutable;

/**
 * Signal representation.
 * 
 * This class represents a signal a byte array of captured amplitude over a small period of time.
 * The signal size is fixed to a given size in order to simplify preprocessing. 
 * Signals longer that the fixed size are splitted into several signals.
 */
@DoNotChange
@Immutable
public final class Signal implements Serializable {
	private static final long	serialVersionUID	= 1429916671677814826L;
	public static int SIZE	= 1024;
	
	private final byte[] content;

	/**
	 * Creates a new signal instance.
	 * The byte array passed is owned by the new signal object, and must not be modified
	 * by the caller after the object has been created.
	 * The content's length must match the expected lengh of <code>Signal.SIZE</code> or an exception will be thrown
	 */
	public Signal(byte[] content) {
		if (content ==  null) {
			throw new IllegalArgumentException("Signal content information was not provided");
		}
		if (content.length != SIZE) {
			throw new IllegalArgumentException("Signal content size mismatch. Expected: " + SIZE + ", received: " + content.length);
		}
		this.content = content;
	}
	
	public byte[] content() {
		return this.content;
	}

	/**
	 * Compares this signal to another one and returns the deviation.
	 * 
	 * Signal deviation is computed by finding the minimum average of squared diferences between <code>this</code>
	 * and <code>that</code>, byte by byte, for each possible alignment of the signals offsetting one up to 10%
	 * of it's size to the other.
	 * 
	 * @return Deviation. 0 if the signals match, >0 increasing as the signals differ more and more.
	 */
	public double findDeviation(Signal that) {
		if (that == null) {
			throw new IllegalArgumentException("Signal cannot be null");
		}

		double difference = findDifference(0, that, 0);
		int delta = this.content.length / 10;
		for (int i = 1; i < delta; i++) {
			difference = Math.min(difference, findDifference(i, that, 0));
			difference = Math.min(difference, findDifference(0, that, i));
		}
		return difference;
	}
	
	/**
	 * Returns the difference of this signal and the other, as the avergae sum of squares of the diference byte by byte
	 */
	private double findDifference(int offset, Signal that, int thatOffset) {
		long difference = 0L;
		int count;
		for (count=0; offset < content.length && thatOffset < that.content.length; offset++, thatOffset++, count++) {
			int dif = this.content[offset] - that.content[thatOffset];
			difference += dif * dif;
		}
		return difference/(double)count;
	}
	
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("Sig(");
		for (int i = 0; i < 10; i++) {
			res.append(String.format("%02x,", content[i]));
		}
		res.append("...)");
		return res.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(content);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Signal other = (Signal) obj;
		if (!Arrays.equals(content, other.content))
			return false;
		return true;
	}
}
