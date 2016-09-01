package com.sxit.crawler.app;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.appstore.AppStoreCrawlerFullModel;
import com.sxit.crawler.app.appstore.AppStoreCrawlerModel;
import com.sxit.crawler.core.CrawlConfig;

public class AppStoreFull {

	/**
	 * @app store(china)
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		AppStoreCrawlerFullModel crawlerModule = new AppStoreCrawlerFullModel();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
