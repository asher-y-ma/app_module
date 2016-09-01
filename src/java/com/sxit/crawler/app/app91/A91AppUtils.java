package com.sxit.crawler.app.app91;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.sxit.crawler.commons.BeanHelper;
import com.sxit.crawler.core.fetch.SimpleUserAgentProvider;
import com.sxit.crawler.core.fetch.UserAgentProvider;
import com.sxit.crawler.module.CrawlModule;

public class A91AppUtils extends CrawlModule{

	private static Logger log = LoggerFactory.getLogger(A91AppUtils.class);
	private final static String DEFAULT_JOB_NAME = A91AppUtils.class.getSimpleName();
	public static final String USER_AGENT_STRING = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.72 Safari/537.36";
	public static final UserAgentProvider USER_AGENT_PROVIDER = new SimpleUserAgentProvider(DEFAULT_JOB_NAME, USER_AGENT_STRING);
	private A91AppUrlUtils a91AppUrlUtils;
	private String updateSql="update TBAS_MM set HEADURL='http://hiapk.91rb.com',BIDNAME='',WEBMMID=?,DOMAIN='91rb.com',DOWNLOADURL=?,BEGINSTR='',ENDSTR='',FEATURETYPE=2  where mmid=?";
	public A91AppUtils(){
		a91AppUrlUtils=new A91AppUrlUtils();
	}
	@Override
	public void execute() {
		covertUrl();
	}
	private void covertUrl(){
		List<Map<String,Object>> list=getUrl();
		if(list!=null){
			convertData(list);
			if(list.size()>0){
				covertUrl();
			}
		}
	}
	private void convertData(List<Map<String,Object>> list){
		JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
		String errMmid=null;
		for(int x=0;x<list.size();x++){
			try {
				Map<String,Object> data=list.get(x);
				String mmid=String.valueOf(data.get("mmid")) ;
				errMmid=mmid;
				String url=String.valueOf(data.get("downloadurl"));
				String downloadurl=a91AppUrlUtils.getUrl(url);
				String webmmid=null;
				
				if(StringUtils.isNotBlank(downloadurl)){
					log.info("--->"+url);
					log.info(downloadurl);
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
				jdbcTemplate.update(updateSql, new Object[]{webmmid,downloadurl,mmid});
			} catch (Exception e) {
				log.warn(e.getMessage());
				log.error("error mmid:"+errMmid);
				delete(errMmid,jdbcTemplate);
			}
			
		}
	}
	private void delete(String mmid,JdbcTemplate jdbcTemplate){
		String sql="delete from TBAS_MM where mmid=?";
		jdbcTemplate.update(sql, new Object[]{mmid});
	}
	@SuppressWarnings("unchecked")
	private List<Map<String,Object>> getUrl(){
		try {
			String sql = "select MMID,DOWNLOADURL from TBAS_MM where headurl = 'http://bbx2.sj.91.com/soft/phone/detail.aspx' and rownum<=10";
			JdbcTemplate jdbcTemplate = (JdbcTemplate)BeanHelper.getBean("jdbcTemplate");
			List<Map<String,Object>> list=jdbcTemplate.query(sql, new RowMapper(){
				public Object mapRow(ResultSet rs, int idx) throws SQLException {
					Map<String,Object> map=new HashMap<String,Object>();
					for(int x=1;x<=rs.getMetaData().getColumnCount();x++){
						map.put(rs.getMetaData().getColumnName(x).toLowerCase(),rs.getObject(x) );
					}
					return map;
				}});
			return list;
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return null;
	}
}
