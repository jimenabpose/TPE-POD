package ar.edu.itba.pod.impl;

import static org.junit.Assert.assertEquals;
import static ar.edu.itba.pod.signal.source.SignalBuilder.*;

import java.rmi.RemoteException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import ar.edu.itba.pod.api.Signal;
import ar.edu.itba.pod.api.SignalProcessor;
import ar.edu.itba.pod.signal.source.RandomSource;
import ar.edu.itba.pod.signal.source.Source;
import ar.edu.itba.pod.signal.source.SignalBuilder.SignalFunction;

/**
 * Test suite that runs the same tests agains an implementation and
 * compares the result against a reference.
 * 
 * Note that this class is abstract. Concrete suites are created by subclassing this and
 * overriding the init() method. 
 */
public abstract class SideBySideTester {
	private SignalProcessor reference;
	private SignalProcessor toTest;
	private Source src;
	
	@Before
	public void setup() throws Exception {
		this.reference = new StandaloneSignalProcessor();
		toTest = init();
		src = new RandomSource(12345);
	}


	/**
	 * Initialize or get a reference to the signal processor to be tested.
	 */
	abstract protected SignalProcessor init() throws Exception;
	
	@Test
	public void test01() throws RemoteException {
		addNoise(100);
		assertFind(src.next());
	}

	@Test
	public void test02() throws RemoteException {
		addNoise(1000);
		add(constant((byte)4));
		add(constant((byte)10));
		assertFind(constant((byte)6));
	}

	@Test
	public void test03() throws RemoteException {
		addNoise(1000);
		Signal r = src.next();
		add(rotate(r, 5));
		add(rotate(r, 10));
		add(rotate(r, 50));
		add(rotate(r, 100));
		add(rotate(r, 200));
		assertFind(r);
	}

	@Test
	public void test04() throws RemoteException {
		add(sine());
		add(square());
		add(modulate(square(), 0.9));
		assertFind(modulate(sine(), 0.9));
	}
	
	@Test
	public void test05() throws RemoteException {
		addNoise(10);
		Signal r = src.next();
		add(rotate(r, 5));
		add(rotate(r, 10));
		add(constant((byte)10));
		assertFind(r);
	}
	
	@Test
	public void test06() throws RemoteException {
		addNoise(1000);
		Signal r = triangle();
		add(r);
		assertFind(r);
	}
	
	@Test
	public void test07() throws RemoteException {
		addNoise(10);
		Signal r = src.next();
		add(rotate(r, 5));
		add(rotate(r, 10));
		add(constant((byte)10));
		add(triangle());
		assertFind(r);

	}
	
	@Test
	public void test08() throws RemoteException {
		addNoise(1500);
		add(square());
		add(sine());
		add(triangle());
		assertFind(triangle());
	}
	
	@Test
	public void test09() throws RemoteException {
		addNoise(150);
		byte[] flux = Arrays.copyOfRange(triangle().content(), 3, 50);
		Signal r = flux((byte)10, 50,flux );
		add(r);
		assertFind(r);
		
	}
	
	
	
	private void assertFind(Signal s) throws RemoteException {
		assertEquals(1,1);
		assertEquals(reference.findSimilarTo(s), toTest.findSimilarTo(s));
	}
	private void add(Signal s) throws RemoteException {
		reference.add(s);
		toTest.add(s);
	}
	
	private void addNoise(int amount) throws RemoteException {
		for (int i = 0; i < amount; i++) {
			Signal s = src.next();
			reference.add(s);
			toTest.add(s);
		}
	}
}
