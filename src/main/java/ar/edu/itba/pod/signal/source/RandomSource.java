package ar.edu.itba.pod.signal.source;

import java.util.Random;

import ar.edu.itba.pod.api.Signal;

/**
 * Implementation that generates {@link Signal}s with random values. 
 */
public class RandomSource implements Source {
	private final Random random;
	
	public RandomSource() {
		super();
		this.random = new Random();
	}

	public RandomSource(int seed) {
		super();
		this.random = new Random(seed);
	}
	
	@Override
	public Signal next() {
		byte[] bytes = new byte[Signal.SIZE];
		this.random.nextBytes(bytes);
		return new Signal(bytes);
	}
}
