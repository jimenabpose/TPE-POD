package ar.edu.itba.pod.impl;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.pod.api.NodeStats;
import ar.edu.itba.pod.api.Result;
import ar.edu.itba.pod.api.SPNode;
import ar.edu.itba.pod.api.Signal;
import ar.edu.itba.pod.api.SignalProcessor;

/**
 * Simple implementation of a signal processor that works on a single thread and
 * a single node.
 */
public class StandaloneSignalProcessor implements SignalProcessor, SPNode {
	private final Set<Signal> signals = new HashSet<>();
	private int receivedSignals = 0;
	private String cluster = null;
	
	@Override
	public void join(String clusterName) throws RemoteException {
		if (cluster != null) {
			throw new IllegalStateException("Already in cluster " + cluster);
		}
		if (!signals.isEmpty()) {
			throw new IllegalStateException("Can't join a cluster because there are signals already stored");
		}
		this.cluster = clusterName;
	}
	
	@Override
	public void exit() throws RemoteException {
		signals.clear();
		receivedSignals = 0;
		cluster = null;
	}
	
	@Override
	public NodeStats getStats() throws RemoteException {
		return new NodeStats(
				cluster == null ? "standalone" : "cluster " + cluster, 
				receivedSignals,
				signals.size(), 
				0, 
				true);
	}
	
	@Override
	public void add(Signal signal) throws RemoteException {
		signals.add(signal);
	}
	
	@Override
	public Result findSimilarTo(Signal signal) throws RemoteException {
		if (signal == null) {
			throw new IllegalArgumentException("Signal cannot be null");
		}

		receivedSignals++;
		Result result = new Result(signal);
		
		for (Signal cmp : signals) {
			Result.Item item = new Result.Item(cmp, signal.findDeviation(cmp));
			result = result.include(item);
		}
		return result;
	}
}
