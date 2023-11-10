package com.igsl.model;

public class Subject {
	public static final String SUBJECT_TYPE_USER = "user";
	public static final String SUBJECT_TYPE_GROUP = "group";
	private String type;
	private String identifier;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
