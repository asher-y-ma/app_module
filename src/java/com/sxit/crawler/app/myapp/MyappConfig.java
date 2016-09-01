package com.sxit.crawler.app.myapp;

import java.util.List;

public class MyappConfig {
	private String pageUrl;
	private String detailUrl;
	private List<String> seedUrlList;

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public String getDetailUrl() {
		return detailUrl;
	}

	public void setDetailUrl(String detailUrl) {
		this.detailUrl = detailUrl;
	}

	public List<String> getSeedUrlList() {
		return seedUrlList;
	}

	public void setSeedUrlList(List<String> seedUrlList) {
		this.seedUrlList = seedUrlList;
	}
}
