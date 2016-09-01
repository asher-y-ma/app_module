package com.sxit.crawler.app.hiapk;

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
import com.sxit.crawler.utils.JsoupUtils;

public class HiapkCrawlerModeul extends CrawlModule{
	private static Logger log = LoggerFactory.getLogger(HiapkCrawlerModeul.class);
	private final static String DEFAULT_JOB_NAME = HiapkCrawlerModeul.class.getSimpleName();
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	private String sql="select count(mmid) from tbas_mm where url= ? ";
	private DatatableOperator datatableOperator;
	private HiapkUrlUtils hiapkUrlUtils;
	private String categoryId;
	public HiapkCrawlerModeul(){
		super();
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		this.datatableOperator = new DatatableOperator(initDatatableConfig("TBAS_MM.xml"), jdbcTemplate);
		this.hiapkUrlUtils=new HiapkUrlUtils();
	}
	@Override
	public void execute() {
		extr();
	}
	private void extr(){
		HiapkUrlList hiapkUrlList=getUrlList();
		List<HiapkConfig> configList=hiapkUrlList.getList();
		for(int x=0;x<configList.size();x++){
			HiapkConfig config=configList.get(x);
			List<String> categoryIdList=config.getCategoryId();
			String seedUrl=config.getUrl();
			for(int y=0;y<categoryIdList.size();y++){
				categoryId=categoryIdList.get(y);
				for(int z=1;z<=100;z++){
					String url=seedUrl.replace("[first]", categoryId);
					url=url.replace("[second]", String.valueOf(z));
					FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(url,USER_AGENT_PROVIDER);
					try {
						FetchHTTP fetchHTTP = new FetchHTTP();
						fetchEntry=fetchHTTP.process(fetchEntry);
						if (fetchEntry.getResult() != null) {
							String html=fetchEntry.getResult().getPageContent().toString();
							if(!analyse(html)){
								log.warn("stop:"+url);
								break;
							}
							log.info(url);
						}
					} catch (Exception e) {
						continue;
					}
				}
			}
		}
	}
	
	/**
	 * get seeds url
	 * @return
	 */
	private HiapkUrlList getUrlList(){
		HiapkUrlList list=(HiapkUrlList) BeanHelper.getBean("hiapkUrlList");
		return list;
	}
	/**
	 * analyse page
	 * @param html
	 * @return 
	 */
	private boolean analyse(String html){
		List<String> list=analyseAppList(html);
		if(list.size()>0){
			analyseApp(list);
		}else{
			return false;
		}
		return true;
	}
	/**
	 * analyse detail url list
	 * @param url
	 */
	private List<String> analyseAppList(String html){
		//提取app列表，如果提取错误，即返还false
		List<String> list=new ArrayList<String>();
		try {
			Document doc=Jsoup.parse(html);
			Elements elements=doc.select("div.list_box>ul>li");
			for(Element element : elements){
				String url=element.select("div.left>a").attr("href");
				if(StringUtils.isNotBlank(url)){
					list.add(url);
				}
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		
		return list;
	}
	//extract data and save data
	private void analyseApp(List<String> list){
		List<Map<String, Object>> recordList = new ArrayList<Map<String,Object>>();
		for(int x=0;x<list.size();x++){
			try {
				String detailUrl=list.get(x);
				if(!exsitDownload(detailUrl)){
					FetchEntry fetchEntry = FetchEntityBuilder.buildFetchEntry(detailUrl,USER_AGENT_PROVIDER);
					FetchHTTP fetchHTTP = new FetchHTTP();
					fetchEntry=fetchHTTP.process(fetchEntry);
					if (fetchEntry.getResult() != null) {
						String html=fetchEntry.getResult().getPageContent().toString();
						Document doc=Jsoup.parse(html);
						Element element=doc.select("div.inner_left740").first();
						
						String mmid=null;
						String mmtitle=JsoupUtils.extrFirstText(element, "label#ctl00_AndroidMaster_Content_Apk_SoftName")+JsoupUtils.extrFirstText(element, "label#ctl00_AndroidMaster_Content_Apk_SoftVersionName");
						String version=JsoupUtils.extrFirstText(element, "label#ctl00_AndroidMaster_Content_Apk_SoftVersionName");
						String classname=JsoupUtils.extrFirstText(element, "span#ctl00_AndroidMaster_Content_Soft_CurrentCategory");
						String parentclassname=JsoupUtils.extrFirstText(element, "span#ctl00_AndroidMaster_Content_Soft_ParentCategory");
						String url=fetchEntry.getUrl();
						String headurl="http://cdn.market.hiapk.com/data";
						String bidname="";
						String webmmid=null;
						String domain="market.hiapk.com";
						String domainname="\u5B89\u5353\u5E02\u573A";
						String createdate=null;
						String downloadurl="http://apk.hiapk.com"+doc.select("div.butn_bg").select("div.butn_3").select("div.btnmarg>a").attr("href");
						if(downloadurl!=null){
							downloadurl=hiapkUrlUtils.getUrl(downloadurl,url);
							if(StringUtils.isNotBlank(downloadurl)){
								webmmid=StringUtils.substringAfterLast(downloadurl, "/");
								if(StringUtils.isBlank(webmmid)){
									return;
								}
								if(webmmid.indexOf("apk")==-1){
									return;
								}
							}else{
								return;
							}
						}
						String beginstr="";
						String endstr="";
//						Long classid=10L;
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
				}
			} catch (Exception e) {
				log.warn(e.getMessage());
			}
		}
		datatableOperator.saveData(recordList, true);
	}
	/**
	 * check data
	 * @param url
	 * @return
	 */
	private boolean exsitDownload(String url){
		return datatableOperator.existsData(sql,new Object[]{url});
	}
}
