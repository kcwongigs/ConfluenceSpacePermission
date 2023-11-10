package com.igsl.model;

import java.util.List;

public class GetPermissions extends Paged<GetPermission> {
	private List<GetPermission> results;
	@Override
	public List<GetPermission> getPagedItems() {
		return results;
	}
	public List<GetPermission> getResults() {
		return results;
	}
	public void setResults(List<GetPermission> results) {
		this.results = results;
	}
}
