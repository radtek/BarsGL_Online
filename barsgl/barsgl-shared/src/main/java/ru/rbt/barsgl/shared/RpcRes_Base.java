package ru.rbt.barsgl.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class RpcRes_Base<T extends Serializable> implements Serializable, IsSerializable {
	
	private String message;
	private boolean isError;
	private T result;
	
	@SuppressWarnings("unused")
	private RpcRes_Base() {}
	
	public RpcRes_Base(T result, boolean isError, String message) {
		this.result = result;
		this.isError = isError;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

	public boolean isError() {
		return isError;
	}

	public T getResult() {
		return result;
	}
}
