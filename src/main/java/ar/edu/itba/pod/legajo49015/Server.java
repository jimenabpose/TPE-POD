package ar.edu.itba.pod.legajo49015;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
	private final int port;
	private final int threads;
	
	private SPNodeImpl node;

	public Server(int port, int threads) {
		this.port = port;
		this.threads = threads;
	}

	public void start() {
		Registry reg;
		try {
			reg = LocateRegistry.createRegistry(port);

			node = new SPNodeImpl(threads);
			Remote proxy = UnicastRemoteObject.exportObject(node, 0);

			// Since the same implementation exports both interfaces, register
			// the same proxy under the two names
			reg.bind("SignalProcessor", proxy);
			reg.bind("SPNode", proxy);
			System.out.println("Server started and listening on port " + port);
		} catch (RemoteException e) {
			System.out.println("Unable to start local server on port " + port);
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.out.println("Unable to register remote objects. Perhaps another instance is runnign on the same port?");
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Command line parameters: Server <port> <threads>");
			return;
		}
		
		Server server = new Server(Integer.valueOf(args[0]),Integer.valueOf(args[1]));
		server.start();
	}
}
