package ar.edu.itba.pod.legajo49015;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import ar.edu.itba.pod.api.NodeStats;
import ar.edu.itba.pod.api.Result;
import ar.edu.itba.pod.api.Result.Item;
import ar.edu.itba.pod.api.SPNode;
import ar.edu.itba.pod.api.Signal;
import ar.edu.itba.pod.api.SignalProcessor;
import ar.edu.itba.pod.legajo49015.message.GenericMessage;
import ar.edu.itba.pod.legajo49015.message.MessageType;
import ar.edu.itba.pod.legajo49015.message.NodeMessage;
import ar.edu.itba.pod.legajo49015.message.ResultMessage;
import ar.edu.itba.pod.legajo49015.message.SignalMessage;

public class SPNodeImpl extends ReceiverAdapter implements SignalProcessor, SPNode {

	private String nodeId;
	private String clusterName;
	private JChannel channel;
	
	// key: address que tiene mi backup, value: mis señales
	private ConcurrentHashMap<Address, BlockingQueue<Signal>> signals = new ConcurrentHashMap<Address, BlockingQueue<Signal>>();
	// key: de quien es el backup, value: backups 
	private ConcurrentHashMap<Address, BlockingQueue<Signal>> backups = new ConcurrentHashMap<Address, BlockingQueue<Signal>>();
	
	// donde se almacenan las señales cuando el nodo no esta conectado
	private BlockingQueue<Signal> localSignals = new LinkedBlockingQueue<Signal>();
	// donde se almacenan las señales cuando se hace un add para distribuirlas y poder hacer read your writes
	private BlockingQueue<Signal> tempSignals = new LinkedBlockingQueue<Signal>();
	
	private final int threads;
	
	private AtomicInteger receivedForComparisons = new AtomicInteger(0);
	private AtomicInteger comparisonsTries = new AtomicInteger(-1);
	private final int triesQuantity = 3;
	
	private View prevView;
	
	private BlockingQueue<Message> receivedMessages = new LinkedBlockingQueue<Message>();
	private BlockingQueue<View> views = new LinkedBlockingQueue<View>();
	
	// Respuestas de los nodos
	private BlockingQueue<Result> comparisons = new LinkedBlockingQueue<Result>();
	// Respuestas de los diferentes threads cuando el nodo esta solo
	private BlockingQueue<Result> localComparisons = new LinkedBlockingQueue<Result>();
	// Señales que se usan en el findSimilarTo
	private BlockingQueue<Signal> signalsToCompare = new LinkedBlockingQueue<Signal>();
	
	// addCounter = add enviados - confirmaicones
	private AtomicInteger addCounter = new AtomicInteger(0);
	private AtomicBoolean degraded = new AtomicBoolean(true);
	private AtomicBoolean degradedByMe = new AtomicBoolean(false);
	
	// Latch utilizado para esperar los resultados de los otros nodos o threads
	private CountDownLatch resultLatch;
	// Latch para esperar a que todos terminen de reacomodar las señales
	private CountDownLatch reorderingLatch;
	// Para evitar señales duplicadas si llaman al find similar mientras hay señales en la cola del add
	private Semaphore findSimilarOrAddSemaphore = new Semaphore(1);
	
	private ExecutorService actionsService = Executors.newFixedThreadPool(3);
	private ExecutorService threadsExecutorService;
	
	public SPNodeImpl(int threads) {
		this.threads = threads;
		this.threadsExecutorService = Executors.newFixedThreadPool(threads);
		actionsService.execute(new SignalDistributor());
		actionsService.execute(new ReceiveActions());
		actionsService.execute(new ViewAcceptedActions());
		try {
			this.channel = new JChannel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* SPNode methods */
	
	@Override
	public void join(String clusterName) {
		
		try {
			if(this.channel != null && this.channel.isConnected()) {
				throw new IllegalStateException("Already in cluster " + clusterName);
			}
			if (!this.localSignals.isEmpty() || !this.signals.isEmpty()) {
				throw new IllegalStateException("Can't join a cluster because there are signals already stored");
			}
			this.channel = new JChannel();
			this.clusterName = clusterName;
			channel.setReceiver(this);
			this.channel.connect(this.clusterName);
			this.nodeId = channel.getAddress().toString();
			if(this.channel.getView().getMembers().size() >= 2) {
				this.degraded.set(false);
			}
			this.addCounter.set(0);
			this.receivedForComparisons.set(0);
			findSimilarOrAddSemaphore.release();
			this.comparisons.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exit() throws RemoteException {
		if(this.channel != null && this.channel.getView() != null &&
				this.channel.getView().getMembers().size() == 1) {
			this.channel.close();
		}
		if(this.channel != null) {
			this.channel.disconnect();
		}
		this.degraded.set(true);
		this.degradedByMe.set(false);
		this.signals.clear();
		this.backups.clear();
		this.localSignals.clear();
		this.tempSignals.clear();
		this.comparisons.clear();
		this.localComparisons.clear();
		this.signalsToCompare.clear();
		this.addCounter.set(0);
		this.receivedForComparisons.set(0);
		this.receivedMessages.clear();
		this.views.clear();
		findSimilarOrAddSemaphore.release();
	}

	@Override
	public NodeStats getStats() throws RemoteException {
		if(this.channel != null && this.channel.isOpen() && this.channel.isConnected()) {
			return new NodeStats(this.nodeId, this.receivedForComparisons.intValue(), this.getSignalsQuantity(), this.getBackupsQuantity(), isDegraded());
		} else {
			return new NodeStats(this.nodeId, this.receivedForComparisons.intValue(), this.localSignals.size(), this.getBackupsQuantity(), isDegraded());
		}
	}
	
	/* SignalProcessor methods */

	@Override
	public void add(Signal signal) throws RemoteException {
		if(this.channel != null && this.channel.isConnected()) {
			if(getMembersQuantity() > 1 && !isDegraded()) {
				degradeEveryone();
			}
			this.tempSignals.add(signal);
		} else {
			this.localSignals.add(signal);
		}
	}
	
	@Override
	public Result findSimilarTo(Signal signal) throws RemoteException {
		if(signal == null) {
			throw new IllegalArgumentException("Signal to be compared can not be null");
		}
		Result result = new Result(signal);
		try {
			int members = getMembersQuantity();
			findSimilarOrAddSemaphore.acquire();
			comparisonsTries.incrementAndGet();
			receivedForComparisons.incrementAndGet();
			this.comparisons.clear();
			this.localComparisons.clear();
			this.signalsToCompare.clear();
			
			boolean await = false;
			
			if(this.channel != null && this.channel.isOpen() && this.channel.isConnected()) {
				// Les pido a todos que me devuelvan sus 10 mejores, y me quedo con las mejores 10
				SignalMessage signalMessage = new SignalMessage(MessageType.COMPARE_SIGNAL, signal, null, null);
				Message message = new Message(null, null, signalMessage);
				this.sendMessage(message);
				
				resultLatch = new CountDownLatch(getMembersQuantity()*threads);
				
				await = false;
				while(members == getMembersQuantity() && !await) {
					await = resultLatch.await(((long)getSignalsQuantity()*(long)0.001)+10000, TimeUnit.MILLISECONDS);
					if(!await && members != getMembersQuantity()) {
						if(comparisonsTries.get() < triesQuantity) {
							findSimilarOrAddSemaphore.release();
							reorderingLatch = new CountDownLatch(getMembersQuantity());
							reorderingLatch.await(((long)getSignalsQuantity()*(long)0.001)+10000, TimeUnit.MILLISECONDS);
							return findSimilarTo(signal);
						} 
					}
				}
				
				for(Result r : this.comparisons) {
					for(Item item : r.items()) {
						result = result.include(item);
					}
				}
			} else {
				signalsToCompare.addAll(localSignals);
				resultLatch = new CountDownLatch(threads);
				// encuentro yo las mejores 10
				findBest10Local(signal);

				await = false;
				while(members == getMembersQuantity() && !await) {
					await = resultLatch.await(((long)getSignalsQuantity()*(long)0.001)+10000, TimeUnit.MILLISECONDS); 
					if(!await && members != getMembersQuantity()) {
						if(comparisonsTries.get() < triesQuantity) {
							findSimilarOrAddSemaphore.release();
							return findSimilarTo(signal);
						}
					}
				}
				for(Result r : this.localComparisons) {
					for(Item item : r.items()) {
						result = result.include(item);
					}
				}
			}
			findSimilarOrAddSemaphore.release();
		} catch (InterruptedException e1) {
			if(comparisonsTries.get() < triesQuantity) {
				findSimilarOrAddSemaphore.release();
				return findSimilarTo(signal);
			} else {
				e1.printStackTrace();
			}
		}
		comparisonsTries.set(-1);
		reorderingLatch = null;
		return result;
	}
	
	/* Node methods */
	
	private Address getRandomAddress() {
		Random random = new Random();
		int rand = random.nextInt(getMembersQuantity());
		return getConnectedNodes().get(rand);
	}
	
	private List<Address> getConnectedNodes() {
		if(this.channel == null) {
			return null;
		}
		return this.channel.getView().getMembers();
	}
	
	private synchronized void addLocal(Signal signal, Address backupAddress, Address from) {
		this.signalsMapPut(backupAddress, signal);
		if(backupAddress != null) {
			GenericMessage genericMessage = new GenericMessage(MessageType.ADD_FINISHED);
			Message message = new Message(from, this.getAddress(), genericMessage);
			this.sendMessage(message);
		}
	}
	
	private synchronized void removeLocalBackup(Signal signal, Address address) {
		this.backupsMapRemove(address, signal);
	}
	
	@Override
	public void viewAccepted(View view) {
		this.views.add(view);
	}
	
	@Override
	public void receive(Message msg) {
		this.receivedMessages.add(msg);
	}
	
	private void sendMessage(Message message) {
		try {
			channel.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initSignals(final Address from) {
		int quantityToSend = getSignalsQuantity() / getMembersQuantity(); 
		Set<Entry<Address, BlockingQueue<Signal>>> signalsEntry = signals.entrySet();
		int i = 0;
		while(i < quantityToSend) {
			for(Entry<Address, BlockingQueue<Signal>> entry : signalsEntry) {
				if(i < quantityToSend) {
					for(Signal signal : entry.getValue()) {
						if(i < quantityToSend) {
							i++;
							
							if(!isDegraded()) {
								degradeEveryone();
							}
							
							Address backupOwner;
							do {
								backupOwner = getRandomAddress();
							} while (backupOwner.equals(from));
							
							prepareToSendSignal();
							SignalMessage signalMessage = new SignalMessage(MessageType.ADD_SIGNAL, signal, from, backupOwner);
							Message message = new Message(from, getAddress(), signalMessage);
							sendMessage(message);
							signalsMapRemove(entry.getKey(), signal);
							
							// saco la señal de donde estaba el backup y se lo agrego a alguien
							prepareToSendSignal();
							SignalMessage backupRemoveSignalMessage = new SignalMessage(MessageType.REMOVE_BACKUP_SIGNAL, signal, getAddress(), entry.getKey());
							Message backupMessage = new Message(entry.getKey(), getAddress(), backupRemoveSignalMessage);
							sendMessage(backupMessage);
							
							prepareToSendSignal();
							SignalMessage backupSignalMessage = new SignalMessage(MessageType.ADD_BACKUP_SIGNAL, signal, from, backupOwner);
							Message sendBackupSignalMessage = new Message(backupOwner, getAddress(), backupSignalMessage);
							sendMessage(sendBackupSignalMessage);
						}
					}
				}
			}
		}
		if(getMembersQuantity() == 2) {
			// Mis señales tienen que estar backapeadas en el nuevo 
			for(Signal signal : getAllSignals()) {
				prepareToSendSignal();
				SignalMessage signalMessage = new SignalMessage(MessageType.ADD_BACKUP_SIGNAL, signal, getAddress(), from);
				Message message = new Message(from, getAddress(), signalMessage);
				sendMessage(message);
			}
			BlockingQueue<Signal> mySignals = signals.get(getAddress());
			if(mySignals != null) {
				signals.put(from, mySignals);
				signals.remove(getAddress());
			}
		}
	}
	
	private void findBest10(final Signal signal, final Address from) {
		for(int i = 0; i < threads; i++) {
			threadsExecutorService.execute(new Runnable() {
				@Override
				public void run() {
					Result result = new Result(signal);
					while(!signalsToCompare.isEmpty()) {
						Signal cmp = signalsToCompare.poll();
						if(cmp != null) {
							double deviation = signal.findDeviation(cmp);
							Result.Item item = new Result.Item(cmp, deviation);
							result = result.include(item);
						}
					}
					ResultMessage resultMessage = new ResultMessage(MessageType.COMPARE_RESULTS, result);
					Message message = new Message(from, getAddress(), resultMessage);
					sendMessage(message);
				}
			});
		}
	}
	
	private void findBest10Local(final Signal signal) {
		for(int i = 0; i < threads; i++) {
			threadsExecutorService.execute(new Runnable() {
				@Override
				public void run() {
					Result result = new Result(signal);
					while(!signalsToCompare.isEmpty()) {
						Signal cmp = signalsToCompare.poll();
						if(cmp != null) {
							double deviation = signal.findDeviation(cmp);
							Result.Item item = new Result.Item(cmp, deviation);
							result = result.include(item);
						}
					}
					
					localComparisons.add(result);
					resultLatch.countDown();
				}
			});
		}
	}
	
	private Address getAddress() {
		if(this.channel == null) {
			return null;
		}
		return this.channel.getAddress();
	}
	
	private boolean isDegraded() {
		return degraded.get();
	}
	
	private int getMembersQuantity() {
		if(!this.channel.isConnected()) {
			return 1;
		}
		return this.channel.getView().getMembers().size();
	}
	
	private int getOldMembersQuantity() {
		if(prevView == null || prevView.getMembers() == null) {
			return 0;
		}
		return prevView.getMembers().size();
	}
	
	private boolean iAmNew(View prevView) {
		if(prevView == null || getAddress() == null) {
			return true;
		}
		return !prevView.containsMember(getAddress());
	}
	
	private synchronized int getSignalsQuantity() {
		int size = 0;
		for(BlockingQueue<Signal> values : signals.values()) {
			size += values.size();
		}
		return size;
	}
	
	private synchronized int getBackupsQuantity() {
		int size = 0;
		for(BlockingQueue<Signal> values : backups.values()) {
			size += values.size();
		}
		return size;
	}
	
	private synchronized BlockingQueue<Signal> getAllSignals() {
		BlockingQueue<Signal> s = new LinkedBlockingQueue<Signal>();
		for(BlockingQueue<Signal> values : signals.values()) {
			s.addAll(values);
		}
		return s;
	}
	
	private synchronized BlockingQueue<Signal> getAllBackupSignals() {
		BlockingQueue<Signal> s = new LinkedBlockingQueue<Signal>();
		for(BlockingQueue<Signal> values : backups.values()) {
			s.addAll(values);
		}
		return s;
	}
	
	private synchronized void signalsMapPut(Address key, Signal signal) {
		if(this.signals.containsKey(key)) {
			signals.get(key).add(signal);
		} else {
			BlockingQueue<Signal> values = new LinkedBlockingQueue<Signal>();
			values.add(signal);
			signals.put(key, values);
		}
	}
	
	private synchronized void signalsMapRemove(Address key, Signal signal) {
		if(this.signals.containsKey(key)) {
			signals.get(key).remove(signal);
			if(signals.get(key).isEmpty()) {
				signals.remove(key);
			}
		}
	}
	
	private synchronized void backupsMapPut(Address key, Signal signal) {
		if(this.backups.containsKey(key)) {
			backups.get(key).add(signal);
		} else {
			BlockingQueue<Signal> values = new LinkedBlockingQueue<Signal>();
			values.add(signal);
			backups.put(key, values);
		}
	}
	
	private synchronized void backupsMapRemove(Address key, Signal signal) {
		if(this.backups.containsKey(key)) {
			backups.get(key).remove(signal);
			if(backups.get(key).isEmpty()) {
				backups.remove(key);
			}
		}
	}
	
	private synchronized void prepareSignalsToCompare() {
		this.signalsToCompare.clear();
		this.signalsToCompare.addAll(getAllSignals());
		this.signalsToCompare.addAll(tempSignals);
	}
	
	private synchronized void prepareToSendSignal() {
		if(!isDegraded()) {
			degradeEveryone();
		}
		addCounter.incrementAndGet();
	}
	
	private synchronized void reorderingFinishedInform() {
		GenericMessage genericMessage = new GenericMessage(MessageType.REORDERING_FINISHED);
		Message message = new Message(null, getAddress(), genericMessage);
		this.sendMessage(message);
	}
	
	private void degradeEveryone() {
		if(!isDegraded()) {
			degradedByMe.set(true);
			degraded.set(true);
			GenericMessage degrade = new GenericMessage(MessageType.DEGRADE_MODE);
			Message message = new Message(null, getAddress(), degrade);
			sendMessage(message);
		}
	}
	
	
	private class SignalDistributor implements Runnable {
		@Override
		public void run() {
			while(!Thread.interrupted()) {
				try {
					Signal signal = tempSignals.take();
					if(getAddress() != null && channel.isConnected()) {
						findSimilarOrAddSemaphore.acquire();
						Address owner = getAddress();
						Address backupOwner = getAddress();
						
						if(getMembersQuantity() > 1) {
							do {
								owner = getRandomAddress();
								backupOwner = getRandomAddress();
							} while (owner.equals(backupOwner) && getMembersQuantity() > 1);
						}
						if(getMembersQuantity() == 1) {
							owner = getAddress();
							backupOwner = getAddress();
						}
						
						prepareToSendSignal();
						SignalMessage signalMessage = new SignalMessage(MessageType.ADD_SIGNAL, signal, owner, backupOwner);
						Message sendSignalMessage = new Message(owner, getAddress(), signalMessage);
						sendMessage(sendSignalMessage);
						
						if(getMembersQuantity() > 1) {
							prepareToSendSignal();
							SignalMessage backupSignalMessage = new SignalMessage(MessageType.ADD_BACKUP_SIGNAL, signal, owner, backupOwner);
							Message sendBackupSignalMessage = new Message(backupOwner, getAddress(), backupSignalMessage);
							sendMessage(sendBackupSignalMessage);
						}
						findSimilarOrAddSemaphore.release();
					} else {
						tempSignals.put(signal);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	private class ReceiveActions implements Runnable {
		@Override
		public void run() {
			Message msg;
			try {
				while(!Thread.interrupted()) {
					msg = receivedMessages.take();
					Address from = msg.getSrc();
					NodeMessage received = (NodeMessage) msg.getObject();
					if(msg.getDest() != null && msg.getDest().equals(getAddress())) {
						if(received.getMessageType().equals(MessageType.ADD_SIGNAL)) {
							SignalMessage signalMessage = (SignalMessage) received;
							addLocal(signalMessage.getSignal(), signalMessage.getBackup(), from);
						} else if(received.getMessageType().equals(MessageType.ADD_FINISHED)) {
							int totalAdds = addCounter.decrementAndGet();
							if(totalAdds == 0 && getMembersQuantity() > 1 && tempSignals.isEmpty() 
									&& degradedByMe.get()) {
								GenericMessage stateMessage = new GenericMessage(MessageType.NORMAL_MODE);
								Message message = new Message(null, getAddress(), stateMessage);
								sendMessage(message);
								degradedByMe.set(false);
							}
						} else if(received.getMessageType().equals(MessageType.REMOVE_BACKUP_SIGNAL)) {
							SignalMessage signalMessage = (SignalMessage) received;
							removeLocalBackup(signalMessage.getSignal(), signalMessage.getOwner());
							GenericMessage genericMessage = new GenericMessage(MessageType.ADD_FINISHED);
							Message message = new Message(from, getAddress(), genericMessage);
							sendMessage(message);
						} else if(received.getMessageType().equals(MessageType.ADD_BACKUP_SIGNAL)) {
							SignalMessage signalMessage = (SignalMessage) received;
							backupsMapPut(signalMessage.getOwner(), signalMessage.getSignal());
							GenericMessage genericMessage = new GenericMessage(MessageType.ADD_FINISHED);
							Message message = new Message(from, getAddress(), genericMessage);
							sendMessage(message);
						} else if(received.getMessageType().equals(MessageType.COMPARE_RESULTS)) {
							ResultMessage resultMessage = (ResultMessage) received;
							comparisons.add(resultMessage.getResult());
							resultLatch.countDown();
						}
					} else if(msg.getDest() == null) {
						if(received.getMessageType().equals(MessageType.ASK_INITIAL_SIGNALS)
								&& !getAddress().equals(from)) {
							//Le mando un porcentaje de mis señales
							initSignals(from);
						} else if(received.getMessageType().equals(MessageType.COMPARE_SIGNAL)) {
							SignalMessage signalMessage = (SignalMessage) received;
							prepareSignalsToCompare();
							findBest10(signalMessage.getSignal(), from);
						} else if(received.getMessageType().equals(MessageType.NORMAL_MODE)) {
							degraded.set(false);
							degradedByMe.set(false);
						} else if(received.getMessageType().equals(MessageType.DEGRADE_MODE)) {
							degraded.set(true);
						} else if(received.getMessageType().equals(MessageType.REORDERING_FINISHED)) {
							if(reorderingLatch != null) {
								reorderingLatch.countDown();
							}
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ViewAcceptedActions implements Runnable {
		@Override
		public void run() {
			try {
				while(!Thread.interrupted()) {
					views.take();
					if(getMembersQuantity() < getOldMembersQuantity()) {
						// se fue uno
						if(!isDegraded()) {
							degradeEveryone();
						}
						
						if(getMembersQuantity() == 1) {
							// Si quedo solo junto mis backups y mis señales en mis señales
							BlockingQueue<Signal> oldSignals = new LinkedBlockingQueue<Signal>();
							oldSignals.addAll(getAllSignals());
							oldSignals.addAll(getAllBackupSignals());
							signals.clear();
							backups.clear();
							signals.put(getAddress(), oldSignals);
						} else {
							Address goneMember = null;
							for(Address oldmember : prevView.getMembers()) {
								if(!channel.getView().containsMember(oldmember)) {
									goneMember = oldmember;
								}
							}
							
							if(goneMember != null) {
								BlockingQueue<Signal> mySignalsInGoneMember = signals.get(goneMember);
								BlockingQueue<Signal> goneMemberSignals = backups.get(goneMember);
								
								Address owner;
								Address backupOwner;
								if(mySignalsInGoneMember != null) {
									// necesito un nuevo backup
									for(Signal s : mySignalsInGoneMember) {
										do {
											backupOwner = getRandomAddress();
										} while (backupOwner.equals(getAddress()));
										
										signalsMapPut(backupOwner, s);
										
										prepareToSendSignal();
										SignalMessage backupSignalMessage = new SignalMessage(MessageType.ADD_BACKUP_SIGNAL, s, getAddress(), backupOwner);
										Message sendBackupSignalMessage = new Message(backupOwner, getAddress(), backupSignalMessage);
										sendMessage(sendBackupSignalMessage);
									}
									signals.remove(goneMember);
								}
								if(goneMemberSignals != null) {
									for(Signal s : goneMemberSignals) {
										do {
											owner = getRandomAddress();
											backupOwner = getRandomAddress();
										} while (owner.equals(backupOwner));
										
										prepareToSendSignal();
										SignalMessage signalMessage = new SignalMessage(MessageType.ADD_SIGNAL, s, owner, backupOwner);
										Message sendSignalMessage = new Message(owner, getAddress(), signalMessage);
										sendMessage(sendSignalMessage);
										
										prepareToSendSignal();
										SignalMessage backupSignalMessage = new SignalMessage(MessageType.ADD_BACKUP_SIGNAL, s, owner, backupOwner);
										Message sendBackupSignalMessage = new Message(backupOwner, getAddress(), backupSignalMessage);
										sendMessage(sendBackupSignalMessage);
									}
									
									backups.remove(goneMember);
								}
							}
						}
						reorderingFinishedInform();
					} else if (iAmNew(prevView)) {
						if(getMembersQuantity() > 1) {
							// se unio uno y soy yo -> pido señales
							GenericMessage signalAskMessage = new GenericMessage(MessageType.ASK_INITIAL_SIGNALS);
							Message sendSignalAskMessage = new Message(null, getAddress(), signalAskMessage);
							sendMessage(sendSignalAskMessage);
						}
					} else if (!iAmNew(prevView) && getMembersQuantity() == 2) {
						// si estaba degradado por estar solo, dejo de estar degradado
						if(tempSignals.isEmpty()) {
							degraded.set(false);
							degradedByMe.set(false);
						}
					}
					prevView = channel.getView();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

