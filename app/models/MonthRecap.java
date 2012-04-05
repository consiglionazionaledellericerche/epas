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
		Logger.debug("Stampingcodelist nel costruttore di default: "+stampingCodeList);
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
		Logger.debug("Stampingcodelist nel costruttore: "+stampingCodeList);
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
			days.add(new PersonDay(person, new LocalDate(year, month, day)));
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
	 * @param month
	 * @param year
	 * @return massimo numero di giorni in un mese per coadiuvare nella realizzazione della tabella per le assenze annuali
	 */
	public int maxNumberOfDays(int year, String month){
		int max = 0;
		
		if(month.equalsIgnoreCase("aprile") || month.equalsIgnoreCase("giugno") || month.equalsIgnoreCase("settembre")
				|| month.equalsIgnoreCase("novembre"))
			max=30;
		if(month.equalsIgnoreCase("gennaio") || month.equalsIgnoreCase("marzo") || month.equalsIgnoreCase("maggio")
				|| month.equalsIgnoreCase("luglio") || month.equalsIgnoreCase("agosto") || month.equalsIgnoreCase("ottobre")
				|| month.equalsIgnoreCase("dicembre"))
			max=31;
		if(month.equalsIgnoreCase("febbraio") && (year==2008 || year==2012 || year==2016 || year==2020))
			max=29;
		else
			max=28;
		return max;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return la totalità delle assenze per quella persona in un anno
	 */
	public List<Absence> getAbsenceInYear(int year, String month, int day){
		LocalDate date = null;
		List<Absence> absences = null;
		if(absences==null){				
			
			if(month.equalsIgnoreCase("gennaio")){
				date = new LocalDate(year,1,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("febbraio")){
				if(day<30){
					date = new LocalDate(year,2,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();
				}
			}
			if(month.equalsIgnoreCase("marzo")){
				date = new LocalDate(year,3,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("aprile")){
				if(day<31){
					date = new LocalDate(year,4,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("maggio")){
				date = new LocalDate(year,5,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("giugno")){
				if(day<31){
					date = new LocalDate(year,6,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
				}
					
			}
			if(month.equalsIgnoreCase("luglio")){
				date = new LocalDate(year,7,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("agosto")){
				date = new LocalDate(year,8,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("settembre")){
				if(day<31){
					date = new LocalDate(year,9,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("ottobre")){
				date = new LocalDate(year,10,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
			if(month.equalsIgnoreCase("novembre")){
				if(day<31){
					date = new LocalDate(year,11,day);
					absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();	
				}
						
			}
			if(month.equalsIgnoreCase("dicembre")){
				date = new LocalDate(year,12,day);
				absences = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).fetch();		
			}
		}
		
		return absences;
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
	
//	private void test() {
//		for (Entry<AbsenceType, Integer> entry : absenceCodeMap.entrySet()) {
//			Logger.info("%s %s", entry.getKey(), entry.getValue());
//		}
//		for (AbsenceType at : absenceCodeMap.keySet()) {
//			Logger.info("%s %s", at, absenceCodeMap.get(at));
//		}
//	}
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
}
