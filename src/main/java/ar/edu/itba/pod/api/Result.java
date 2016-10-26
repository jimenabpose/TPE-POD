package ar.edu.itba.pod.api;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import ar.edu.itba.pod.util.DoNotChange;
import ar.edu.itba.pod.util.Immutable;

/**
 * Result of finding similar signals
 */
@DoNotChange
@Immutable
public final class Result implements Serializable {
	private static final long serialVersionUID = -6569534192275231393L;
	
	private final Signal reference;
	private final int maxItems;
	private final SortedSet<Item> items;
	
	/**
	 * Creates a new result object
	 * @param reference The reference signal
	 */
	public Result(Signal reference) {
		this(reference, 10, new TreeSet<Item>());
	}
	
	/**
	 * Internal constructor for builders and test
	 */
	Result(Signal reference, int maxItems, SortedSet<Item> items) {
		if (reference == null) {
			throw new IllegalArgumentException("Reference signal cannot be null");
		}
		this.reference = reference;
		this.maxItems = maxItems;
		this.items = items;
	}
	
	/**
	 * Returns a new Result that includes (if appropiate) the item.
     * If the result has less items than the maximum allowed, the item will be added.
     * If there are enough items, and the new item has lower deviation than the last one,
     * the item will be added and the last one will be discarded. Else the same object will 
     * be returned.
	 * @return
	 */
	public Result include(Item item) {
		if (items.size() < maxItems) {
			SortedSet<Item> newItems = new TreeSet<>(items);
			newItems.add(item);
			return new Result(reference, maxItems, newItems);
		}
		if (items.last().compareTo(item) > 0) {
			SortedSet<Item> newItems = new TreeSet<>(items);
			newItems.add(item);
			newItems.remove(newItems.last());
			return new Result(reference, maxItems, newItems);
		}
		return this;
	}

	public Signal reference() {
		return reference;
	}
	
	public int size() {
		return items.size();
	}
	
	public Iterable<Item> items() {
		return items;
	}
	
	public Item find(Signal signal) {
		for (Item item : items) {
			if (item.signal().equals(signal)){
				return item;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Result(");
		str.append(size());
		if (size() > 0) { 
			str.append(", ");
			str.append(String.format("%.3f", items.first().deviation));
			if (size() > 1) {
				Iterator<Item> iter = items.iterator();
				iter.next();
				while(iter.hasNext()) {
					str.append("|");
					str.append(String.format("%.3f", iter.next().deviation));
				}
			}
		}
		str.append(")");
		return str.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((items == null) ? 0 : items.hashCode());
		result = prime * result
				+ ((reference == null) ? 0 : reference.hashCode());
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
		Result other = (Result) obj;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		if (reference == null) {
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}



	/**
	 * Result item, containing a signal and it's deviation from the reference
	 */
	@DoNotChange
	public final static class Item implements Serializable, Comparable<Item> {
		private static final long serialVersionUID = -8816707427963358435L;
		
		private final Signal signal;
		private final double deviation;
		
		public Item(Signal signal, double deviation) {
			super();
			this.signal = signal;
			this.deviation = deviation;
		}
		
		public Signal signal() {
			return signal;
		}
		
		public double deviation() {
			return deviation;
		}

		@Override
		public int compareTo(Item that) {
			int res = Double.compare(this.deviation, that.deviation);
			if (res == 0) {
				for (int i = 0; i < Signal.SIZE; i++) {
					res = Byte.compare(signal.content()[i], that.signal.content()[i]);
					if (res != 0) {
						return res;
					}
				}
			}
			return res;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(deviation);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result
					+ ((signal == null) ? 0 : signal.hashCode());
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
			Item other = (Item) obj;
			if (Double.doubleToLongBits(deviation) != Double
					.doubleToLongBits(other.deviation))
				return false;
			if (signal == null) {
				if (other.signal != null)
					return false;
			} else if (!signal.equals(other.signal))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return signal + "[dev: " + deviation + "]";
		}
	}
}
