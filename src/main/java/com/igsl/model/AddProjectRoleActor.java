package com.igsl.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AddProjectRoleActor {
	@JsonIgnore
	private String projectKey;
	@JsonIgnore
	private String role;
	private List<String> groupId = new ArrayList<>();
	private List<String> user = new ArrayList<>();
	public AddProjectRoleActor(String projectKey, String role) {
		this.projectKey = projectKey;
		this.role = role;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Project: ").append(projectKey).append(" Role: ").append(role).append(" ");		
		for (String item : groupId) {
			sb.append("Group: ").append(item).append(" ");
		}
		for (String item : user) {
			sb.append("User: ").append(item).append(" ");
		}
		return sb.toString();
	}
	public List<String> getGroupId() {
		return groupId;
	}
	public void setGroupId(List<String> groupId) {
		this.groupId = groupId;
	}
	public List<String> getUser() {
		return user;
	}
	public void setUser(List<String> user) {
		this.user = user;
	}
}
