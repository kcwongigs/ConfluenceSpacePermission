package com.igsl;

public enum DCSpacePermission {
	EDITSPACE("Edit space"),
	VIEWSPACE("View space"),
	EXPORTPAGE("Export page"),
	SETPAGEPERMISSIONS("Set page permissions"),
	REMOVEPAGE("Remove page"),
	EDITBLOG("Edit blog"),
	REMOVEBLOG("Remove blog"),
	COMMENT("Comment"),
	REMOVECOMMENT("Remove comment"),
	CREATEATTACHMENT("Create attachment"),
	REMOVEATTACHMENT("Remove attachment"),
	REMOVEMAIL("Remove email"),
	EXPORTSPACE("Export space"),
	SETSPACEPERMISSIONS("Set space permissions");
	DCSpacePermission(String description) {
		this.description = description;
	}
	private String description;
	public String getDescription() {
		return description;
	}
}