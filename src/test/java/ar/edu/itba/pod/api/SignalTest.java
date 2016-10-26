package ar.edu.itba.pod.api;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static ar.edu.itba.pod.signal.source.SignalBuilder.*;

import org.junit.Test;

public class SignalTest {

	@Test
	public void sameSignalHasNoDeviation() {
		Signal s = constant((byte) 60);
		assertEquals(s.findDeviation(s), 0.0);
	}
	
	@Test
	public void deviationIsCommutative() {
		Signal s = constant((byte) 5);
		Signal s2 = constant((byte) 10);
		assertEquals(s.findDeviation(s2), s2.findDeviation(s));
	}
	
	@Test
	public void deviationHandlesDifferentStart() {
		Signal s = constant((byte) 5);
		Signal s2 = flux((byte) 5, 4, new byte[] { 10, 10, 10 });
		
		assertEquals(s.findDeviation(s2), 0.0);
	}

	@Test
	public void deviationHandlesDifferentEnd() {
		Signal s = constant((byte) 5);
		Signal s2 = flux((byte) 5, Signal.SIZE - 10, new byte[] { 10, 10, 10 });
		
		assertEquals(s.findDeviation(s2), 0.0);
	}
	
	@Test 
	public void deviationGrowsOnBitMismatches() {
		Signal s = constant((byte) 5);
		Signal s2 = flux((byte) 5, 500, new byte[] { 6 });
		Signal s3 = flux((byte) 5, 500, new byte[] { 6, 6 });
		
		assertTrue(s.findDeviation(s2) < s.findDeviation(s3));
	}
	
	@Test 
	public void deviationGrowsOnValueDifference() {
		Signal s = constant((byte) 5);
		Signal s2 = flux((byte) 5, 500, new byte[] { 6 });
		Signal s3 = flux((byte) 5, 500, new byte[] { 7 });

		assertTrue(s.findDeviation(s2) < s.findDeviation(s3));
	}

	@Test 
	public void deviationIsOffsetAgnostic() {
		Signal s = constant((byte) 5);
		Signal s2 = flux((byte) 5, 500, new byte[] { 6 });
		Signal s3 = flux((byte) 5, 800, new byte[] { 6 });

		assertEquals(s.findDeviation(s2), s.findDeviation(s3));
	}

	@Test 
	public void deviationDoesntOverflow() {
		Signal s = constant((byte) -128);
		Signal s2 = constant((byte) 127);

		assertFalse(Double.isInfinite(s.findDeviation(s2)));
		assertFalse(Double.isNaN(s.findDeviation(s2)));
		assertTrue(s.findDeviation(s2) > 0.0);
	}
	
}
