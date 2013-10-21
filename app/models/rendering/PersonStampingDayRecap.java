package models.rendering;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.StampProfile;
import models.Stamping;
import models.WorkingTimeType;
import models.Stamping.WayType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.xhtmlrenderer.css.style.CalculatedStyle;

public class PersonStampingDayRecap {

	public Long personDayId;
	public Person person;
		
	public String mealTicket;
	
	public LocalDate date;
	public boolean holiday;
	public boolean past;
	public boolean today;
	public boolean future;
	
	public List<Absence> absences;
	
	public List<StampingTemplate> stampingsTemplate;
	
	public String workTime = "";
	public String todayLunchTimeCode = "";
	public String fixedWorkingTimeCode = "";
	public String exitingNowCode = "";
	
	public String difference = "";
	public String progressive = "";
	public String workingTimeTypeDescription = "";
	public String note = "";
	
	public PersonStampingDayRecap(PersonDay pd, int numberOfInOut)
	{
		this.personDayId = pd.id;
		this.holiday = pd.isHoliday();
		this.person = pd.person;
		setDate(pd.date); 
		this.absences = pd.absences;
		List<Stamping> stampingsForTemplate = pd.getStampingsForTemplate(numberOfInOut, today);
		this.setStampingTemplate( stampingsForTemplate, pd );
		
		//----------------------------------------------- fixed:  worktime, difference, progressive, p---------------------------------
		StampModificationType smt = pd.getFixedWorkingTime();
		if(smt !=null)
		{
			if(this.holiday || pd.isAllDayAbsences() || this.future)
			{
				this.fixedWorkingTimeCode = "";
				this.setWorkingTime(0);
				pd.timeAtWork = 0;
				pd.save();
			}
			else
			{
				this.fixedWorkingTimeCode = smt.code;
				pd.save(); //TODO toglierlo quando il db sarà consistente
				int temporaryWorkTime = pd.getCalculatedTimeAtWork();
				this.setWorkingTime( temporaryWorkTime );
				pd.timeAtWork = temporaryWorkTime;
				pd.save();	//TODO toglierlo quando il db sarà consistente
				pd.updateDifference(); //TODO toglierlo quando il db sarà consistente 
				pd.updateProgressive(); 
			}
			this.setDifference(0);
			this.setProgressive(0);
		}
		//----------------------------------------  not fixed:  worktime, difference, progressive for today-------------------------------
		else if(this.today)
		{
			int temporaryWorkTime = pd.getCalculatedTimeAtWork();
			this.setWorkingTime( temporaryWorkTime );
			if(pd.absences.size() == 0)
			{
				int dif = 0;
				if(pd.person.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek()).workingTime > temporaryWorkTime)
				{
					dif = - (this.person.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek()).workingTime - temporaryWorkTime);
				}
				else
				{
					dif = temporaryWorkTime - this.person.workingTimeType.workingTimeTypeDays.get(date.getDayOfWeek()).workingTime;
				}
				this.setDifference( dif );
				this.setProgressive( pd.previousPersonDay().progressive + dif);
			}
			if(pd.absences.size()!=0)
			{
				this.setDifference( pd.difference );
				this.setProgressive(pd.difference + pd.previousPersonDay().progressive);	
			}
		}
		//---------------------------------------- not fixed:  worktime, difference, progressive for past-----------------------------
		else if(this.past)
		{
			pd.save();	//TODO toglierlo quando il db sarà consistente
			int temporaryWorkTime = pd.getCalculatedTimeAtWork();
			this.setWorkingTime(temporaryWorkTime);
			pd.timeAtWork = temporaryWorkTime;
			pd.save();	//TODO toglierlo quando il db sarà consistente
			pd.updateDifference(); //TODO toglierlo quando il db sarà consistente
			pd.updateProgressive(); 
						
			if(!this.holiday || pd.stampings.size() != 0)
			{
				this.setDifference( pd.difference );
			}
			else
			{
				this.setDifference(0);
			}
			if(!this.holiday)
			{
				this.setProgressive(pd.progressive);
			}
			else
			{
				PersonDay previousPd = pd.checkPreviousProgressive();
				if(previousPd!=null)
				{
					this.setProgressive(previousPd.progressive);
				}
				else
				{
					this.setProgressive(0);
				}
			}
			pd.save();	//TODO poi va rimosso
		}
		//---------------------------------------- worktime, difference, progressive for future ----------------------------------------
		if(this.future)
		{
			this.difference = "";
			this.workTime = "";
			this.progressive = "";
		}
		
		//----------------------------------------------- meal ticket (NO)--------------------------------------------------------------
		if(!this.holiday)
			this.setMealTicket(pd.mealTicket());	
		else
			this.setMealTicket(true);
		
		//----------------------------------------------- lunch (p,e) ------------------------------------------------------------------
		if(pd.lunchTimeCode!=null && !this.future)
		{
			this.todayLunchTimeCode = pd.lunchTimeCode;		
		}
		//----------------------------------------------- uscita adesso f ---------------------------------------------------------------
		if(!this.holiday && !pd.isAllDayAbsences() && this.today)
			this.exitingNowCode = "f";
		//------------------------------------------------description work time type ----------------------------------------------------
		WorkingTimeType wtt = pd.person.workingTimeType;
		if(wtt!=null)
		{
			this.workingTimeTypeDescription = wtt.description; 
		}
		//--------------------------------------------------------------------------------------------------------------------------------
	}
	
	

	private void setMealTicket(boolean mealTicket) {
		if(!mealTicket)
			this.mealTicket = "NO";
		else
			this.mealTicket = "";
	}
	
	private void setDate(LocalDate date)
	{
		LocalDate today = new LocalDate();
		this.date = date;
		if(date.equals(today))
		{
			this.today = true;
			this.past = false;
			this.future = false;
			return;
		}
		if(date.isBefore(today))
		{
			this.today = false;
			this.past = true;
			this.future = false;
			return;
		}
		else
		{
			this.today = false;
			this.past = false;
			this.future = true;
			return;
		}
	}
	
	private void setStampingTemplate(List<Stamping> stampings, PersonDay pd)
	{
		this.stampingsTemplate = new ArrayList<StampingTemplate>();
		for(int i = 0; i<stampings.size(); i++)
		{
			this.stampingsTemplate.add(new StampingTemplate(stampings.get(i), i, pd));
		}
	}
	
	private void setWorkingTime(int workTime)
	{
		this.workTime = fromMinuteToHourMinute(workTime);
	}
	
	private void setDifference(int difference)
	{
		this.difference = fromMinuteToHourMinute(difference);
	}
	
	private void setProgressive(int progressive)
	{
		this.progressive = fromMinuteToHourMinute(progressive);
	}
	
	private static String fromMinuteToHourMinute(int minute)
	{
		String s = "";
		if(minute<0)
		{
			s = s + "-";
			minute = minute * -1;
		}
		int hour = minute / 60;
		int min  = minute % 60;
		
		if(hour<10)
		{
			s = s + "0" + hour;
		}
		else
		{
			s = s + hour;
		}
		s = s + ":";
		if(min<10)
		{
			s = s + "0" + min;
		}
		else
		{
			s = s + min;
		}
		return s;
	}
	
	
	protected static class StampingTemplate
	{
		protected Long stampingId;
		protected String colour;
		protected LocalDateTime date;
		protected String way;
		protected String hour;
		protected String insertStampingClass;
		protected String markedByAdminCode;
		protected String identifier;
		protected String missingExitStampBeforeMidnightCode;
		protected boolean valid;
		
		protected StampingTemplate(Stamping stamping, int index, PersonDay pd)
		{
			this.stampingId = stamping.id;

			if(stamping.date == null)
			{
				this.hour = ""; 
				this.markedByAdminCode = "";
				this.identifier = "";
				this.insertStampingClass = "";
				this.missingExitStampBeforeMidnightCode = "";
				this.valid = true;
				setColour(stamping);
				return;
			}
			
			
			
			this.date = stamping.date;
			
			this.way = stamping.way.description;
			
			setHour(stamping.date);//stamping.date.getHourOfDay() + ":" + stamping.date.getMinuteOfHour();
			
			this.insertStampingClass = "insertStamping" + stamping.date.getDayOfMonth() + "-" + index;
			
			if(stamping.stampType!=null && stamping.stampType.identifier!=null)
				this.identifier = stamping.stampType.identifier;
			else
				this.identifier = "";
			
			if(stamping.markedByAdmin) 
			{
				StampModificationType smt = StampModificationType.findById(StampModificationTypeValue.MARKED_BY_ADMIN.getId());
				this.markedByAdminCode = smt.code;
			}
			else
				this.markedByAdminCode = "";
			
			//missingExitStampBeforeMidnightCode ??
			if(stamping.stampModificationType!=null)
			{
				StampModificationType smt = pd.checkMissingExitStampBeforeMidnight();
				if(smt!=null)
				{
					this.missingExitStampBeforeMidnightCode = smt.code;
				}
				else
				{
					this.missingExitStampBeforeMidnightCode = "";
				}
			}
			
			this.valid = stamping.isValid();
			setColour(stamping);
		}
		
		protected void setColour(Stamping stamping)
		{
			this.colour = stamping.way.description;
			if(this.valid==false)
			{
				this.colour = "warn";
			}
		}
		
		protected void setHour(LocalDateTime date)
		{
			String hour = date.getHourOfDay() + "";
			if(hour.length() == 1)
				hour="0"+hour;
			String minute = date.getMinuteOfHour()+"";
			if(minute.length() == 1)
				minute="0"+minute;
			this.hour = hour + ":" + minute;
		}
	}
	
}


