package ar.edu.itba.pod.signal.source;

import java.util.List;

import ar.edu.itba.pod.api.Signal;

/**
 * Source file that has a predefined number of signals.
 * After all the signals are exhausted, the source will reset and 
 * start with the first one
 */
public class PredefinedSource implements Source {
	public final List<Signal> signals;
	public int index = 0;
	
	public PredefinedSource(List<Signal> signals) {
		super();
		if (signals == null || signals.size() == 0) {
			throw new IllegalArgumentException("List of signals must not be null or empty");
		}
		this.signals = signals;
	}

	@Override
	public Signal next() {
		index = (index == signals.size()) ? 0 : index+1;
		return signals.get(index);
	}

}
