/**
 * 
 */
package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
@Entity
@Table(name="working_time_types")
public class WorkingTimeType extends Model {

	@Required
	public String description;
	
	public Boolean shift;
	
	public int mondayWorkingTime = 0;
	public int mondayMealTicketTime = 0;
	public int mondayBreakTicketTime = 0;
	public int mondayHoliday = 0;
	public int mondayTimeSlotEntranceFrom = 0;
	public int mondayTimeSlotEntranceTo = 0;
	public int mondayTimeSlotExitFrom = 0;
	public int mondayTimeSlotExitTo = 0;
	public int mondayTimeMealFrom = 0;
	public int mondayTimeMealTo = 0;

	public int tuesdayWorkingTime = 0;
	public int tuesdayMealTicketTime = 0;
	public int tuesdayBreakTicketTime = 0;
	public int tuesdayHoliday = 0;
	public int tuesdayTimeSlotEntranceFrom = 0;
	public int tuesdayTimeSlotEntranceTo = 0;
	public int tuesdayTimeSlotExitFrom = 0;
	public int tuesdayTimeSlotExitTo = 0;
	public int tuesdayTimeMealFrom = 0;
	public int tuesdayTimeMealTo = 0;
	
	public int wednesdayWorkingTime = 0;
	public int wednesdayMealTicketTime = 0;
	public int wednesdayBreakTicketTime = 0;
	public int wednesdayHoliday = 0;
	public int wednesdayTimeSlotEntranceFrom = 0;
	public int wednesdayTimeSlotEntranceTo = 0;
	public int wednesdayTimeSlotExitFrom = 0;
	public int wednesdayTimeSlotExitTo = 0;
	public int wednesdayTimeMealFrom = 0;
	public int wednesdayTimeMealTo = 0;
	
	public int thursdayWorkingTime = 0;
	public int thursdayMealTicketTime = 0;
	public int thursdayBreakTicketTime = 0;
	public int thursdayHoliday = 0;
	public int thursdayTimeSlotEntranceFrom = 0;
	public int thursdayTimeSlotEntranceTo = 0;
	public int thursdayTimeSlotExitFrom = 0;
	public int thursdayTimeSlotExitTo = 0;
	public int thursdayTimeMealFrom = 0;
	public int thursdayTimeMealTo = 0;

	public int fridayWorkingTime = 0;
	public int fridayMealTicketTime = 0;
	public int fridayBreakTicketTime = 0;
	public int fridayHoliday = 0;
	public int fridayTimeSlotEntranceFrom = 0;
	public int fridayTimeSlotEntranceTo = 0;
	public int fridayTimeSlotExitFrom = 0;
	public int fridayTimeSlotExitTo = 0;
	public int fridayTimeMealFrom = 0;
	public int fridayTimeMealTo = 0;
	
	public int saturdayWorkingTime = 0;
	public int saturdayMealTicketTime = 0;
	public int saturdayBreakTicketTime = 0;
	public int saturdayHoliday = 0;
	public int saturdayTimeSlotEntranceFrom = 0;
	public int saturdayTimeSlotEntranceTo = 0;
	public int saturdayTimeSlotExitFrom = 0;
	public int saturdayTimeSlotExitTo = 0;
	public int saturdayTimeMealFrom = 0;
	public int saturdayTimeMealTo = 0;

	public int sundayWorkingTime = 0;
	public int sundayMealTicketTime = 0;
	public int sundayBreakTicketTime = 0;
	public int sundayHoliday = 0;
	public int sundayTimeSlotEntranceFrom = 0;
	public int sundayTimeSlotEntranceTo = 0;
	public int sundayTimeSlotExitFrom = 0;
	public int sundayTimeSlotExitTo = 0;
	public int sundayTimeMealFrom = 0;
	public int sundayTimeMealTo = 0;	
}

