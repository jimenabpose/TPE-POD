package ar.edu.itba.pod.legajo49015.message;

import ar.edu.itba.pod.api.Result;

public class ResultMessage extends NodeMessage {
	private static final long serialVersionUID = 1L;
	private Result result;

	public ResultMessage(MessageType messageType, Result result) {
		super(messageType);
		this.result = result;
	}

	public Result getResult() {
		return result;
	}
}
