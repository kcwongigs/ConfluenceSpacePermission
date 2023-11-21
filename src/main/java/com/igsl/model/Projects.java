package com.igsl.model;

import java.util.List;

public class Projects extends PagedWithNumber<Project> {
	private List<Project> values;
	@Override
	public List<Project> getPagedItems() {
		return values;
	}
	public List<Project> getValues() {
		return values;
	}
	public void setValues(List<Project> values) {
		this.values = values;
	}
}
