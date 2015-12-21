package cnr.sync.controllers;


import com.google.common.collect.Lists;

import cnr.sync.consumers.PeopleConsumer;
import cnr.sync.dto.PersonDto;
import cnr.sync.dto.SimplePersonDto;
import play.mvc.Controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;


public class ConsumerTest extends Controller {

  @Inject
  static PeopleConsumer peopleConsumer;

  private static PersonDto person(int iid) {

    PersonDto personDTO = new PersonDto();

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

  private static List<SimplePersonDto> people() {

    List<SimplePersonDto> people = Lists.newArrayList();

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
