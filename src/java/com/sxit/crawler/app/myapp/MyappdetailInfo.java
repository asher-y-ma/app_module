package com.sxit.crawler.app.myapp;

import java.util.List;


public class MyappdetailInfo {
	private int pageCount;
	private int pageNo;
	private int pageSize;
	private List<MyappdetailInfoValue> value;
	private int nextPageIndex;

	public int getPageCount() {
		return pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public List<MyappdetailInfoValue> getValue() {
		return value;
	}

	public void setValue(List<MyappdetailInfoValue> value) {
		this.value = value;
	}

	public int getNextPageIndex() {
		return nextPageIndex;
	}

	public void setNextPageIndex(int nextPageIndex) {
		this.nextPageIndex = nextPageIndex;
	}
}
