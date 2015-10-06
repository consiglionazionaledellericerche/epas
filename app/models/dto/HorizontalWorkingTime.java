package models.dto;

import java.util.List;

import models.Office;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class HorizontalWorkingTime {
	
	@Min(1) @Max(23)
	public int workingTimeHour = 7;
	@Min(0) @Max(59)
	public int workingTimeMinute = 12;
	
	public List holidays;
	
	public boolean mealTicketEnabled = true;
	
	@Min(1) @Max(23)
	public int mealTicketTimeHour = 6;
	@Min(0) @Max(59)
	public int mealTicketTimeMinute = 0;
	@Min(30)
	public int breakTicketTime = 30;
	
	public boolean afternoonThresholdEnabled = false;
	
	@Min(1) @Max(23)
	public int ticketAfternoonThresholdHour = 13;
	@Min(0) @Max(59)
	public int ticketAfternoonThresholdMinute = 30;
	@Min(0)
	public int ticketAfternoonWorkingTime = 1;
	
	@Required
	public String name;
	
	public HorizontalWorkingTime() {
		this.holidays = Lists.newArrayList();
		this.holidays.add("sabato");
		this.holidays.add("domenica");
	}
	
	public HorizontalWorkingTime(WorkingTimeType wtt) {
		
		this.name = wtt.description;
		this.holidays = Lists.newArrayList();
		
		
		for (WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
			
			if (wttd.holiday) {
				this.holidays.add(holidayName(wttd.dayOfWeek));
				continue;
			}
		
			this.workingTimeHour = wttd.workingTime / 60;
			this.workingTimeMinute = wttd.workingTime % 60;
			
			if (wttd.mealTicketTime > 0) {
				this.mealTicketEnabled = true;
				this.mealTicketTimeHour = wttd.mealTicketTime / 60;
				this.mealTicketTimeMinute = wttd.mealTicketTime % 60;
				this.breakTicketTime = wttd.breakTicketTime;
			} else {
				this.mealTicketEnabled = false;
			}
			
			if (wttd.ticketAfternoonThreshold > 0) {
				this.afternoonThresholdEnabled = true;
				this.ticketAfternoonThresholdHour = wttd.ticketAfternoonThreshold / 60;
				this.ticketAfternoonThresholdMinute = wttd.ticketAfternoonThreshold % 60;
				this.ticketAfternoonWorkingTime = wttd.ticketAfternoonWorkingTime;
			} else {
				this.afternoonThresholdEnabled = false;
			}
		}
	}
	
	public WorkingTimeType buildWorkingTimeType(Office office) {
		
		WorkingTimeType wtt = new WorkingTimeType();
		
		wtt.horizontal = true;
		wtt.description = this.name;
		wtt.office = office;
		wtt.disabled = false;
		
		wtt.save();
		
		for (int i = 0; i < 7; i++) {
			
			WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
			wttd.dayOfWeek = i + 1;
			wttd.workingTime = this.workingTimeHour * 60 + this.workingTimeMinute;
			wttd.holiday = isHoliday(wttd);
			
			if (this.mealTicketEnabled) {
				wttd.mealTicketTime = this.mealTicketTimeHour * 60 + this.mealTicketTimeMinute;
				wttd.breakTicketTime = this.breakTicketTime;
				
				if (this.afternoonThresholdEnabled) {
					wttd.ticketAfternoonThreshold = this.ticketAfternoonThresholdHour * 60 
							+ this.ticketAfternoonThresholdMinute;
					wttd.ticketAfternoonWorkingTime = this.ticketAfternoonWorkingTime;
				}
			}
			
			wttd.workingTimeType = wtt;
			wttd.save();

		}

 	    return null;
		
	}
	
	/**
	 * FIXME: Un metodo un pò brutto...
	 * @param wttd
	 * @return
	 */
	private boolean isHoliday(WorkingTimeTypeDay wttd) {
		
		String name;
		
		if (wttd.dayOfWeek == 1) {
			name = "lunedì";
		} else if (wttd.dayOfWeek == 2) {
			name = "martedì";
		} else if (wttd.dayOfWeek == 3) {
			name = "mercoledì";
		} else if (wttd.dayOfWeek == 4) {
			name = "giovedì";
		} else if (wttd.dayOfWeek == 5) {
			name = "venerd'";
		} else if (wttd.dayOfWeek == 6) {
			name = "sabato";
		} else {
			name = "domenica";
		}

		return this.holidays.contains(name);
	}
	
	/**
	 * FIXME: Anche questo è brutto.
	 * @param wttd
	 * @return
	 */
	private static String holidayName(int dayOfWeek) {
		if (dayOfWeek == 1) {
			return "lunedì";
		} else if (dayOfWeek == 2) {
			return "martedì";
		} else if (dayOfWeek == 3) {
			return "mercoledì";
		} else if (dayOfWeek == 4) {
			return "giovedì";
		} else if (dayOfWeek == 5) {
			return "venerd'";
		} else if (dayOfWeek == 6) {
			return "sabato";
		} else {
			return "domenica";
		}
	}
	
	/**
	 * TODO: Impostare un global binder.
	 * @param value
	 */
	public void setHolidays(String value) {
		this.holidays = Lists.newArrayList((Splitter.on(",")
			       .trimResults()
			       .omitEmptyStrings()
			       .split(value)));
	}
	

}
