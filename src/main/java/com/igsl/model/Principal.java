package com.igsl.model;

public class Principal {
	public static final String TYPE_USER = "user";
	public static final String TYPE_GROUP = "group";
	public static final String TYPE_ROLE = "role";
	private String id;
	private String type;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
