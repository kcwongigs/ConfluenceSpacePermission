package com.igsl.model;

/**
 * Wrapper class to handle the complex structure mandated by Atlassian
 */
public class DCRestrictionWrapper {
	private String name;
	public DCRestrictionWrapper(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
