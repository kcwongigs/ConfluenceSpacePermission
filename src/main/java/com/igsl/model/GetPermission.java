package com.igsl.model;

public class GetPermission {
	private String id;
	private Principal principal;
	private Operation operation;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Principal getPrincipal() {
		return principal;
	}
	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
