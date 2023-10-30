package models.dto;

import models.Person;

public class PersonOvertimeInMonth {

  public Person person;
  public Integer quantity;

  public PersonOvertimeInMonth(Person person, Integer quantity) {
    this.person = person;
    this.quantity = quantity;
  }
}
