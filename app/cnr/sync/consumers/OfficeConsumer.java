package cnr.sync.consumers;

import cnr.sync.dto.OfficeDTO;
import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import play.Play;
import play.libs.WS;

import java.util.List;


public class OfficeConsumer {
	
	private final String URL_BASE = Play.configuration.getProperty("perseo.base");
	private final String OFFICE_ENDPOINT = Play.configuration.getProperty("perseo.rest.departments");
	
	public ListenableFuture<List<OfficeDTO>> getOffices() throws IllegalStateException{
				
		ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
				.listenInPoolThread(WS.url(URL_BASE+OFFICE_ENDPOINT+"list").getAsync());
		
		return Futures.transform(future, new Function<WS.HttpResponse, List<OfficeDTO>>() {
			@Override
			public List<OfficeDTO> apply(WS.HttpResponse response) {
				if (!response.success()) {
					throw new IllegalStateException("not found");
				}
				return new Gson().fromJson(response.getJson(),
						new TypeToken<List<OfficeDTO>>() {}.getType());
			}
		});
	}

}
