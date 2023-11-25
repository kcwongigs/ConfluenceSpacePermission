package com.igsl.model;

import java.util.List;

public class DCPages extends PagedWithNumber<DCPage> {
	private List<DCPage> results;
	@Override
	public List<DCPage> getPagedItems() {
		return results;
	}
	public List<DCPage> getResults() {
		return results;
	}
	public void setResults(List<DCPage> results) {
		this.results = results;
	}
}
