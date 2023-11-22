package com.igsl.model;

public class ContentRestriction {
	private String operation;
	private Restriction restrictions;
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public Restriction getRestrictions() {
		return restrictions;
	}
	public void setRestrictions(Restriction restrictions) {
		this.restrictions = restrictions;
	} 
}
