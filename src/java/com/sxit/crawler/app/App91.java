package com.sxit.crawler.app;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.app91.A91AppCrawlerModeul;
import com.sxit.crawler.core.CrawlConfig;

public class App91 {

	/**
	 * @91手机助手
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		A91AppCrawlerModeul crawlerModule = new A91AppCrawlerModeul();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}

}
