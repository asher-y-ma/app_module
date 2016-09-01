package com.sxit.crawler.app;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.myapp.MyappCrawlerModel;
import com.sxit.crawler.core.CrawlConfig;

public class Myapp {

	/**
	 * @腾讯应用宝
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		MyappCrawlerModel crawlerModule = new MyappCrawlerModel();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
