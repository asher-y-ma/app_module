package com.sxit.crawler.app.appstore;

import java.util.List;
import java.util.Map;

public class AppStoreConifg {

	private List<String> seedUrlList;
	private Map<String,String> catalogueId;
	public Map<String, String> getCatalogueId() {
		return catalogueId;
	}

	public void setCatalogueId(Map<String, String> catalogueId) {
		this.catalogueId = catalogueId;
	}

	public List<String> getSeedUrlList() {
		return seedUrlList;
	}

	public void setSeedUrlList(List<String> seedUrlList) {
		this.seedUrlList = seedUrlList;
	}
}
