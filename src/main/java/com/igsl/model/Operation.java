package com.igsl.model;

public class Operation {
	public static final String TARGET_SPACE = "space";
	private String target = TARGET_SPACE;
	private String key;
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
