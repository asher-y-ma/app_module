

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sxit.crawler.app.app91.A91AppCrawlerModeul;
import com.sxit.crawler.commons.SystemConstant;
import com.sxit.crawler.core.CrawlConfig;

public class App91 {

	/**
	 * @91 ÷ª˙÷˙ ÷
	 */
	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("classpath*:*-beans.xml");
		System.setProperty(SystemConstant.APP_HOME_KEY, "E:\\lex\\workspace\\crawler\\ecom_module\\data");
		System.setProperty(SystemConstant.MODULE_HOME_KEY, "E:\\lex\\workspace\\crawler\\ecom_module\\data");
		A91AppCrawlerModeul crawlerModule = new A91AppCrawlerModeul();
		CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setCrawlJobName(crawlerModule.getClass().getCanonicalName());
		crawlConfig.setAppId(41);
		crawlerModule.setCrawlConfig(crawlConfig);
		crawlerModule.execute();
	}

}
