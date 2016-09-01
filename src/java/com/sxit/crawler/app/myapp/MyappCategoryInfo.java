package com.sxit.crawler.app.myapp;

import java.util.List;

/**
 * 分类json对象
 * @author Administrator
 *
 */
public class MyappCategoryInfo {
	private List<MyappCategoryInfoValue> value;

	public List<MyappCategoryInfoValue> getValue() {
		return value;
	}

	public void setValue(List<MyappCategoryInfoValue> value) {
		this.value = value;
	}
}
