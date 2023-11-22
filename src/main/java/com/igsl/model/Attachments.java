package com.igsl.model;

import java.util.List;

public class Attachments extends PagedWithCursor<Attachment> {
	private List<Attachment> results;
	@Override
	public List<Attachment> getPagedItems() {
		return results;
	}
	public List<Attachment> getResults() {
		return results;
	}
	public void setResults(List<Attachment> results) {
		this.results = results;
	}
}
