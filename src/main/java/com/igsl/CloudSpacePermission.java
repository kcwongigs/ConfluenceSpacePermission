package com.igsl;

public enum CloudSpacePermission {
	/**
	 * The v1 REST API requires using a key (e.g. read or update) with a target (e.g. space or page)
	 * But the documentation is not complete, not all valid key/target pairings are listed. 
	 * 
	 * We are not going to find out the full list by experimentation.
	 * We will only support those we need
	 */
	/* Supposed full list of keys
	administer, 
	archive, 
	copy, 
	create, 
	delete, 
	export, 
	move, 
	purge, 
	purge_version, 
	read, 
	restore, 
	restrict_content, 
	update, 
	use;
	*/
	/*
	 * Supposed full list of targets
	 * page, 
	 * blogpost, 
	 * comment, 
	 * attachment, 
	 * space
	 */
	administer("administer", "space", "Administer space"),
	readspace("read", "space", "Read space"),
	createpage("create", "page", "Create or update page");
	private String description;
	private String key;
	private String target;
	CloudSpacePermission(String key, String target, String description) {
		this.key = key;
		this.target = target;
		this.description = description;
	}
	public String getDescription() {
		return this.description;
	}
	public String getKey() {
		return this.key;
	}
	public String getTarget() {
		return this.target;
	}
}