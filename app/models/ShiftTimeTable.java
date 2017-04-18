package models;

import models.base.BaseModel;

import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "shift_time_table")
public class ShiftTimeTable extends BaseModel {

  private static final long serialVersionUID = -7869931573320174606L;

  @OneToMany(mappedBy = "shiftTimeTable")
  public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();

  // start time of morning shift
  @Column(name = "start_morning", columnDefinition = "VARCHAR")
  public LocalTime startMorning;

  // end time of morning shift
  @Column(name = "end_morning", columnDefinition = "VARCHAR")
  public LocalTime endMorning;

  // start time of afternoon shift
  @Column(name = "start_afternoon", columnDefinition = "VARCHAR")
  public LocalTime startAfternoon;

  // end time of afternoon shift
  @Column(name = "end_afternoon", columnDefinition = "VARCHAR")
  public LocalTime endAfternoon;
  
  @Column(name = "start_evening", columnDefinition = "VARCHAR")
  public LocalTime startEvening;
  
  @Column(name = "end_evening", columnDefinition = "VARCHAR")
  public LocalTime endEvening;

  // start time for morning lunch break
  @Column(name = "start_morning_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime startMorningLunchTime;

  // end time for the morning lunch break
  @Column(name = "end_morning_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime endMorningLunchTime;

  // start time for the lunch break
  @Column(name = "start_afternoon_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime startAfternoonLunchTime;

  // end time for the lunch break
  @Column(name = "end_afternoon_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime endAfternoonLunchTime;
  
  // start time for the lunch break
  @Column(name = "start_evening_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime startEveningLunchTime;

  // end time for the lunch break
  @Column(name = "end_evening_lunch_time", columnDefinition = "VARCHAR")
  public LocalTime endEveningLunchTime;

  // total amount of working minutes
  @Column(name = "total_working_minutes")
  public Integer totalWorkMinutes;

  // Paid minuts per shift
  @Column(name = "paid_minutes")
  public Integer paidMinutes;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "office_id")
  public Office office;

}
