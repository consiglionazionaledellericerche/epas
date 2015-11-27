package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.joda.time.LocalTime;

import models.base.BaseModel;

@Entity
@Table(name = "shift_time_table")
public class ShiftTimeTable extends BaseModel {

  private static final long serialVersionUID = -7869931573320174606L;

  @OneToMany(mappedBy = "shiftTimeTable")
  public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();

  // start time of morning shift
  @Column(name = "start_morning", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime startMorning;

  // end time of morning shift
  @Column(name = "end_morning", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime endMorning;

  // start time of afternoon shift
  @Column(name = "start_afternoon", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime startAfternoon;

  // end time of afternoon shift
  @Column(name = "end_afternoon", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime endAfternoon;

  // start time for morning lunch break
  @Column(name = "start_morning_lunch_time", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime startMorningLunchTime;

  // end time for the morning lunch break
  @Column(name = "end_morning_lunch_time", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime endMorningLunchTime;

  // start time for the lunch break
  @Column(name = "start_afternoon_lunch_time", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime startAfternoonLunchTime;

  // end time for the lunch break
  @Column(name = "end_afternoon_lunch_time", columnDefinition = "VARCHAR")
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
  public LocalTime endAfternoonLunchTime;

  // total amount of working minutes
  @Column(name = "total_working_minutes")
  public Integer totalWorkMinutes;

  // Paid minuts per shift
  @Column(name = "paid_minutes")
  public Integer paidMinutes;

}
