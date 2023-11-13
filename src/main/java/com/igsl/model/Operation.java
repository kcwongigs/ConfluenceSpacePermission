package com.igsl.model;

public class Operation {
	private String target;	// Used by v1 API to set space permission
	private String targetType;	// Used by v2 API to get space permissions
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
	public String getTargetType() {
		return targetType;
	}
	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}
}
