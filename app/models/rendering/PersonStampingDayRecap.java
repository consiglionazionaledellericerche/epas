package models.rendering;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.WorkingTimeType;
import models.Stamping.WayType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.xhtmlrenderer.css.style.CalculatedStyle;

/**
 * Oggetto che modella il giorno di una persona nelle viste personStamping e stampings.
 * @author alessandro
 *
 */
public class PersonStampingDayRecap {

	public static List<StampModificationType> stampModificationTypeList;
	public static List<StampType> stampTypeList;
	
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
	
	public List<String> note = new ArrayList<String>();
		
	public PersonStampingDayRecap(PersonDay pd, int numberOfInOut)
	{
		if(pd.date.getDayOfMonth()==22)
		{
			System.out.println("");
		}
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
			//	pd.timeAtWork = 0;
			//	pd.save();
			}
			else
			{
				this.fixedWorkingTimeCode = smt.code;
				addStampModificationTypeToList(smt);
			//	pd.save(); //TODO toglierlo quando il db sarà consistente
				int temporaryWorkTime = pd.getCalculatedTimeAtWork();
				this.setWorkingTime( temporaryWorkTime );
			//	pd.timeAtWork = temporaryWorkTime;
			//	pd.save();	//TODO toglierlo quando il db sarà consistente
			//	pd.updateDifference(); //TODO toglierlo quando il db sarà consistente 
			//	pd.updateProgressive(); 
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
	
			this.setDifference( pd.difference );
	
			PersonDay previousPd = pd.checkPreviousProgressive();
			if(pd.date.getDayOfMonth()==1)
			{
				this.setProgressive(pd.progressive);
			}
			else if(previousPd!=null)
			{
				this.setProgressive(pd.difference + previousPd.progressive);
			}
			else
			{
				this.setProgressive(0);
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
		if(pd.lunchTimeStampModificationType!=null && !this.future)
		{
			this.todayLunchTimeCode = pd.lunchTimeStampModificationType.code;
			addStampModificationTypeToList(pd.lunchTimeStampModificationType);
		}
		//----------------------------------------------- uscita adesso f ---------------------------------------------------------------
		if(!this.holiday && !pd.isAllDayAbsences() && this.today) //TODO 
		{
			smt = StampModificationType.findById(StampModificationTypeValue.ACTUAL_TIME_AT_WORK.getId());
			this.exitingNowCode = smt.code;
			addStampModificationTypeToList(smt);
		}
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
			Stamping stamping = stampings.get(i);
			
			//nuova stamping for template
			StampingTemplate st = new StampingTemplate(stamping, i, pd);
			this.stampingsTemplate.add(st);
			if(stamping.note!=null && !stamping.note.equals("")){
				note.add(st.hour + ": " + stamping.note);
			}
			
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
	
	/**
	 * TODO sostituirlo con PersonTags.toHourTime
	 * @param minute
	 * @return
	 */
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
	
	private static void addStampModificationTypeToList(StampModificationType smt)
	{
		try
		{
		if(!stampModificationTypeList.contains(smt))
		{
			stampModificationTypeList.add(smt);
		}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void addStampTypeToList(StampType st)
	{
		if(!stampTypeList.contains(st))
		{
			stampTypeList.add(st);
		}
		
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

			//stamping nulle o exiting now non sono visualizzate
			if(stamping.date == null  || stamping.exitingNow)
			{
				this.hour = ""; 
				this.markedByAdminCode = "";
				this.identifier = "";
				this.insertStampingClass = "";
				this.missingExitStampBeforeMidnightCode = "";
				this.valid = true;
				setColor(stamping);
				return;
			}
			
			this.date = stamping.date;
			
			this.way = stamping.way.description;
			
			setHour(stamping.date);//stamping.date.getHourOfDay() + ":" + stamping.date.getMinuteOfHour();
			
			this.insertStampingClass = "insertStamping" + stamping.date.getDayOfMonth() + "-" + index;
			
			
			//----------------------------------------- timbratura di servizio ---------------------------------------------------
			if(stamping.stampType!=null && stamping.stampType.identifier!=null)
			{
				this.identifier = stamping.stampType.identifier;
				addStampTypeToList(stamping.stampType);
			}
			else
				this.identifier = "";
			
			//----------------------------------------- timbratura modificata dall'amministatore ---------------------------------
			if(stamping.markedByAdmin) 
			{
				StampModificationType smt = StampModificationType.findById(StampModificationTypeValue.MARKED_BY_ADMIN.getId());
				this.markedByAdminCode = smt.code;
				addStampModificationTypeToList(smt);
			}
			else
			{
				this.markedByAdminCode = "";
			}
			
			//----------------------------------------- missingExitStampBeforeMidnightCode ?? --------------------------------------
			if(stamping.stampModificationType!=null)
			{
				StampModificationType smt = pd.checkMissingExitStampBeforeMidnight();
				if(smt!=null)
				{
					this.missingExitStampBeforeMidnightCode = smt.code;
					addStampModificationTypeToList(smt);
				}
				else
				{
					this.missingExitStampBeforeMidnightCode = "";
				}
			}
			//------------------------------------------- timbratura valida (colore cella) -----------------------------------------
			LocalDate today = new LocalDate();
			LocalDate stampingDate = new LocalDate(this.date.getYear(), this.date.getMonthOfYear(), this.date.getDayOfMonth());
			if(today.isEqual(stampingDate))
			{
				this.valid = true;
			}
			else
			{
				this.valid = stamping.isValid();
			}
			setColor(stamping);
			//-----------------------------------------------------------------------------------------------------------------------
		}
		
		protected void setColor(Stamping stamping)
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


