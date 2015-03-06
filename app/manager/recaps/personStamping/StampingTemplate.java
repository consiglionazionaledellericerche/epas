package manager.recaps.personStamping;

import manager.PersonDayManager;
import models.PersonDay;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.Stamping;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import dao.StampingDao;

/**
 * Oggetto che modella la singola timbratura nelle viste personStamping e stampings.
 * @author alessandro
 *
 */
public class StampingTemplate {
	
	//private final PersonDayManager personDayManager;
	//private final StampingDao stampingDao;
	
	public Long stampingId;
	public String colour;
	public int pairId;
	public String pairPosition;			//left center right none
	public LocalDateTime date;
	public String way;
	public String hour;
	public String insertStampingClass;
	public String markedByAdminCode;
	public String identifier;
	public String missingExitStampBeforeMidnightCode;
	public boolean valid;
	
	public StampingTemplate(PersonDayManager personDayManager,
			StampingDao stampingDao, Stamping stamping,
			int index, PersonDay pd, int pairId, String pairPosition)
	{
		//this.stampingDao = stampingDao;
		//this.personDayManager = personDayManager;
		
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
		
		setHour(stamping.date);
		
		this.insertStampingClass = "insertStamping" + stamping.date.getDayOfMonth() + "-" + index;
		
		
		//----------------------------------------- timbratura di servizio ---------------------------------------------------
		if(stamping.stampType!=null && stamping.stampType.identifier!=null)
		{
			this.identifier = stamping.stampType.identifier;
			PersonStampingDayRecap.stampTypeSet.add(stamping.stampType);
		}
		else
			this.identifier = "";
		
		//----------------------------------------- timbratura modificata dall'amministatore ---------------------------------
		if(stamping.markedByAdmin) 
		{
			StampModificationType smt = stampingDao.getStampModificationTypeById(StampModificationTypeValue.MARKED_BY_ADMIN.getId());
			this.markedByAdminCode = smt.code;
			PersonStampingDayRecap.stampModificationTypeSet.add(smt);
		}
		else
		{
			this.markedByAdminCode = "";
		}
		
		//----------------------------------------- missingExitStampBeforeMidnightCode ?? --------------------------------------
		if(stamping.stampModificationType!=null)
		{
			StampModificationType smt = personDayManager.checkMissingExitStampBeforeMidnight(pd);
			if(smt!=null)
			{
				this.missingExitStampBeforeMidnightCode = smt.code;
				PersonStampingDayRecap.stampModificationTypeSet.add(smt);
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