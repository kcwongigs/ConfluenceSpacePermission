package com.igsl.model;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PagedWithNumber<T> {
	private static final Logger LOGGER = LogManager.getLogger();
	public abstract List<T> getPagedItems();
}
