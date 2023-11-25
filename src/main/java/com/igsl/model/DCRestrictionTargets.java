package com.igsl.model;

public class DCRestrictionTargets {
	private DCRestrictionTargetUsers user;
	private DCRestrictionTargetGroups group;
	public DCRestrictionTargets() {
		user = new DCRestrictionTargetUsers();
		group = new DCRestrictionTargetGroups();
	}
	public DCRestrictionTargetUsers getUser() {
		return user;
	}
	public void setUser(DCRestrictionTargetUsers user) {
		this.user = user;
	}
	public DCRestrictionTargetGroups getGroup() {
		return group;
	}
	public void setGroup(DCRestrictionTargetGroups group) {
		this.group = group;
	}
}
