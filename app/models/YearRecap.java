package models;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.joda.time.LocalDate;

import net.sf.oval.constraint.Min;
import net.sf.oval.constraint.NotNull;
import net.sf.oval.constraint.Range;

import play.Logger;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 * 
 * Per adesso la classe Year recap contiene la stessa struttura della tabella presente sul db Mysql per 
 * l'applicazione Orologio. Deve essere rivista.
 */
@Entity
@Table(name = "year_recaps")
public class YearRecap extends Model{
	
	private static final long serialVersionUID = -5721503493068567394L;

	@ManyToOne
	@JoinColumn(name = "person_id")
	public Person person;
	
	@Column
	public short year;
	@Column
	public int remaining;
	@Column
	public int remainingAp;
	@Column
	public int recg;
	@Column
	public int recgap;
	@Column
	public int overtime;
	@Column
	public int overtimeAp;
	@Column
	public int recguap;
	@Column
	public int recm;
	@Column
	public Timestamp lastModified;
	@Transient
	private boolean persistent = false;
	@Transient
	private List<String> months = null;
	@Transient
	private Map<AbsenceType,Integer> mappaAssenze = new HashMap<AbsenceType,Integer>();
	
	protected YearRecap(){
		
		
	}
	/**
	 * Construttore di default con i parametri obbligatori
	 * 
	 * @param person la persona associata al riepilogo annuale
	 * @param year l'anno di riferimento
	 * 
	 */
	public YearRecap(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		this.person = person;
		this.year = year;		
		
	}
	
	/**
	 * Preleva dallo storage le il YearRecap relativo ai dati passati.
	 * Se il yearRecap non è presente sul db ritorna un'istanza vuota
	 * ma con associati i dati passati.
	 * 
	 * @param person la persona associata al riepilogo mensile
	 * @param year l'anno di riferimento
	 * @return il riepilogo annuale, se non è presente nello storage viene 
	 * 	restituito un riepilogo annuale vuoto
	 */
	public static YearRecap byPersonAndYear(
			@NotNull Person person, 
			@Min(1970) short year
			) {
		if (person == null) {
			throw new IllegalArgumentException("Person mandatory");
		}
		YearRecap yearRecap = YearRecap.find("byPersonAndYear", person, year).first();
		if (yearRecap == null) {
			return new YearRecap(person, year);
		}
		yearRecap.persistent  = true;
		return yearRecap;
	}
	
	/**
	 * QUI INIZIA LA PARTE DELLE FUNZIONI CHE MI SERVONO PER IL CALCOLO DELLE ASSENZE ANNUALI CHE LA PERSONA HA FATTO
	 */
	
	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<String> getMonths() {

		if (months != null) {
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
		if(month.equalsIgnoreCase("febbraio")){
			if(year == 2012 || year == 2016 || year == 2020 )
				max = 29;
			else
				max = 28;
		}
		return max;
	}
	
	/**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return la totalità delle assenze per quella persona in un anno, in più aggiorna la lista privata yearlyAbsences nel caso in 
	 * cui trova un assenza con un codice che ancora non è stato inserito
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
	 * @return la mappa contenente le assenze fatte dalla persona nell'anno con i relativi codici d'assenza e descrizioni
	 */
	public Map<AbsenceType,Integer> getYearlyAbsence(){
		
		List<AbsenceType> listaAssenze = null;
		LocalDate dateFrom = new LocalDate(year,1,1);
		LocalDate dateTo = new LocalDate(year,12,31);
		listaAssenze = AbsenceType.find("SELECT abt FROM Absence abs, AbsenceType abt, Person p WHERE abt = abs.absenceType AND " +
				"abs.person = p AND p = ? AND abs.date >= ? AND abs.date < ?", person, dateFrom, dateTo).fetch();
		Logger.warn("ListaAssenze: " +listaAssenze);
		if(mappaAssenze.isEmpty()){
			Integer i = 0;
			for(AbsenceType absenceType : listaAssenze){
				boolean stato = mappaAssenze.containsKey(absenceType);
				if(stato==false){
					i=1;
					mappaAssenze.put(absenceType, i);
				}
				else{
					i = mappaAssenze.get(absenceType);
					mappaAssenze.remove(absenceType);
					mappaAssenze.put(absenceType, i+1);
				}
				
					
			}
		}
		
		Logger.warn("mappaAssenze: " +mappaAssenze);
		return mappaAssenze;
	}
	
	/**
	 * QUI INIZIA LA PARTE DI FUNZIONI RELATIVE AL CALCOLO DELLE FERIE IN UN ANNO, SIA TRA QUELLE PREVISTE DA CONTRATTO, SIA TRA QUELLE
	 * CHE NON SONO STATE UTILIZZATE TRA QUELLE DELL'ANNO PRECEDENTE.
	 */
	
	/**
	 * 
	 * @return il numero di giorni di ferie maturati nell'anno corrente con l'ausilio di un metodo di codifica privato
	 */
	public int vacationCurrentYear(int year){
		int days = 0;
		int vacationDays = 0;
		Contract contract = Contract.find("Select con from Contract con where con.person = ? order by begincontract desc", person).first();
		LocalDate beginContract = new LocalDate(contract.beginContract);
		LocalDate now = new LocalDate().now();
		now.withYear(year);
		
		int difference = now.getYear()-beginContract.getYear();
		Logger.warn("difference is:" +difference+ "now.getYear is: "+now.getYear()+ "beginContract.getYear is: "+beginContract.getYear());
		if(now.getYear()-beginContract.getYear() < 3){
			if(now.getYear()-beginContract.getYear() < 1 && beginContract.getMonthOfYear() != 1 && beginContract.getDayOfMonth() != 1){
				LocalDate newDate = now.minusYears(beginContract.getYear()).minusMonths(beginContract.getMonthOfYear()).minusDays(beginContract.getDayOfMonth());
				days = newDate.getDayOfMonth();
				vacationDays = convertWorkDaysToVacationDaysLessThreeYears(days);
				
			}
			else{
				days = now.getDayOfYear();
				vacationDays = convertWorkDaysToVacationDaysLessThreeYears(days);
			}
		}
		else{
			days = now.getDayOfYear();
			vacationDays = convertWorkDaysToVacationDaysMoreThreeYears(days);
		}
		
		return vacationDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di permesso legge maturati nell'anno corrente
	 */
	public int permissionCurrentYear(int year){
		int days = 0;
		int permissionDays = 0;
		LocalDate now = new LocalDate().now();
		now.withYear(year);
		days = now.getDayOfYear();
		
		permissionDays = convertWorkDaysToPermissionDays(days);
		return permissionDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di ferie avanzati da quelli maturati l'anno precedente e non ancora utilizzati
	 */
	public int vacationLastYearNotYetUsed(int year){
		int actualVacationDays = 0;
		LocalDate date = new LocalDate(year,1,1);
		//recupero dal db le assenze fatte quest'anno che hanno codice 4 ovvero quelle con causale "residuo anno precedente"
		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? and abs.date > ? " +
				"and abs.absenceType = abt and abt.code = ?", person, date, "31").fetch();
		LocalDate beginLastYear = new LocalDate(year-1,1,1);
		LocalDate endLastYear = new LocalDate(year-1,12,31);
		//recupero dal db le assenze fatte l'anno precedente che hanno codice 5 ovvero quelle con causale "ferie anno corrente"		
		List<Absence> oldAbsence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? and abs.date >= ? and " +
				"abs.date < ? and abs.absenceType = abt and abt.code = ?", person, beginLastYear, endLastYear, "32").fetch();
		//con questa query vado a prendere il piano ferie previsto per la persona da cui vado a estrarre il numero di giorni di ferie 
		//ad essa attribuiti
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		int vacationDays = vacCode.vacationDays;
		int residualVacationDays = vacationDays-oldAbsence.size();
		if(residualVacationDays > 0){
			actualVacationDays = residualVacationDays-absence.size();
			
		}
		
		return actualVacationDays;
	}
		
	
	/**
	 * 
	 * @return il numero di giorni di permesso che la persona ha da contratto
	 */
	public int personalPermission(){
		int permissionDays = 0;
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		permissionDays = vacCode.permissionDays;
		return permissionDays;
	}
	
	
	/**
	 * 
	 * @return i giorni di permesso che, a oggi, la persona ha utilizzato
	 */
	public int personalPermissionUsed(){
		int permissionDays = 0;
		LocalDate now = new LocalDate().now();
		LocalDate beginYear = new LocalDate(now.getYear(),1,1);
		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? and " +
				"abs.date between ? and ? and abs.absenceType = abt and abt.code = ?", person, beginYear, now, "94").fetch();
		permissionDays = absence.size();
		return permissionDays;
	}
	
	/**
	 * 
	 * @return il numero di giorni di permesso che la persona ha da contratto
	 */
	public int personalVacationDays(){
		int vacationDays = 0;
		VacationCode vacCode = VacationCode.find("Select vc from VacationCode vc, VacationPeriod vp where vp.vacationCode = vc " +
				"and vp.person = ? order by vp.beginFrom desc", person).first();
		vacationDays = vacCode.vacationDays;
		return vacationDays;
	}
	
	/**
	 * 
	 * @param currentYear
	 * @return il numero di giorni di ferie presi l'anno precedente. Il numero di giorni di ferie corrisponde a tutte quelle giornate
	 * di assenza registrate sul database con codice 31, ovvero "ferie anno precedente", fatte nell'anno corrente, più le giornate
	 * di assenza registrate sul database con codice 32, ovvero "ferie anno corrente", fatte nell'anno precedente.
	 */
	public int vacationDaysLastYear(int currentYear){
		int vacationDaysLastYear = 0;
		LocalDate beginLastYear = new LocalDate(currentYear-1,1,1);
		LocalDate endLastYear = new LocalDate(currentYear-1,12,31);
		LocalDate now = new LocalDate().now();
		LocalDate beginYear = new LocalDate(currentYear,1,1);
		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? " +
				"and (abs.date between ? and ? and abs.absenceType = abt and abt.code = ? or abs.date between ? and ? and abs.absenceType = abt and abt.code = ?)", 
				person, beginLastYear, endLastYear, "32", beginYear, now,  "31").fetch();
		vacationDaysLastYear = absence.size();
		return vacationDaysLastYear;
	}
	
	/**
	 * 
	 * @param currentYear
	 * @return la lista delle assenze che utilizzerò nella finestra di popup per elencare le date in cui sono state fatte le assenze
	 * 
	 */
	public List<Absence> listVacationDaysLastYear(int currentYear){
		LocalDate beginLastYear = new LocalDate(currentYear-1,1,1);
		LocalDate endLastYear = new LocalDate(currentYear-1,12,31);
		LocalDate now = new LocalDate().now();
		LocalDate beginYear = new LocalDate(currentYear,1,1);
		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? " +
				"and (abs.date between ? and ? and abs.absenceType = abt and abt.code = ? or abs.date between ? and ? and abs.absenceType = abt and abt.code = ?)", 
				person, beginLastYear, endLastYear, "32", beginYear, now,  "31").fetch();
		return absence;
	}
	
	/**
	 * 
	 * @param currentYear
	 * @return il numero di giorni di ferire per l'anno corrente. Il numero di giorni di ferie corrisponde a tutte quelle giornate di
	 * assenza registrate sul database col codice 32 ovvero "ferie anno corrente"
	 */
	public int vacationDaysCurrentYear(int currentYear){
		int vacationDaysCurrentYear = 0;
		LocalDate now = new LocalDate().now();
		LocalDate beginYear = new LocalDate(currentYear,1,1);
		List<Absence> absence = Absence.find("Select abs from Absence abs, AbsenceType abt where abs.person = ? " +
				"and abs.date between ? and ? and abs.absenceType = abt and abt.code = ?", person, beginYear, now, "32").fetch();
		vacationDaysCurrentYear = absence.size();
		return vacationDaysCurrentYear;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in 
	 * istituto da meno di tre anni
	 */
	private int convertWorkDaysToVacationDaysLessThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days <= 15)
			vacationDays = 0;
		if(days >= 16 && days <= 45)
			vacationDays = 2;
		if(days >= 46 && days <= 75)
			vacationDays = 4;
		if(days >= 76 && days <= 106)
			vacationDays = 6;
		if(days >= 107 && days <= 136)
			vacationDays = 8;
		if(days >= 137 && days <= 167)
			vacationDays = 10;
		if(days >= 168 && days <= 197)
			vacationDays = 13;
		if(days >= 198 && days <= 227)
			vacationDays = 15;
		if(days >= 228 && days <= 258)
			vacationDays = 17;
		if(days >= 259 && days <= 288)
			vacationDays = 19;
		if(days >= 289 && days <= 319)
			vacationDays = 21;
		if(days >= 320 && days <= 349)
			vacationDays = 23;
		if(days >= 350 && days <= 365)
			vacationDays = 26;
		return vacationDays;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di ferie che corrispondono al numero di giorni lavorati dall'inizio dell'anno per chi lavora in
	 * istituto da più di tre anni
	 */
	private int convertWorkDaysToVacationDaysMoreThreeYears(int days){
		int vacationDays = 0;
		if(days >= 1 && days <= 15)
			vacationDays = 0;
		if(days >= 16 && days <= 45)
			vacationDays = 2;
		if(days >= 46 && days <= 75)
			vacationDays = 4;
		if(days >= 76 && days <= 106)
			vacationDays = 7;
		if(days >= 107 && days <= 136)
			vacationDays = 9;
		if(days >= 137 && days <= 167)
			vacationDays = 11;
		if(days >= 168 && days <= 197)
			vacationDays = 14;
		if(days >= 198 && days <= 227)
			vacationDays = 16;
		if(days >= 228 && days <= 258)
			vacationDays = 18;
		if(days >= 259 && days <= 288)
			vacationDays = 21;
		if(days >= 289 && days <= 319)
			vacationDays = 23;
		if(days >= 320 && days <= 349)
			vacationDays = 25;
		if(days >= 350 && days <= 365)
			vacationDays = 28;
		return vacationDays;
	}
	
	/**
	 * 
	 * @param days
	 * @return il numero di giorni di permesso legge spettanti al dipendente a seconda dei giorni di presenza
	 */
	private int convertWorkDaysToPermissionDays(int days){
		int permissionDays = 0;
		if(days >= 45 && days <= 135)
			permissionDays = 1;
		if(days >= 136 && days <= 225)
			permissionDays = 2;
		if(days >= 226 && days <= 315)
			permissionDays = 3;
		if(days >= 316 && days <= 365)
			permissionDays = 4;
		return permissionDays;
	}
	
}
