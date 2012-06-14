package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;
import net.sf.oval.guard.Guarded;

import org.hibernate.annotations.Target;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Questa classe modella un riepilogo mensile dei giorni lavorativi e non di una persona.
 * La classe contiene una serie di metodi di utilità per estrarre valore calcolati in funzione
 * delle timbrature ed assenze mensili della persona indicata. 
 * 
 * @author cristian
 * @author dario
 *
 * Per adesso la classe Month recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista sia nella struttura che più banalmente nei nomi dei campi
 * 
 */
@Guarded
@Entity
@Table(name = "month_recaps")
public class MonthRecap extends Model {
	
	private static final long serialVersionUID = -448436166612631217L;

	@Required
	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Required
	public int month;

	@Required
	public int year;

	public short workingDays;

	public short daysWorked;

	public short giorniLavorativiLav;

	public int workTime;

	public int remaining;

	public short justifiedAbsence;

	public short vacationAp;

	public short vacationAc;

	public short holidaySop;

	public int recoveries;

	public short recoveriesG;

	public short recoveriesAp;

	public short recoveriesGap;

	public int overtime;

	public Timestamp lastModified;

	public int residualApUsed;

	public int extraTimeAdmin;

	public int additionalHours;

	public boolean nadditionalHours;

	public int residualFine;

	public short endWork;

	public short beginWork;

	public int timeHourVisit;

	public short endRecoveries;

	public int negative;

	public int endNegative;

	public String progressive;
	
	protected boolean persistent = false;
	
	@Transient
	private List<PersonDay> days = null;
	
	@Transient
	private List<String> months = null;
	
	@Transient	
	private int progressiveOfDailyTime=0; 
	
	@Transient
	private Map<AbsenceType, Integer> absenceCodeMap;
	
	@Transient
	private List<StampModificationType> stampingCodeList;
	
	
	protected MonthRecap(){
		this.stampingCodeList = new ArrayList<StampModificationType>();
		this.absenceCodeMap  = new HashMap<AbsenceType, Integer>();
	}

	
	/**
	 * Construttore di default con i parametri obbligatori
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @param month il mese di riferimento
	 */
	public MonthRecap(
			@NotNull Person person, 
			@Min(1970) int year, 
			@Range(min=1, max=12) int month) {
		this.person = person;
		this.year = year;
		this.month = month;
		this.stampingCodeList = new ArrayList<StampModificationType>();
		this.absenceCodeMap  = new HashMap<AbsenceType, Integer>();
	}
	
	/**
	 * Preleva dallo storage le il MonthRecap relativo ai dati passati.
	 * Se il monthRecap non è presente sul db ritorna un'istanza vuota
	 * ma con associati i dati passati.
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @param month il mese di riferimento
	 * @return il riepilogo mensile, se non è presente nello storage viene 
	 * 	restituito un riepilogo mensile vuoto
	 */
	public static MonthRecap byPersonAndYearAndMonth(
			@NotNull Person person, 
			@Min(1970) int year, 
			@Range(min=1, max=12) int month) {
		if (person == null) {
			throw new IllegalArgumentException("Person mandatory");
		}
		MonthRecap monthRecap = MonthRecap.find("byPersonAndYearAndMonth", person, year, month).first();
		if (monthRecap == null) {
			return new MonthRecap(person, year, month);
		}
		monthRecap.persistent = true;
		return monthRecap;
	}
	
	
	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<PersonDay> getDays() {

		if (days != null) {
			return days;
		}
		days = new ArrayList<PersonDay>();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		//Nel calendar i mesi cominciano da zero
		firstDayOfMonth.set(year, month - 1, 1);
		
		Logger.trace(" %s-%s-%s : maximum day of month = %s", 
			year, month, 1, firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH));
		
		for (int day = 1; day <= firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {
		
			Logger.trace("generating PersonDay: person = %s, year = %d, month = %d, day = %d", person.username, year, month, day);
			days.add(new PersonDay(person, new LocalDate(year, month, day), 0, 0, 0));
		}
		return days;
	}	
	

	/**
	 * 
	 * @return la lista dei mesi
	 */
	public List<String> getMonths(){
		
		if(months!=null){
			return months;
		}
		
		months = new ArrayList<String>();
		LocalDate firstMonthOfYear = new LocalDate(year, 1,1);
		
		for(int month = 1; month <= firstMonthOfYear.getMonthOfYear(); month++){
			String mese = firstMonthOfYear.monthOfYear().getAsText();
			months.add(mese);
			firstMonthOfYear=firstMonthOfYear.plusMonths(1);
		}
		return months;
	}
	
	
	
	/**
	 * 
	 * @return il progressivo della differenza giornaliera tra orario di lavoro previsto e orario di lavoro effettivamente fatto
	 */
	private int getProgressive(int difference){
		
		
		progressiveOfDailyTime=progressiveOfDailyTime+difference;
		return progressiveOfDailyTime;
		
	}
	/**
	 * 
	 * @param days lista di PersonDay
	 * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
	 */
	
	public Map<AbsenceType,Integer> getAbsenceCode(){
		
		if(days == null){
			days = getDays();
		}
		if(absenceCodeMap.isEmpty()){
			Integer i = 0;
			for(PersonDay pd : days){
	             AbsenceType absenceType = pd.getAbsenceType();
	             if(absenceType != null){
	            	 boolean stato = absenceCodeMap.containsKey(absenceType);
	            	 if(stato==false){
	                	 i=1;
	                	 absenceCodeMap.put(absenceType,i);            	 
	                 }
	            	 else{
	                	 i = absenceCodeMap.get(absenceType);
	                	 absenceCodeMap.remove(absenceType);
	                	 absenceCodeMap.put(absenceType, i+1);
	               	 }
	             }            
	            	 
	        }       
		}
		      
        return absenceCodeMap;	
						
	}
	
	
	public Map<AbsenceType, Integer> getAbsenceCodeMap() {
		return absenceCodeMap;
	}

	/**
	 * 
	 * @param days
	 * @return lista dei codici delle timbrature nel caso in cui ci siano particolarità sulle timbrature dovute a mancate timbrature
	 * per pausa mensa ecc ecc...
	 */
	public List<StampModificationType> getStampingCode(){
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			List stampings = pd.getStampings();
			StampModificationType smt = pd.checkTimeForLunch(stampings);
			/**
			 * da togliere dopo il debug...
			 */
			if(stampingCodeList!=null)
				Logger.debug("stampingcodelist: "+stampingCodeList);
			else{
				Logger.warn("stampingcodelist è nullo");
				
			}
			if(smt!=null)
				Logger.debug("smt: "+smt);
			else{
				Logger.warn("smt è nullo");
				
			}
			
			boolean stato = stampingCodeList.contains(smt);
			if(smt != null && stato==false){
				stampingCodeList.add(smt);
			}		
		
		}
		return stampingCodeList;
	}
	
	/**
	 * metodo di utilità che calcola nel mese corrente qual'è stato il massimo numero di timbrature giornaliere
	 * mi servierà nella form di visualizzazione per stabilire quante colonne istanziare per le timbrature
	 * @return
	 */
	public int maxNumberOfStamping(){
		int max = 0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			List stampings = pd.getStampings();
			int number = stampings.size();
			if(number > max)
				max = number;
		}
		return max;
	}
	
	/**
	 * 
	 * @return il numero di buoni pasto usabili per quel mese
	 */
	public int numberOfMealTicketToUse(){
		int tickets=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==true)
				tickets++;
		}
		
		return tickets;
	}
	
	/**
	 * 
	 * @return il numero di buoni pasto da restituire per quel mese
	 */
	public int numberOfMealTicketToRender(){
		int ticketsToRender=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==false && (pd.isHoliday()==false))
				ticketsToRender++;
		}
		
		return ticketsToRender;
	}
	
	/**
	 * 
	 * @return il numero di giorni lavorati in sede. Per stabilirlo si controlla che per ogni giorno lavorativo, esista almeno una 
	 * timbratura.
	 */
	public int basedWorkingDays(){
		int basedDays=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			List<Stamping> stamp = pd.getStampings();
			if(stamp.size()>0 && pd.isHoliday()==false)
				basedDays++;
		}
		return basedDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di indennità di reperibilità festiva per quella persona in quel mese di quell'anno
	 */
	public int holidaysAvailability(int year, int month){
		int holidaysAvailability = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "208").first();
		Logger.warn("competence: " +competence);
		if(competence != null)
			holidaysAvailability = competence.value;
		else
			holidaysAvailability = 0;
		return holidaysAvailability;
	}

	/**
	 * 
	 * @return il numero di giorni di indennità di reperibilità feriale per quella persona in quel mese di quell'anno
	 */
	public int weekDayAvailability(int year, int month){
		int weekDayAvailability = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "207").first();
		if(competence != null)
			weekDayAvailability = competence.value;
		else
			weekDayAvailability = 0;
		return weekDayAvailability;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @return il numero di giorni di straordinario diurno nei giorni lavorativi 
	 */
	public int daylightWorkingDaysOvertime(int year, int month){
		int daylightWorkingDaysOvertime = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "S1").first();
		if(competence != null)
			daylightWorkingDaysOvertime = competence.value;
		else
			daylightWorkingDaysOvertime = 0;
		return daylightWorkingDaysOvertime;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @return il numero di giorni di straordinario diurno nei giorni festivi o notturno nei giorni lavorativi
	 */
	public int daylightholidaysOvertime(int year, int month){
		int daylightholidaysOvertime = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "S2").first();
		if(competence != null)
			daylightholidaysOvertime = competence.value;
		else
			daylightholidaysOvertime = 0;
		return daylightholidaysOvertime;
	}
	
	/**
	 * 
	 * @return il numero di giorni di turno ordinario
	 */
	public int ordinaryShift(int year, int month){
		int ordinaryShift = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "T1").first();
		if(competence != null)
			ordinaryShift = competence.value;
		else
			ordinaryShift = 0;
		return ordinaryShift;
	}
	
	/**
	 * 
	 * @return il numero di giorni di turno notturno
	 */
	public int nightShift(int year, int month){
		int nightShift = 0;
		Competence competence = Competence.find("Select comp from Competence comp where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.code = ?", person, year, month, "T2").first();
		if(competence != null)
			nightShift = competence.value;
		else
			nightShift = 0;
		return nightShift;
	}
	
	/**
	 * questo metodo riempe il campo del residuo mensile delle ore del mese precedente nel momento in cui inizia il nuovo mese
	 * @param date
	 */
	public void fillHoursRemaining(LocalDate date){
		if(date.getDayOfMonth()==1){
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? " +
					"and pm.month = ?", person, date.getYear(),date.getMonthOfYear()-1).first();
			if(pm==null){
				pm = new PersonMonth(person,date.getYear(),date.getMonthOfYear()-1);
				if((date.getMonthOfYear()-1)==1 || (date.getMonthOfYear()-1)==3 || (date.getMonthOfYear()-1)==5 || 
						(date.getMonthOfYear()-1)==7 || (date.getMonthOfYear()-1)==8 || 
						(date.getMonthOfYear()-1)==10 || (date.getMonthOfYear()-1)==12){
					LocalDate pastMonth = new LocalDate(date.getYear(),date.getMonthOfYear()-1,31);
					PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and " +
							"pd.date = ?", person, pastMonth).first();
					pm.remainingHours = pd.progressive;
					pm.save();
				}
				if((date.getMonthOfYear()-1)==4 || (date.getMonthOfYear()-1)==6 || (date.getMonthOfYear()-1)==9 
						|| (date.getMonthOfYear()-1)==11){
					LocalDate pastMonth = new LocalDate(date.getYear(),date.getMonthOfYear()-1,30);
					PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and " +
							"pd.date = ?", person, pastMonth).first();
					pm.remainingHours = pd.progressive;
					pm.save();
				}
				if((date.getMonthOfYear()-1)==2){
					if(date.getYear()==2008 || date.getYear()==2012 || date.getYear()==2016 || date.getYear() == 2020){
						LocalDate pastMonth = new LocalDate(date.getYear(),date.getMonthOfYear()-1,29);
						PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and " +
								"pd.date = ?", person, pastMonth).first();
						pm.remainingHours = pd.progressive;
						pm.save();
					}
					else{
						LocalDate pastMonth = new LocalDate(date.getYear(),date.getMonthOfYear()-1,28);
						PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and " +
								"pd.date = ?", person, pastMonth).first();
						pm.remainingHours = pd.progressive;
						pm.save();
					}
					
				}
			}
		}
	}
	
	/**
	 * 
	 * @return il numero di ore di lavoro in eccesso/difetto dai mesi precedenti, calcolate a partire da gennaio dell'anno corrente.
	 * nel caso in cui ci trovassimo a gennaio, le ore di lavoro in eccesso/difetto provengono dall'anno precedente
	 */
	public int pastRemainingHours(int month, int year){
		int pastRemainingHours = 0;
		/**
		 * per adesso ricorro al metodo di ricerca del progressivo all'ultimo giorno dell'anno precedente, non appena sarà pronta 
		 * la classe PersonYear, andrò a fare la ricerca direttamente dentro quella classe sulla base della persona e dell'anno che mi
		 * interessa.
		 */
		if(month == DateTimeConstants.JANUARY){
			LocalDate lastDayOfYear = new LocalDate(year-1,DateTimeConstants.DECEMBER,31);
//			PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ?",person).first();
//			pastRemainingHours = py.remainingHours;
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, lastDayOfYear).first();
			pastRemainingHours = pd.progressive;
		}
		else{
			//int month = date.getMonthOfYear();
			int counter = 0;
			for(int i = 1; i < month; i++){
				int day = 0;
				if(i==1 || i==3 || i==5 || i==7 || i==8 || i==10 || i==12)
					day = 31;
				if(i==4 || i==6 || i==9 || i==11)
					day = 30;
				if(i==2){
					if(year==2012 || year==2016 || year==2020)
						day = 29;
					else
						day = 28;
				}			
				LocalDate endOfMonth = new LocalDate(year,i,day);	
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? " +
						"and pd.date = ?", person, endOfMonth).first();
				counter = counter+pd.progressive;
			}
			pastRemainingHours = counter;
			
		}
		
		return pastRemainingHours;
	}
	
	
}
