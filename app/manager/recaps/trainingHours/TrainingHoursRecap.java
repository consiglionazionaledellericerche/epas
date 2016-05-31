package manager.recaps.trainingHours;

import models.Person;

import org.joda.time.LocalDate;

public class TrainingHoursRecap {
  
  public Person person;
  public Integer trainingHours;
  public LocalDate begin;
  public LocalDate end;
  public boolean sentToAttestati;

  public TrainingHoursRecap(Person person, Integer trainingHours, LocalDate begin, LocalDate end, boolean sentToAttestati) {
    this.person = person;
    this.trainingHours = trainingHours;
    this.begin = begin;
    this.end = end;
    this.sentToAttestati = sentToAttestati;
  }
}
