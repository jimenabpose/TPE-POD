package ar.edu.itba.pod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ar.edu.itba.pod.api.Result;
import ar.edu.itba.pod.api.SPNode;
import ar.edu.itba.pod.api.SignalProcessor;
import ar.edu.itba.pod.signal.source.RandomSource;

/**
 * Simple interactive client to test a cluster node
 */
public class SampleClient {
	private final String hostname;
	private final int port;
	
	RandomSource src = new RandomSource();

	public SampleClient(String hostname, int port) {
		super();
		this.hostname = hostname;
		this.port = port;
	}

	public void start() {
		this.waitForCommand();
	}

	public void waitForCommand() {
		try {
			Registry registry = LocateRegistry.getRegistry(hostname, port);
			SignalProcessor	sp = (SignalProcessor) registry.lookup("SignalProcessor");
			SPNode node = (SPNode) registry.lookup("SPNode");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			printOptions();
			while (true) {
				String line = in.readLine().toLowerCase();
				long start = System.nanoTime();
				if (line.equals("1")) {
					node.join("cluster");
				}
				else if (line.equals("2")) {
					node.exit();
				} else if (line.equals("3")) {
					node.getStats().print(System.out);
				} else if (line.equals("4")) {
					Result results = sp.findSimilarTo(src.next());
					System.out.println(">>> Result: " + results);
				} else if (line.equals("5")) {
					this.generate(sp, 1);
				} else if (line.equals("6")) {
					this.generate(sp, 10);
				} else if (line.equals("7")) {
					this.generate(sp, 100);
				} else if (line.equals("8")) {
					this.generate(sp, 1000);
				} else if (line.equals("9")) {
					this.generate(sp, 1000000);
				} else if (line.equals("0")) {
					return;
				} else {
					System.out.println("Invalid option");
					printOptions();
					continue;
				}
				long end = System.nanoTime();
				System.out.println(">>> elapsed time: " + ((end - start) / 1000000000.0) + " seconds");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generate(SignalProcessor sp, int amount) throws RemoteException {
		for (int i = 0; i < amount; i++) {
			sp.add(src.next());
		}
	}

	private void printOptions() {
		System.out.println("Options:");
		System.out.println("1 - Join to cluster \"cluster\"");
		System.out.println("2 - Exit cluster");
		System.out.println("3 - Print stats");
		System.out.println("4 - Process signal for matches");
		System.out.println("5 - Add 1 signal");
		System.out.println("6 - Add 10 signals");
		System.out.println("7 - Add 100 signals");
		System.out.println("8 - Add 1.000 signals");
		System.out.println("9 - Add 1.000.000 signals");
		System.out.println("0 - End");
		System.out.print("> ");
		System.out.flush();
	}

	/**
	 * Simple cluster client.
	 * @param args Command line arguments: <hostname> <port> of the node's RMI Registry
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException {
		if (args.length < 2) {
			System.out.println("Command line parameters: SampleClient <host> <port> ");
			return;
		}
		SampleClient client = new SampleClient(args[0], Integer.valueOf(args[1]));
		client.start();
	}
}
