package com.igsl.model;

public class AddPermission {
	private Subject subject;
	private Operation operation;
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (subject != null) {
			sb.append(" Subject type: ").append(subject.getType());
			sb.append(" Subject id: ").append(subject.getIdentifier());
		}
		if (operation != null) {
			sb.append(" Permission target: ").append(operation.getTarget());
			sb.append(" Permission key: ").append(operation.getKey());
		}
		return sb.toString();
	}
	public Subject getSubject() {
		return subject;
	}
	public void setSubject(Subject subject) {
		this.subject = subject;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
