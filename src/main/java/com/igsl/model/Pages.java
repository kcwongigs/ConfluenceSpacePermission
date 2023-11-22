package com.igsl.model;

import java.util.List;

public class Pages extends PagedWithCursor<Page> {
	private List<Page> results;
	@Override
	public List<Page> getPagedItems() {
		return results;
	}
	public List<Page> getResults() {
		return results;
	}
	public void setResults(List<Page> results) {
		this.results = results;
	}
}
