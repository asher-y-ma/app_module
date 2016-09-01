package com.sxit.crawler.app.myapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.gson.Gson;
import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class MyappCrawlerModel extends CrawlModule {
	private static Logger log = LoggerFactory.getLogger(MyappCrawlerModel.class);
	private final static String DEFAULT_JOB_NAME = MyappCrawlerModel.class.getSimpleName();
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	private MyappConfig config;
	private MyappUrlUtils myappUrlUtils;
	private DatatableOperator datatableOperator;
	public MyappCrawlerModel() {
		super();
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(initDatatableConfig("TBAS_MM.xml"), jdbcTemplate);
		myappUrlUtils=new MyappUrlUtils();
		config=(MyappConfig) BeanHelper.getBean("myappConfig");
	}

	@Override
	public void execute() {
		log.info("start job..............");
		extr();
		log.info("................end job");
	}

	private void extr() {
		String pageUrl = config.getPageUrl();
		List<String> seedUrlList = getUrlList();
		for (int x = 0; x < seedUrlList.size(); x++) {
			String categoryUrl = seedUrlList.get(x);
			List<MyappCategoryInfoValue> myappCategoryInfoValueList = getCategoryList(categoryUrl);
			if (myappCategoryInfoValueList != null) {
				for (int y = 0; y < myappCategoryInfoValueList.size(); y++) {
					MyappCategoryInfoValue myappCategoryInfoValue = myappCategoryInfoValueList.get(y);
					String icfa = myappCategoryInfoValue.getIcfa();
					String categoryId = myappCategoryInfoValue.getCategoryid();
					// get total number
					if(!"10013".equals(categoryId)){
						int num = getPageNo(categoryId, icfa, pageUrl);
						// analyze app json
						for (int z = 1; z < num; z++) {
							String json = getJson(categoryId, icfa, String.valueOf(z), pageUrl);
							Myappdetail myappdetail = getObjectFromJson(json);
							analyseApp(myappdetail, categoryId);
						}
					}
				}
			}
		}
	}

	/**
	 * get seeds url
	 * 
	 * @return
	 */
	private List<String> getUrlList() {
		List<String> list = config.getSeedUrlList();
		return list;
	}

	/**
	 * get category list
	 * 
	 * @return
	 */
	private List<MyappCategoryInfoValue> getCategoryList(String categoryUrl) {
		List<MyappCategoryInfoValue> list = null;
		try {
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(categoryUrl, USER_AGENT_PROVIDER);
			fetchEntry.setUrl(categoryUrl);
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry = fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				String json = fetchEntry.getResult().getPageContent().toString();
				if (StringUtils.isNotBlank(json)) {
					MyappCategory myappCategory = new Gson().fromJson(json, MyappCategory.class);
					if (myappCategory != null) {
						MyappCategoryInfo myappCategoryInfo = myappCategory.getInfo();
						if (myappCategoryInfo != null) {
							list = myappCategoryInfo.getValue();
						}
					}
				}

			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return list;
	}

	/**
	 * get page Number
	 * 
	 * @return
	 */
	private int getPageNo(String categoryid, String icfa, String seedUrl) {
		int number = 0;
		String json = getJson(categoryid, icfa, "1", seedUrl);
		Myappdetail myappdetail = getObjectFromJson(json);
		if (myappdetail != null) {
			MyappdetailInfo myappdetailInfo = myappdetail.getInfo();
			if (myappdetailInfo != null) {
				number = myappdetailInfo.getPageCount();
			}
		}
		return number;
	}

	private String getJson(String categoryid, String icfa, String pageNo, String seedUrl) {
		String json = null;
		try {
			String url = seedUrl.replace("[first]", categoryid);
			url = url.replace("[second]", icfa);
			url = url.replace("[third]", pageNo);
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, USER_AGENT_PROVIDER);
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry = fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				json = fetchEntry.getResult().getPageContent().toString();
				log.info(url);
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return json;
	}

	private Myappdetail getObjectFromJson(String json) {
		return new Gson().fromJson(json, Myappdetail.class);
	}

	private void analyseApp(Myappdetail myappdetail,String categoryId) {
		try {
			List<Map<String,Object>> recordList=new ArrayList<Map<String,Object>>();
			Myappdetail appdetail = myappdetail;
			if (appdetail != null) {
				MyappdetailInfo myappdetailInfo = appdetail.getInfo();
				if (myappdetailInfo != null) {
					List<MyappdetailInfoValue> list = myappdetailInfo.getValue();
					if (list != null) {
						for (int x = 0; x < list.size(); x++) {
							try {
								MyappdetailInfoValue app = list.get(x);
								if (app != null) {
									String mmid=null;
									String mmtitle=app.getSoftname();
									String classname=app.getCname();
									String version=app.getVersionname();
									String parentclassname=app.getCname();
									String url=buildUrl(config.getDetailUrl(), app.getAppid(), app.getIcfa());
									String headurl="http://down.myapp.com";
									String bidname="";
									String webmmid=null;
									String domain="myapp.com";
									String domainname="\u817E\u8BAF\u5E94\u7528\u5B9D";
									String createdate=null;
									String downloadurl=getDownloadUrl(url);
									if(downloadurl!=null){
										downloadurl=myappUrlUtils.getUrl(downloadurl);
										if(StringUtils.isNotBlank(downloadurl)){
											webmmid=StringUtils.substringAfterLast(downloadurl, "/");
											if(StringUtils.isBlank(webmmid)){
												continue;
											}
											if(webmmid.indexOf("apk")==-1){
												continue;
											}
										}else{
											continue;
										}
									}else{
										continue;
									}
									String beginstr="";
									String endstr="";
//									Long classid=10L;
									Long featuretype=2L;
									
									Map<String, Object> row = new HashMap<String, Object>();
									row.put("MMID", mmid);
									row.put("MMTITLE", mmtitle);
									row.put("VERSION", version);
									row.put("CLASSNAME", classname);
									row.put("PARENTCLASSNAME", parentclassname);
									row.put("URL", url);
									row.put("HEADURL", headurl);
									row.put("BIDNAME", bidname);
									row.put("WEBMMID", webmmid);
									row.put("DOMAIN", domain);
									row.put("DOMAINNAME", domainname);
									row.put("CREATEDATE", createdate);
									row.put("DOWNLOADURL", downloadurl);
									row.put("BEGINSTR", beginstr);
									row.put("ENDSTR", endstr);
									row.put("CLASSID", categoryId);
									row.put("FEATURETYPE", featuretype);
									recordList.add(row);
								}
							} catch (Exception e) {
								log.warn(e.getMessage());
								continue;
							}
						}
					}
				}
			}
			datatableOperator.saveData(recordList, true);
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
	}
	private String buildUrl(String url,String appid,String icfa){
		String newUrl=url.replace("[first]", appid);
		newUrl=newUrl.replace("[second]", icfa);
		return newUrl;
	}
	private String getDownloadUrl(String detailUrl){
		String downloadUrl=null;
		try {
			FetchEntry fetchEntry=FetchEntityBuilder.buildFetchEntry(detailUrl, USER_AGENT_PROVIDER);
			FetchHTTP fetchHTTP=new FetchHTTP();
			fetchEntry=fetchHTTP.process(fetchEntry);
			if(fetchEntry.getResult()!=null){
				String detailHtml=fetchEntry.getResult().getPageContent().toString();
				Document doc=Jsoup.parse(detailHtml);
				downloadUrl="http://android.myapp.com"+doc.select("a.downtopc").attr("href");
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return downloadUrl;
	}
}
