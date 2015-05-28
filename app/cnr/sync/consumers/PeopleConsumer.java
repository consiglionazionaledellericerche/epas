package cnr.sync.consumers;

import java.util.List;

import play.Play;
import play.libs.WS;
import cnr.sync.dto.PersonDTO;
import cnr.sync.dto.SimplePersonDTO;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PeopleConsumer {
	
	public ListenableFuture<PersonDTO> getPerson(int id) throws IllegalStateException{
		String urlBase = Play.configuration.getProperty("perseo.base");
		String endpointPeople = Play.configuration.getProperty("perseo.rest.people");
		
		ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
				.listenInPoolThread(WS.url(urlBase+endpointPeople+id).getAsync());
		return Futures.transform(future, new Function<WS.HttpResponse, PersonDTO>() {
			@Override
			public PersonDTO apply(WS.HttpResponse response) {
				if (!response.success()) {
					throw new IllegalStateException("not found");
				}
				return new Gson().fromJson(response.getJson(), PersonDTO.class);
			}
		});
	}
	
	public ListenableFuture<List<SimplePersonDTO>> getPeople() throws IllegalStateException{
		String urlBase = Play.configuration.getProperty("perseo.base");
		String endpointPeople = Play.configuration.getProperty("perseo.rest.people");
		
		ListenableFuture<WS.HttpResponse> future = JdkFutureAdapters
				.listenInPoolThread(WS.url(urlBase+endpointPeople+"list").getAsync());
		
		return Futures.transform(future, new Function<WS.HttpResponse, List<SimplePersonDTO>>() {
			@Override
			public List<SimplePersonDTO> apply(WS.HttpResponse response) {
				if (!response.success()) {
					throw new IllegalStateException("not found");
				}
				return new Gson().fromJson(response.getJson(),
						new TypeToken<List<SimplePersonDTO>>() {}.getType());
			}
		});
	}

}
