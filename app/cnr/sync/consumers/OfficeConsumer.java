package cnr.sync.consumers;

import java.util.List;

import play.Play;
import play.libs.WS;
import cnr.sync.dto.OfficeDTO;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;


public class OfficeConsumer {
	
	public ListenableFuture<List<OfficeDTO>> getOffices() throws IllegalStateException{
		String urlBase = Play.configuration.getProperty("perseo.base");
		String endpointOffices = Play.configuration.getProperty("perseo.rest.departments");
		
		ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
				.listenInPoolThread(WS.url(urlBase+endpointOffices+"list").getAsync());
		
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
