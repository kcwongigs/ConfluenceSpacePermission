package com.igsl.model;

public class DCRestrictionUpdateTargetGroup {
	private String type;
	private String name;
	public DCRestrictionUpdateTargetGroup(String name) {
		this.type = "group";
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
