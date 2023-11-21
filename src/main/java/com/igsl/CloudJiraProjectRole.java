package com.igsl;

public enum CloudJiraProjectRole {
	ADMINISTRATOR("Administrator"),
	MEMBER("Member"),
	VIEWER("Viewer");
	CloudJiraProjectRole(String description) {
		this.description = description;
	}
	private String description;
	public String getDescription() {
		return description;
	}
}