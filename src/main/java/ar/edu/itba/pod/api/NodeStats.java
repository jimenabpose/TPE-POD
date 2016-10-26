package ar.edu.itba.pod.api;

import java.io.PrintStream;
import java.io.Serializable;

import ar.edu.itba.pod.util.DoNotChange;
import ar.edu.itba.pod.util.Immutable;

/**
 * Statistics about a node.
 * 
 * <p>
 * See each value method for an explanation of the meaning of each value
 * </p>
 */
@DoNotChange
@Immutable
public final class NodeStats implements Serializable {
	private static final long	serialVersionUID	= 8003083613353664802L;
	
	private final String nodeId;
	private final long receivedSignals;
	private final long storedSignals;
	private final long backupSignals;
	private final boolean degraded;

	/**
	 * Create a new statistic objects
	 */
	public NodeStats(String nodeId, long receivedSignals, long storedSignals, long backupSignals, boolean degraded) {
		super();
		this.nodeId = nodeId;
		this.receivedSignals = receivedSignals;
		this.backupSignals = backupSignals;
		this.storedSignals = storedSignals;
		this.degraded = degraded;
	}
	
	/**
	 * Returns the unique identifier for the node
	 */
	public String nodeId() {
		return nodeId;
	}
	
	/**
	 * Returns the number of signals that this node has received for comparisons.
	 * <p>This counter is incremented once for each signal received 
	 * for comparison with stored signals</p>
	 */
	public long receivedSignals() {
		return receivedSignals;
	}
	
	/**
	 * Returns the number of signals stored as backup in this node.
	 */
	public long backupSignals() {
		return backupSignals;
	}
	
	/**
	 * Returns the number of singal stored as primary in this node.
	 */
	public long storedSignals() {
		return storedSignals;
	}
	
	/**
	 * @return true if the node is in degraded state (i.e.: non fault-tolerant).
	 */
	public boolean isDegraded() {
		return degraded;
	}

	/**
	 * Prints the statistics in human readable form to an output stream
	 */
	public void print(PrintStream out) {
		out.println("Statistics for node: " + this.nodeId);
		if (degraded) {
			out.println("*** Node is working in degraded mode ***");
		}
		out.println("Number of signals received: " + this.receivedSignals);
		out.println("Number of signals stored: " + this.storedSignals);
		out.println("Number of signals backed-up: " + this.backupSignals);
	}
}
