package ar.edu.itba.pod.impl;

import static junit.framework.Assert.assertEquals;

import java.rmi.RemoteException;

import org.junit.Before;
import org.junit.Test;

import ar.edu.itba.pod.api.Result;
import ar.edu.itba.pod.api.Signal;
import ar.edu.itba.pod.signal.source.RandomSource;
import ar.edu.itba.pod.signal.source.Source;

public class StandaloneSignalProcessorTest {
	private StandaloneSignalProcessor sut;
	private Source src;

	@Before
	public void setup() {
		 sut = new StandaloneSignalProcessor();
		 src = new RandomSource(1);
	}
	
	@Test
	public void startsEmpty() throws RemoteException {
		Signal s1 = src.next();
		Result res = sut.findSimilarTo(s1);

		assertEquals(res.size(), 0);
	}
	
	@Test
	public void addNewSignal() throws RemoteException {
		Signal s1 = src.next();

		sut.add(s1);
		Result res = sut.findSimilarTo(s1);
		
		assertEquals(res.size(), 1);
		assertEquals(res.find(s1), new Result.Item(s1, 0.0));
	}
	
	@Test 
	public void returnsUpTo10Results() throws RemoteException {
		for (int i = 0; i<50; i++) {
			sut.add(src.next());
		}
		Result res = sut.findSimilarTo(src.next());
		
		assertEquals(res.size(), 10);
	}
	
	@Test 
	public void returnsCorrectDeviation() throws RemoteException {
		Signal s1 = src.next();
		Signal s2 = src.next();
		Signal s3 = src.next();
		
		sut.add(s1);
		sut.add(s2);
		
		Result res = sut.findSimilarTo(s3);
		
		assertEquals(res.size(), 2);
		assertEquals(res.find(s1), new Result.Item(s1, s3.findDeviation(s1)));
		assertEquals(res.find(s2), new Result.Item(s2, s3.findDeviation(s2)));
	}
	
}
