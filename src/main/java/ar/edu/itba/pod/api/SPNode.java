package ar.edu.itba.pod.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ar.edu.itba.pod.util.DoNotChange;

/**
 * Administrative interface for SignalProcessing nodes
 */
@DoNotChange
public interface SPNode extends Remote {

	/**
	 * Joins a cluster.
	 * If there is no cluster found under the given name, a new cluster will be started.
	 * In order to join a cluster the node must not have any signal registered.
	 * 
	 * @param clusterName Name of the cluster to create or join
	 * @throws IllegalStateException if the node joins a cluster but already has some values stored
	 * @throws IllegalStateException if the node is already part of a cluster
	 * @throws RemoteException
	 */
	void join(String clusterName) throws RemoteException;

	/**
	 * Cleanly disconects a node the the cluster.
	 * Note that this command doesn't stop the node, but detaches it from a cluster and returns it to it's initial state: no signals 
	 * stored, and not part of any cluster.
	 * If the node is the only node in the cluster, the cluster will be shutdown. 
	 */
	void exit() throws RemoteException;

	/**
	 * Returns the running {@link NodeStats} for this node. 
	 * @return Statistics for this node
	 * @throws RemoteException
	 */
	NodeStats getStats() throws RemoteException;
}
