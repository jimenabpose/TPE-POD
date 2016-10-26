package ar.edu.itba.pod.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ar.edu.itba.pod.util.DoNotChange;

/**
 * Signal processor that finds similarities between signals
 */
@DoNotChange
public interface SignalProcessor extends Remote {

	/**
	 * Adds a new {@link Signal} to the database. 
	 * This method may return before the signal is actually added, but calling get <code>findSimilarTo(...)</code> from the same client after adding
	 * some signals MUST take them into account (read your writes consistency model).
	 * 
	 * If the same signal is added twice to the system, the behaviour is unespecified. Implementations may discard the duplicate signal or store it twice.
	 * 
	 * @param signal The signal to add.
	 */
	void add(Signal signal) throws RemoteException;

	/**
	 * Returns the top 10 {@link Signal}s that best match the given signal.
	 * If the system has less than 10 signals registered, it may return less than 10 results, but it will never return more.
	 * If there are several signals with the same comparison value, any of them may be in the results in an unespecified order.
	 * @param signal The signal to compare against stored signals
	 */
	Result findSimilarTo(Signal signal) throws RemoteException;
}
