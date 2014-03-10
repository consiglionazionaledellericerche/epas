package models.rendering;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.PersonDay.PairStamping;
import models.ConfGeneral;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.WorkingTimeType;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;

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
	
	private static StampModificationType fixedStampModificationType = null;
	
	public Long personDayId;
	public Person person;
	public WorkingTimeTypeDay wttd;
	public WorkingTimeType wtt;
	public String workingTime = "";
	public String mealTicketTime = "";
	public String timeMealFrom = "";
	public String timeMealTo = "";
	public String breakTicketTime = "";
	
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
	public boolean differenceNegative;
	public String progressive = "";
	public boolean progressiveNegative;
	public String workingTimeTypeDescription = "";
	
	
	public List<String> note = new ArrayList<String>();
		
	public PersonStampingDayRecap(PersonDay pd, int numberOfInOut){			
		this.personDayId = pd.id;
		this.holiday = pd.isHoliday();
		this.person = pd.person;
		setDate(pd.date); 
		this.absences = pd.absences;
		
		List<Stamping> stampingsForTemplate = pd.getStampingsForTemplate(numberOfInOut, today);

		this.setStampingTemplate( stampingsForTemplate, pd );
		
		
		this.wtt = pd.person.getWorkingTimeType(pd.date);
		if(wtt!=null)
		{
			this.wttd = this.wtt.getWorkingTimeTypeDayFromDayOfWeek(pd.date.getDayOfWeek());
			this.setWorkingTime(this.wttd.workingTime);
			this.setMealTicketTime(this.wttd.mealTicketTime);
			this.setBreakTicketTime(this.wttd.breakTicketTime);
		}
		else
		{
			this.setWorkingTime(0);
			this.setMealTicketTime(0);
			this.setBreakTicketTime(0);
		}
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		this.setTimeMealFrom(conf.mealTimeStartHour, conf.mealTimeStartMinute);
		this.setTimeMealTo(conf.mealTimeEndHour, conf.mealTimeEndMinute);
		
		
		//----------------------------------------------- fixed:  worktime, difference, progressive, p---------------------------------
		
		if(pd.isFixedTimeAtWork())
		{
			if(this.future)
			{
				this.fixedWorkingTimeCode = "";
			}
			else
			{
				this.setWorkTime( pd.timeAtWork );
				this.setDifference( pd.difference );
				this.setProgressive( pd.progressive );
				if(pd.timeAtWork!=0)
				{
					if(fixedStampModificationType==null)							//DEVE ANDARE NELLA CACHE
						this.fixedStampModificationType = pd.getFixedWorkingTime();
					this.fixedWorkingTimeCode = this.fixedStampModificationType.code;
					addStampModificationTypeToList(this.fixedStampModificationType);
				}
				
			}
		}
		//---------------------------------------- not fixed:  worktime, difference, progressive for past-----------------------------
		else if(this.past)
		{
			this.setWorkTime(pd.timeAtWork);
			this.setDifference( pd.difference );
			this.setProgressive(pd.progressive);
		}
		//----------------------------------------  not fixed:  worktime, difference, progressive for today-------------------------------
		else if(this.today)
		{
			pd.queSeraSera();
			this.setWorkTime(pd.timeAtWork);
			this.setDifference( pd.difference );
			this.setProgressive(pd.progressive);
		}
		//---------------------------------------- worktime, difference, progressive for future ----------------------------------------
		if(this.future)
		{
			this.difference = "";
			this.workTime = "";
			this.progressive = "";
		}

		//----------------------------------------------- meal ticket (NO)--------------------------------------------------------------
		if(this.today && !pd.isAllDayAbsences())
			this.setMealTicket(pd.isTicketAvailable, true);
		
		else if(this.today && pd.isAllDayAbsences())
			this.setMealTicket(pd.isTicketAvailable, false);	//c'è una assenza giornaliera, la decisione è già presa
		
		else if(!this.holiday)
			this.setMealTicket(pd.isTicketAvailable, false);	
	
		else
			this.setMealTicket(true, false);

		//----------------------------------------------- lunch (p,e) ------------------------------------------------------------------
		if(pd.stampModificationType!=null && !this.future)
		{
			this.todayLunchTimeCode = pd.stampModificationType.code;
			addStampModificationTypeToList(pd.stampModificationType);
		}
		//----------------------------------------------- uscita adesso f ---------------------------------------------------------------
		if(this.today && !this.holiday && !pd.isAllDayAbsences()) 
		{
			StampModificationType smt = StampModificationType.findById(StampModificationTypeValue.ACTUAL_TIME_AT_WORK.getId());
			this.exitingNowCode = smt.code;
			addStampModificationTypeToList(smt);
		}
		//------------------------------------------------description work time type ----------------------------------------------------
		
		if(wtt!=null)
		{
			this.workingTimeTypeDescription = wtt.description; 
		}
		//--------------------------------------------------------------------------------------------------------------------------------
	}
	
	

	private void setMealTicket(boolean mealTicket, boolean todayInProgress) {
		
		if(todayInProgress)
		{
			if(!mealTicket)
			{
				this.mealTicket = "NOT_YET";
			}
			else
			{
				this.mealTicket = "YES";
			}
			return;
		}
			
		if(!mealTicket){
			this.mealTicket = "NO";
		}
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
		StampingTemplate st;
		int actualPair = 0;
		this.stampingsTemplate = new ArrayList<StampingTemplate>();
		for(int i = 0; i<stampings.size(); i++)
		{
			Stamping stamping = stampings.get(i);
			
			
			//Setto pairId e type
			if(stamping.pairId!=0 && stamping.isIn())
			{
				st = new StampingTemplate(stamping, i, pd, stamping.pairId, "left");
				actualPair = stamping.pairId;
			}
			else if(stamping.pairId!=0 && stamping.isOut())
			{
				st = new StampingTemplate(stamping, i, pd, stamping.pairId, "right");
				actualPair = 0;
			}
			else if(actualPair!=0)
			{
				st = new StampingTemplate(stamping, i, pd, actualPair, "center");
			}
			else
			{
				st = new StampingTemplate(stamping, i, pd, 0, "none");
			}
			
			
			//nuova stamping for template
			//st = new StampingTemplate(stamping, i, pd, actualPair, actualPosition);
			this.stampingsTemplate.add(st);
			if(stamping.note!=null && !stamping.note.equals("")){
				note.add(st.hour + ": " + stamping.note);
			}
			
		}
	}
	
	private void setWorkingTime(int workingTime)
	{
		if(workingTime==0)
			this.workingTime = "";
		else
			this.workingTime = DateUtility.fromMinuteToHourMinute(workingTime);
	}
	
	private void setMealTicketTime(int mealTicketTime)
	{
		if(mealTicketTime==0)
			this.mealTicketTime = "";
		else
			this.mealTicketTime = DateUtility.fromMinuteToHourMinute(mealTicketTime);
	}
	
	private void setBreakTicketTime(int breakTicketTime)
	{
		if(breakTicketTime==0)
			this.breakTicketTime = "";
		else
			this.breakTicketTime = DateUtility.fromMinuteToHourMinute(breakTicketTime);
	}
	
	private void setTimeMealFrom(int timeMealFromHour, int timeMealFromMinute)
	{
		String hour = timeMealFromHour + "";
		if(hour.length() == 1)
			hour="0"+hour;
		String minute = timeMealFromMinute+"";
		if(minute.length() == 1)
			minute="0"+minute;
		this.timeMealFrom = hour + ":" + minute;
	}
	
	private void setTimeMealTo(int timeMealToHour, int timeMealToMinute)
	{
		String hour = timeMealToHour + "";
		if(hour.length() == 1)
			hour="0"+hour;
		String minute = timeMealToMinute+"";
		if(minute.length() == 1)
			minute="0"+minute;
		this.timeMealTo = hour + ":" + minute;
	}
	
	private void setWorkTime(int workTime)
	{
		this.workTime = DateUtility.fromMinuteToHourMinute(workTime);
	}
	
	private void setDifference(int difference)
	{
		if(difference<0)
		{
			differenceNegative = true;
			//difference = difference * -1;
		}
		else
		{
			differenceNegative = false;
		}
		this.difference = DateUtility.fromMinuteToHourMinute(difference);
	}
	
	private void setProgressive(int progressive)
	{
		if(progressive<0)
		{
			progressiveNegative = true;
			//progressive = progressive * -1;
		}
		else
		{
			progressiveNegative = false;
		}
		this.progressive = DateUtility.fromMinuteToHourMinute(progressive);
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
		protected int pairId;
		protected String pairPosition;			//left center right none
		protected LocalDateTime date;
		protected String way;
		protected String hour;
		protected String insertStampingClass;
		protected String markedByAdminCode;
		protected String identifier;
		protected String missingExitStampBeforeMidnightCode;
		protected boolean valid;
		
		protected StampingTemplate(Stamping stamping, int index, PersonDay pd, int pairId, String pairPosition)
		{
			this.stampingId = stamping.id;
			this.pairId = pairId;
			this.pairPosition = pairPosition;

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


