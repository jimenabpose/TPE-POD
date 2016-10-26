package ar.edu.itba.pod.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import ar.edu.itba.pod.api.Result.Item;
import ar.edu.itba.pod.signal.source.RandomSource;
import ar.edu.itba.pod.signal.source.Source;

public class ResultTest {
	private Source src;
	
	@Before
	public void setup() {
		this.src = new RandomSource(1);
	}
	
	@Test
	public void resultStartsEmpty() {
		Result r = new Result(src.next());
		
		assertEquals(r.size(), 0);
	}

	@Test
	public void includePreservesOrder() {
		Result r = new Result(src.next());
		r.include(new Item(src.next(), 2.0));
		r.include(new Item(src.next(), 1.0));
		r.include(new Item(src.next(), 5.0));
		r.include(new Item(src.next(), 0.5));

		double lastDev = 0.0;
		for (Result.Item item : r.items()) {
			assertTrue(lastDev < item.deviation());
			lastDev = item.deviation();
		}
	}
	
	@Test 
	public void maxSizeIsHonored() {
		Result r = new Result(src.next(), 2, new TreeSet<Result.Item>());
		r = r.include(new Item(src.next(), 3.0));
		r = r.include(new Item(src.next(), 7.0));
		r = r.include(new Item(src.next(), 4.0));

		assertEquals(2, r.size());
		
		double lastDev = 0.0;
		for (Result.Item item : r.items()) {
			assertTrue(lastDev < item.deviation());
			lastDev = item.deviation();
		}
		assertEquals(lastDev, 4.0, 0.0001);
	}

}
