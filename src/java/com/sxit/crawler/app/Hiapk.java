package com.sxit.crawler.app;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.hiapk.HiapkCrawlerModeul;
import com.sxit.crawler.core.CrawlConfig;

public class Hiapk {

	/**
	 * @安卓市场
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		HiapkCrawlerModeul crawlerModule = new HiapkCrawlerModeul();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}
}
