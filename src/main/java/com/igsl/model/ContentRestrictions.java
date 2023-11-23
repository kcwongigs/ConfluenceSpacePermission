package com.igsl.model;

import java.util.List;

public class ContentRestrictions extends PagedWithNumber<ContentRestriction> {
	private List<ContentRestriction> results;
	public List<ContentRestriction> getResults() {
		return results;
	}
	public void setResults(List<ContentRestriction> results) {
		this.results = results;
	}
	@Override
	public List<ContentRestriction> getPagedItems() {
		return results;
	}
}
