package com.igsl.model;

public class DCRestriction {
	private String operation;
	private DCRestrictionTargets restrictions;
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public DCRestrictionTargets getRestrictions() {
		return restrictions;
	}
	public void setRestrictions(DCRestrictionTargets restrictions) {
		this.restrictions = restrictions;
	}
}
