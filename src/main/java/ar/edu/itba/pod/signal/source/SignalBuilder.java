package ar.edu.itba.pod.signal.source;

import ar.edu.itba.pod.api.Signal;

/**
 * Method that help crafting signals
 */
public class SignalBuilder {
	/**
	 * Creates a signal with constant value
	 */
	public static Signal constant(byte value) {
		byte[] cont = new byte[Signal.SIZE];
		for (int i = 0; i<cont.length; i++) {
			cont[i] = value;
		}
		return new Signal(cont);
	}

	/**
	 * Creates a signal that is constant except for a flux on a specified offset
	 * @return
	 */
	public static Signal flux(byte value, int offset, byte[] flux) {
		byte[] cont = new byte[Signal.SIZE];
		for (int i = 0; i<cont.length; i++) {
			cont[i] = value;
		}
		for (int i = 0; i < flux.length; i++) {
			cont[offset + i] = flux[i];
		}
		return new Signal(cont);
	}
	
	/**
	 * Builds a signal from a function
	 */
	public static Signal fn(SignalFunction fn) {
		byte[] cont = new byte[Signal.SIZE];
		for (int i = 0; i<cont.length; i++) {
			cont[i] = fn.eval(i * (1.0 / (Signal.SIZE-1)));
		}
		return new Signal(cont);
	}
	
	/**
	 * Builds a signal as a rotation of another signal
	 */
	public static Signal rotate(Signal sg, int offset) {
		byte[] cont = new byte[Signal.SIZE];
		for (int i = 0; i<cont.length; i++) {
			cont[i] = sg.content()[(Signal.SIZE-offset+i) % Signal.SIZE];
		}
		return new Signal(cont);
		
	}
	
	/**
	 * Returns a sinusoidal signal
	 */
	public static Signal sine() {
		return fn(new SignalFunction() {
			@Override
			public byte eval(double x) {
				double rad = Math.PI * 2 * x;
				return (byte)(Math.sin(rad) * 127);
			}
		});
	}
	
	/**
	 * Return a Triangle signal
	 */
	public static Signal triangle() {
		return fn(new SignalFunction() {
			@Override
			public byte eval(double x) {
				int k = 0;
				double sum = 0;
				double rad = 0;
				double numerator = 0;
				double denominator =0;
				for(k =0; k < 20; k++){
					rad = Math.PI * 2 * x;
					numerator = (Math.sin(2*k+1)*rad);
					denominator = Math.pow((2*k+1), 2);
					sum+= Math.pow(-1, k)*numerator/denominator;
				}
				
				return (byte)((8/((Math.pow(Math.PI, 2))))*sum);
			}
		});
	}

	/**
	 * Returns a squared signal, staring with a positive cycle
	 */
	public static Signal square() {
		byte[] cont = new byte[Signal.SIZE];
		int i;
		for (i = 0; i<cont.length/2; i++) {
			cont[i] = 127;
		}
		for (; i<cont.length; i++) {
			cont[i] = -127;
		}
		return new Signal(cont);
	}
	
	/**
	 * Returns a signal that is the given signal modulated by the given amplitude,
	 * with saturation (going over the max or min will result in max and 
	 * min value respectively).
	 * @param amplitude amplitude scale factor (1 == do not scale)
	 * @return
	 */
	public static Signal modulate(Signal base, double amplitude) {
		byte[] cont = new byte[Signal.SIZE];
		for (int i = 0; i<cont.length; i++) {
			cont[i] = toSaturatedByte(base.content()[i] * amplitude);
		}
		return new Signal(cont);
	}
	
	private static byte toSaturatedByte(double d) {
		if (d >= 127.0) {
			return 127;
		}
		if (d <= -127.0) {
			return -127;
		}
		return (byte)d;
	}

	/**
	 * Resolves a signal value based on a funciton evaluated in the interval [0, 1]
	 */
	public static interface SignalFunction {
		/**
		 * Evaluate the function in x (0.0 <= x <= 1.0)
		 */
		byte eval(double x);
	}
	
}
