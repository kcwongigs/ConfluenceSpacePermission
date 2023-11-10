package com.igsl.model;

import java.util.ArrayList;
import java.util.List;

public class DCDataObject {
	private String spaceKey;
	private String permission;
	private String target;
	public DCDataObject(String spaceKey, String permission, String target) {
		this.spaceKey = spaceKey;
		this.permission = permission;
		this.target = target;
	}
	public List<String> format() {
		List<String> list = new ArrayList<>();
		list.add(permission);
		list.add(target);
		list.add(spaceKey);
		return list;
	}
	@Override
	public String toString() {
		return "Target: " + target + " Permission: " + permission + " Space: " + spaceKey;
	}
	public String getSpaceKey() {
		return spaceKey;
	}
	public String getPermission() {
		return permission;
	}
	public String getTarget() {
		return target;
	}
}
