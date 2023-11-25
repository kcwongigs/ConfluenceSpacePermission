package com.igsl.model;

public class DCRestrictionUpdate {
	private String operation;
	private DCRestrictionUpdateTarget restrictions;
	public DCRestrictionUpdate(String operation) {
		this.operation = operation;
		this.restrictions = new DCRestrictionUpdateTarget();
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public DCRestrictionUpdateTarget getRestrictions() {
		return restrictions;
	}
	public void setRestrictions(DCRestrictionUpdateTarget restrictions) {
		this.restrictions = restrictions;
	}
}
