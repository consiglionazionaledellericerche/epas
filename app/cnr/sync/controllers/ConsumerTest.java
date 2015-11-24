package cnr.sync.controllers;


import cnr.sync.consumers.PeopleConsumer;
import cnr.sync.dto.PersonDTO;
import cnr.sync.dto.SimplePersonDTO;
import com.google.common.collect.Lists;
import play.mvc.Controller;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class ConsumerTest extends Controller{

	@Inject
	static PeopleConsumer peopleConsumer;

	private static PersonDTO person(int iid){

		PersonDTO personDTO = new PersonDTO();

		try {
			personDTO = peopleConsumer.getPerson(iid).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return personDTO;
	}

	private static List<SimplePersonDTO> people(){

		List<SimplePersonDTO> people = Lists.newArrayList();

		try {
			people = peopleConsumer.getPeople().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return people;

	}

	//	public static void syncPeople(){
	//
	//		List<SimplePersonDTO> perseoPeople = people();
	//		
	//		List<Integer> ids = FluentIterable.from(perseoPeople).transform(
	//				new Function<SimplePersonDTO, Integer>() {
	//					@Override
	//					public Integer apply(SimplePersonDTO input) {
	//						return input.id;
	//					}	
	//				}).toList();
	//
	//		
	//
	//		flash.success("chiamata la syncPeople()");
	//
	//		Offices.showOffices();
	//	}

}
