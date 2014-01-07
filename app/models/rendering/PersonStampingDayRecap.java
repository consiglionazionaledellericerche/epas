package models.rendering;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import models.Absence;
import models.Person;
import models.PersonDay;
import models.PersonDay.PairStamping;
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
	
	private static StampModificationType fixedStampModificationType = null;
	
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
		//List<PairStamping> pairStamping = PairStamping.getValidPairStamping(pd.stampings);
		List<Stamping> stampingsForTemplate = pd.getStampingsForTemplate(numberOfInOut, today);
		//List<PairStamping> pairStamping = PairStamping.getValidPairStamping(stampingsForTemplate);
		this.setStampingTemplate( stampingsForTemplate, pd );
		
		
		WorkingTimeType wtt = pd.person.getWorkingTimeType(pd.date);
		
		//----------------------------------------------- fixed:  worktime, difference, progressive, p---------------------------------
		
		if(pd.isFixedTimeAtWork())
		{
			if(this.future)
			{
				this.fixedWorkingTimeCode = "";
			}
			else
			{
				this.setWorkingTime( pd.timeAtWork );
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
			this.setWorkingTime(pd.timeAtWork);
			this.setDifference( pd.difference );
			this.setProgressive(pd.progressive);
		}
		//----------------------------------------  not fixed:  worktime, difference, progressive for today-------------------------------
		else if(this.today)
		{
			int previousProgressive;
			PersonDay previousPersonDay = pd.previousPersonDay(); 
			if(previousPersonDay == null)
				previousProgressive = 0;
			else
				previousProgressive = previousPersonDay.progressive;
			int workingTime = this.person.getWorkingTimeType(pd.date).getWorkingTimeTypeDayFromDayOfWeek(pd.date.getDayOfWeek()).workingTime;
			int temporaryWorkTime = pd.getCalculatedTimeAtWork();
			this.setWorkingTime( temporaryWorkTime );
			if(this.holiday)
			{
				this.setDifference( 0 );
				this.setProgressive( previousProgressive );
			}
			else
			{
				if(pd.absences.size() == 0)
				{
					int dif = 0;
					if(workingTime > temporaryWorkTime)
					{
						dif = - (workingTime - temporaryWorkTime);
					}
					else
					{
						dif = temporaryWorkTime - workingTime;
					}
					this.setDifference( dif );

					this.setProgressive( previousProgressive + dif);
				}
				else if(pd.absences.size()!=0)
				{
					this.setDifference( pd.difference );
					this.setProgressive(pd.difference + previousProgressive);	
				}
			}
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
			this.setMealTicket(pd.isTicketAvailable);	
		else
			this.setMealTicket(true);

		//----------------------------------------------- lunch (p,e) ------------------------------------------------------------------
		if(pd.modificationType!=null && !this.future)
		{
			//TODO urgente. Creare il campo in personDay con relazione verso StampModificatioType per 
			//mantenere persistita l'informazione (p || e || null). Adesso è persistito solo il codice da stampare.
			StampModificationType smtlunch = StampModificationType.getStampModificationTypeByCode(pd.modificationType);
			this.todayLunchTimeCode = smtlunch.code;
			addStampModificationTypeToList(smtlunch);
		}
		//----------------------------------------------- uscita adesso f ---------------------------------------------------------------
		if(!this.holiday && !pd.isAllDayAbsences() && this.today) 
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
	
	private void setWorkingTime(int workTime)
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


