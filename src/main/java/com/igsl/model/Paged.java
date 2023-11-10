package com.igsl.model;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.igsl.Log;

public abstract class Paged<T> {
	public static final String CURSOR_PARAMETER = "cursor";
	private static final Logger LOGGER = LogManager.getLogger();
	private Links _links;
	public abstract List<T> getPagedItems();
	public String getNextPageCursor() {
		if (_links != null && _links.getNext() != null) {
			try {
				URIBuilder builder = new URIBuilder(_links.getNext());
				for (NameValuePair pair : builder.getQueryParams()) {
					if (CURSOR_PARAMETER.equals(pair.getName())) {
						return pair.getValue();
					}
				}
			} catch (URISyntaxException usex) {
				Log.error(LOGGER, "Failed to parse cursor from next URL", usex);
			}
		}
		return null;
	}
	public Links get_links() {
		return _links;
	}
	public void set_links(Links _links) {
		this._links = _links;
	}
}
