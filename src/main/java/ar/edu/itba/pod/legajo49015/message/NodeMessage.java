package ar.edu.itba.pod.legajo49015.message;

import java.io.Serializable;


public abstract class NodeMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private MessageType messageType;
	
	public NodeMessage(MessageType messageType) {
		this.messageType = messageType;
	}

	public MessageType getMessageType() {
		return messageType;
	}
}
