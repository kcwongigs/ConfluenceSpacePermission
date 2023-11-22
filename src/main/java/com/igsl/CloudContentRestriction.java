package com.igsl;

public enum CloudContentRestriction {
	administer("Administer"), 
	copy("Copy"), 
	create("Create"), 
	delete("Delete"), 
	export("Export"), 
	move("Move"), 
	purge("Purge"), 
	purge_version("Purge version"), 
	read("Read"), 
	restore("Restore"), 
	update("Update"), 
	use("Use");
	private String description;
	CloudContentRestriction(String description) {
		this.description = description;
	}
	public String getDescription() {
		return this.description;
	}
}