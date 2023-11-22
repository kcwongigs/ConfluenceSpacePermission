package com.igsl.model;

public class AddRestriction {
	
	private String contentId;
	private String restriction;
	private boolean user;
	private String target;
	
	public AddRestriction(String contentId, String restriction, boolean user, String target) {
		this.contentId = contentId;
		this.restriction = restriction;
		this.user = user;
		this.target = target;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb	.append("Content: ").append(contentId)
			.append(" Restriction: ").append(restriction)
			.append(" ").append((user? "User" : "Group")).append(": ").append(target);
		return sb.toString();
	}
}
