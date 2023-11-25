package com.igsl.model;

import java.util.ArrayList;
import java.util.List;

public class DCRestrictionUpdateTarget {
	private List<DCRestrictionUpdateTargetUser> user = new ArrayList<>();
	private List<DCRestrictionUpdateTargetGroup> group = new ArrayList<>();
	public List<DCRestrictionUpdateTargetUser> getUser() {
		return user;
	}
	public void setUser(List<DCRestrictionUpdateTargetUser> user) {
		this.user = user;
	}
	public List<DCRestrictionUpdateTargetGroup> getGroup() {
		return group;
	}
	public void setGroup(List<DCRestrictionUpdateTargetGroup> group) {
		this.group = group;
	}
}
