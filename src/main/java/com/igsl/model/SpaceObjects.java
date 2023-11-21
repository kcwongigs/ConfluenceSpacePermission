package com.igsl.model;

import java.util.List;

public class SpaceObjects extends PagedWithCursor<SpaceObject> {
	private List<SpaceObject> results;
	@Override
	public List<SpaceObject> getPagedItems() {
		return results;
	}
	public List<SpaceObject> getResults() {
		return results;
	}
	public void setResults(List<SpaceObject> results) {
		this.results = results;
	}
}
