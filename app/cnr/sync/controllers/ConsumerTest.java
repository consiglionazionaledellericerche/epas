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

    PersonDto personDto = new PersonDto();

    try {
      personDto = peopleConsumer.getPerson(iid).get();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return personDto;
  }

  public static List<SimplePersonDto> people() {

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

}
