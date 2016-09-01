package com.sxit.crawler.app;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.mm10086.Mm10086IphoneCrawlerModeul;
import com.sxit.crawler.core.CrawlConfig;

public class Mm10086Iphone {

	/**
	 * @移动mm商城__iphone app
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		Mm10086IphoneCrawlerModeul crawlerModule = new Mm10086IphoneCrawlerModeul();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
