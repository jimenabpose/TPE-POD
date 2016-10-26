package ar.edu.itba.pod.signal.source;

import ar.edu.itba.pod.api.Signal;

/**
 * Source of {@link Signal}s.
 * Note that a signal source is not expected to end, as reading signals
 * through any sensor can be done for as much as required.
 */
public interface Source {
	/**
	 * Retrieves signals to be analyzed.
	 * This method may block if reading signals from an external source.
	 */
	public Signal next();

}
