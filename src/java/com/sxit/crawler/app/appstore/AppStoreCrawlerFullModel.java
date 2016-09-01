package com.sxit.crawler.app.appstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.commons.jdbc.DatatableOperator;
import com.sxit.crawler.core.fetch.FetchEntityBuilder;
import com.sxit.crawler.core.fetch.FetchEntry;
import com.sxit.crawler.core.fetch.FetchHTTP;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class AppStoreCrawlerFullModel extends CrawlModule {
	private static Logger log = LoggerFactory.getLogger(AppStoreCrawlerFullModel.class);
	private final static String DEFAULT_JOB_NAME = AppStoreCrawlerFullModel.class.getSimpleName();
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	private AppStoreConifg config;
	private DatatableOperator datatableOperator;
	
	private enum Letter {
		A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z
	};

	public AppStoreCrawlerFullModel() {
		super();
		JdbcTemplate jdbcTemplate = (JdbcTemplate) BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(initDatatableConfig("TBAS_MM_APPSTORE.xml"), jdbcTemplate);
		config = (AppStoreConifg) BeanHelper.getBean("appStoreConifg");
	}

	@Override
	public void execute() {
		log.info("start job..............");
		extr();
		//hotapp();
		log.info("................end job");
	}
	/**
	 * 只爬取相关类别的热门应用 250*23
	 *
	 */
	private void hotapp() {
		// http://itunes.apple.com/cn/genre/mobile-software-applications/id36?mt=8
		List<String> seedUrlList = getUrlList();
		for (int x = 0; x < seedUrlList.size(); x++) {
			String seedUrl = seedUrlList.get(x);
			Map<String,String>  catalogueMap = getCategoryList(seedUrl);
			if(catalogueMap!=null){
				int catalogue=0;
				int catalogueCount=catalogueMap.size();
				for(String classname:catalogueMap.keySet()){
					String catologueUrl=catalogueMap.get(classname);
					catalogue++;
					log.info("catalogue["+catalogue+"/"+catalogueCount+"],"+catologueUrl);
							List<String> appDetailUrlList = getAppUrlList(catologueUrl);
							for (int z = 0; z < appDetailUrlList.size(); z++) {
								String appDetailUrl = appDetailUrlList.get(z);
								log.info("appDetailUrl["+(z+1)+"/"+appDetailUrlList.size()+"],"+appDetailUrl);
								extrDataFromUrl(appDetailUrl,classname);
							}

				}
				
			}
		}
	}
	private void extr() {
		// http://itunes.apple.com/cn/genre/mobile-software-applications/id36?mt=8
		List<String> seedUrlList = getUrlList();
		for (int x = 0; x < seedUrlList.size(); x++) {
			String seedUrl = seedUrlList.get(x);
			Map<String,String>  catalogueMap = getCategoryList(seedUrl);
			if(catalogueMap!=null){
				int catalogue=0;
				int catalogueCount=catalogueMap.size();
				for(String classname:catalogueMap.keySet()){
					String catologueUrl=catalogueMap.get(classname);
					catalogue++;
					for (Letter letter : Letter.values()) {
						String letterUrl = catologueUrl + "&letter=" + letter;
						int pageNum = getPageNum(letterUrl);
						//log.info("pageNum--->" + pageNum);
						for (int page = 1; page <= pageNum; page++) {
							String pageUrl = letterUrl + "&page=" + page;
							log.info("catalogue["+catalogue+"/"+catalogueCount+"],letter["+letter+"],page["+page+"/"+pageNum+"],"+pageUrl);
							List<String> appDetailUrlList = getAppUrlList(pageUrl);
							for (int z = 0; z < appDetailUrlList.size(); z++) {
								String appDetailUrl = appDetailUrlList.get(z);
								log.info("appDetailUrl["+(z+1)+"/"+appDetailUrlList.size()+"],"+appDetailUrl);
								extrDataFromUrl(appDetailUrl,classname);
							}
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
	 * get Catalogue from app store
	 * 
	 * @return
	 */
	private Map<String,String> getCategoryList(String url) {
		Map<String,String> nameurl_map=new HashMap<String,String>();
		Map<String,String> nameid_map=new HashMap<String,String>();
		try {
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, USER_AGENT_PROVIDER);
			FetchHTTP FetchHTTP = new FetchHTTP();
			fetchEntry = FetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
				Elements elements = doc.select("div#genre-nav>div.grid3-column>ul>li");
				for (Element element : elements) {
					String href = element.select("a").attr("href");
					String name=element.select("a").first().text();
					if (StringUtils.isNotBlank(href)) {
						nameurl_map.put(name, href);
					}
					if (StringUtils.isNotBlank(href) || StringUtils.isNotBlank(name)) {
						String id=StringUtils.substringBetween(href,"id","?");
						if(StringUtils.isNotBlank(id)){
							nameid_map.put(name, id);
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		this.config.setCatalogueId(nameid_map);
		return nameurl_map;
	}

	/**
	 * get page Number from pageUrl
	 * 
	 * @return
	 */
	private int getPageNum(String url) {
		int num = -1;
		try {
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, USER_AGENT_PROVIDER);
			FetchHTTP FetchHTTP = new FetchHTTP();
			fetchEntry = FetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
				Elements elements = doc.select("div#selectedgenre").select("ul.list").select("ul.paginate").get(0).select("li");
				int lenth = elements.size();
				String str = elements.get(lenth - 2).text();
				num = Integer.parseInt(str);
			}
		} catch (Exception e) {
			log.error("getpagenum error!",e);
		}
		return num;
	}

	private List<String> getAppUrlList(String url) {
		List<String> list = new ArrayList<String>();
		try {
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, USER_AGENT_PROVIDER);
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry = fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
				Elements colElments = doc.select("div#selectedcontent>div>ul>li");
				for (Element element : colElments) {
					String appDetailUrl = element.select("a").attr("href");
					if (StringUtils.isNotBlank(appDetailUrl)) {
						list.add(appDetailUrl);
					}
				}
			}
		} catch (Exception e) {
			log.error("getAppUrlList error!",e);
		}
		return list;
	}


	private void extrDataFromUrl(String url,String classname) {
		try {
			//log.info("url-->"+url);
			FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url, USER_AGENT_PROVIDER);
			FetchHTTP fetchHTTP = new FetchHTTP();
			fetchEntry = fetchHTTP.process(fetchEntry);
			if (fetchEntry.getResult() != null) {
				analyseApp(fetchEntry,classname);
			}
		} catch (Exception e) {
			log.error("extrDataFromUrl error!"+e);
		}
		
	}

	private void analyseApp(FetchEntry fetchEntry,String parentclassname) {
		try {
			Document doc = Jsoup.parse(fetchEntry.getResult().getPageContent().toString());
			String mmid = null;
			String mmtitle = doc.select("h1").text();
			String classname =doc.select("li.genre>a").text();
			String version =doc.select("div#left-stack>div").get(0).select("ul>li").text();
			if(version!=null){
				if(version.indexOf("\u7248\u672C\u003A\u0020")!=-1){
					int start=version.indexOf("\u7248\u672C\u003A\u0020");
					version=version.substring(start+4, version.length());
					int end=version.indexOf("\u0020");
					version=version.substring(0,end);
				}
			}
			String url = fetchEntry.getUrl();
			String headurl = "http://itunes.apple.com/cn/app/";
			String bidname = null;//"notfound";
			String webmmid = null;
			String domain ="apple.com";//"itunes.apple.com";
			String domainname = "app store";//"APPSTORE";
			String createdate = null;
			String downloadurl =null;
			downloadurl = doc.select("div#left-stack>div").get(0).select("a>div.artwork>img").attr("src");
			
			if(StringUtils.isNotBlank(downloadurl)){
				if(downloadurl.indexOf("mzstatic.com")!=-1 || downloadurl.indexOf(".jpg")!=-1){
					//http://a3.mzstatic.com/us/r30/Purple/e0/64/35/mzl.stjoxxar.175x175-75.jpg
					webmmid=StringUtils.substringAfter(downloadurl, "mzstatic.com/");
					webmmid=StringUtils.substringBeforeLast(webmmid, ".jpg");
					webmmid=StringUtils.substringBeforeLast(webmmid, ".");
				}
			}
			
			String beginstr ="apple.com/";
			String endstr = ".512x512-75.jpg";
			/*Elements e=doc.select("div.download>a");
			if(e!=null&e.size()>1){
				downloadurl=StringUtils.substringBetween(e.get(1).attr("onclick"), "location='", "';");
			}else{
				downloadurl="http://www.apple.com.cn/itunes/download/?id="+webmmid;
			}
			webmmid=StringUtils.substringBetween(url,beginstr,endstr);*/
			
			String classid=config.getCatalogueId().get(parentclassname);
			
			if(StringUtils.isBlank(classid)){
				classid="10";
			}
			Long featuretype = 1L;

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
			row.put("CLASSID", classid);
			row.put("FEATURETYPE", featuretype);
//			List<Map<String, Object>> recordList = new ArrayList<Map<String, Object>>();
//			recordList.add(row);
//			datatableOperator.saveData(recordList, true);
			datatableOperator.saveColumnData(row, true);
		} catch (Exception e) {
			log.error("analyseApp error!",e);
		}
	}
}
