package ar.edu.itba.pod.legajo49015.message;

import org.jgroups.Address;

import ar.edu.itba.pod.api.Signal;

public class SignalMessage extends NodeMessage {
	private static final long serialVersionUID = 1L;
	private Signal signal;
	private Address owner;
	private Address backup;

	public SignalMessage(MessageType messageType, Signal signal, Address owner, Address backup) {
		super(messageType);
		this.signal = signal;
		this.owner = owner;
		this.backup = backup;
	}

	public Signal getSignal() {
		return signal;
	}
	
	public Address getOwner() {
		return owner;
	}
	
	public Address getBackup() {
		return backup;
	}
}
