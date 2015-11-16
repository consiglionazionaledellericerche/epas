package manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http.Header;

import java.util.HashMap;
import java.util.Map;

public class ReportCentreManager {
	
	private final static Logger log = LoggerFactory.getLogger(ReportCentreManager.class);
	
	/**
	 * 
	 * @param map
	 * @return la stringa contenente la action da cui viene lanciata la richiesta di report causa errori o malfunzionamenti
	 */
	public String getActionFromRequest(HashMap<String, Header> map){
		String action = "";
		for (Map.Entry<String, Header> entry : map.entrySet()) {
			if(entry.getKey().equals("cookie")){
				Header val = entry.getValue();
				for(String v : val.values){
					int start = v.indexOf("actionSelected=");
					String rest= v.substring(start+15, v.length());
					action = rest.split("[_&]")[0];

					log.info("Action :{}", action);
				}
			}

		}
		return action;
	}

}
