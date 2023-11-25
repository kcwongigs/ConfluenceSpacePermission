package com.igsl.model;

public class DCRestrictionUpdateTargetUser {
	private String type;
	private String username;
	public DCRestrictionUpdateTargetUser(String username) {
		this.type = "user";
		this.username = username;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
}
