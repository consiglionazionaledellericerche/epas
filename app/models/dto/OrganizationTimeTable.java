package models.dto;

import models.enumerate.PaymentType;

public class OrganizationTimeTable {

  public int numberSlot;
  public String beginSlot;
  public String endSlot;
  public boolean isMealActive = false;
  public String beginMealSlot;
  public String endMealSlot;
  public PaymentType paymentType;
  public int minutesPaid;
}
