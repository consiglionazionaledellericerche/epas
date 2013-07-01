package models;


import java.sql.Blob;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.enumerate.AuthWebStamping;
import models.enumerate.AutoDeclareAbsences;
import models.enumerate.AutoDeclareWorkingTime;
import models.enumerate.CapacityCompensatoryRestFourEight;
import models.enumerate.CapacityCompensatoryRestOneThree;
import models.enumerate.ResidualWithPastYear;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;


import play.data.validation.Email;
import play.db.jpa.Model;


@Audited
@Entity
@Table(name="configurations")
public class Configuration extends Model{
	
	/**
	 * booleano per stabilire se una configurazione è in uso (true) oppure no (false)
	 */
	@Column(name = "in_use")
	public boolean inUse = true;
	
	
	/**
	 * Data di inizio uso di questo programma gg/mm/aaaa
	 */
	@Column(name = "init_use_program")
	//@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public Date initUseProgram;
	
	/**
	 * date di inizio e fine validità della configurazione
	 */
	@Column(name = "begin_date")
	//@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public Date beginDate;
	
	@Column(name = "end_date")
	//@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public Date endDate;
		
	/**
	 * Nome dell'Istituto
	 */
	@Column(name = "institute_name")
	public String instituteName;
	
	/**
	 * Email a cui può essere inviato un messaggio in caso di password dimenticata
	 */	
	@Column(name = "email_to_contact")
	@Email
	public String emailToContact;
	
	/**
	 * Codice sede
	 */
	@Column(name = "seat_code")
	public Integer seatCode;
	
	/**
	 * URL per gestione attestato presenza dell'amministrazione centrale
	 */
	@Column(name = "url_to_presence")
	public String urlToPresence;
	
	/**
	 * User per gestione attestato presenza dell'amministrazione centrale
	 */
	@Column(name="user_to_presence")
	public String userToPresence;
	
	/**
	 * Password per gestione attestato presenza dell'amministrazione centrale
	 */	
	@Column(name="password_to_presence")
	public String passwordToPresence;
	
	/**
	 * il path assoluto in cui verrà salvato il file contenente assenze e competenze da inviare alla sede di Roma
	 */
	@Column(name="path_to_save_presence_situation")
	public String pathToSavePresenceSituation;
	
	/**
	 * i prossimi due campi sono il mese e il giorno del santo patrono...per adesso usiamo questa configurazione che è uguale alla 
	 * precedente per considerare il santo patrono. Poi si vedrà se esistono possibili varianti
	 */
	@Column(name="month_of_patron")
	public int monthOfPatron;
	
	@Column(name="day_of_patron")
	public int dayOfPatron;
	
	/**
	 * numero di colonne da visualizzare
	 */
	public int numberOfViewingCoupleColumn;
	
	/**
	 * Indicare se la durata dell'intervallo pranzo è più corta del minimo deve essere automaticamente spostata l'ora di rientro
	 */
	public boolean isMealTimeShorterThanMinimum;
	
	/**
	 * Se quando manca l'intervallo pranzo il tempo dell'intervallo deve essere tolto automaticamente dal tempo di lavoro
	 */
	public boolean isIntervalTimeCutFromWorkingTime;
	
	/**
	 * Se quando manca l'ora di rientro dopo l'intervallo pranzo essa deve essere calcolata automaticamente
	 */
	public boolean calculateIntervalTimeWithoutReturnFromIntevalTime;
	
	/**
	 * Buono mensa assegnato solo se c'è un intervallo pranzo reale (non calcolato automatcamente)
	 */
	public boolean mealTicketAssignedWithMealTimeReal;
	
	/**
	 * Buono mensa assegnato solo se c'è un intervallo reale con causale pausa mensa
	 */
	public boolean mealTicketAssignedWithReasonMealTime;
	
	/**
	 * Consente all'amministratore di inserire/modificare orari specificando il segno + per ridurre il tempo di lavoro 
	 * a quello definito nell'orario di lavoro
	 */
	public boolean insertAndModifyWorkingTimeWithPlusToReduceAtRealWorkingTime;
	
	/**
	 * Abilita la possibilità per l'amministratore di inserire tempo di lavoro in eccesso
	 */
	public boolean addWorkingTimeInExcess;
	
	/**
	 * L'ultimo giorno lavorativo prima di Natale deve essere considerato intero anche se le ore di lavoro sono minori?
	 */
	public boolean isLastDayBeforeXmasEntire;
	
	/**
	 * L'ultimo giorno lavorativo prima di Pasqua deve essere considerato intero anche se le ore di lavoro sono minori?
	 */
	public boolean isLastDayBeforeEasterEntire;
	
	/**
	 * L'ultimo giorno dell'anno deve essere considerato intero anche se le ore di lavoro sono minori? true se sì, false altrimenti
	 */
	public boolean isLastDayOfTheYearEntire;
	
	/**
	 * Se il primo o ultimo giorno di missione è festivo deve essere considerato come presenza al lavoro?
	 */
	public boolean isFirstOrLastMissionDayAHoliday;
	
	/**
	 * Giorno di missione festivo deve essere considerato come presenza al lavoro?
	 */
	public boolean isHolidayInMissionAWorkingDay;
	
	/**
	 * Tempo di lavoro giornaliero default (in minuti) valore medio se il tempo di lavoro cambia di giorno in giorno
	 */
	public Integer workingTime;
	
	/**
	 * Default tempo di lavoro giornaliero per avere il buono pasto (in minuti)
	 */
	public Integer workingTimeToHaveMealTicket;
	
	/**
	 * Default tempo intervallo pasto (in minuti)
	 */
	public Integer mealTime;
	
	/**
	 * Scadenza ferie dell'anno precedente mese
	 */
	public Integer monthExpiryVacationPastYear;
	
	/**
	 * Scadenza ferie dell'anno precedente giorno
	 */
	public Integer dayExpiryVacationPastYear;
	
	/**
	 * residuo minimo in minuti da avere a disposizione per poter richiedere un giorno di recupero 
	 */
	public Integer minimumRemainingTimeToHaveRecoveryDay;
	
	/**
	 * Mese entro il quale devono essere usati i residui dell'anno precedente per i livelli da 1 a 3 (mm) 
	 * 00 = i recuperi devono essere presi nell'anno. 99 = i recuperi non scadono mai
	 */
	public Integer monthExpireRecoveryDaysOneThree;
	
	/**
	 * Mese entro il quale devono essere usati i residui dell'anno precedente per i livelli da 4 a 9 (mm)
	 * 00 = i recuperi devono essere presi nell'anno. 99 = i recuperi non scadono mai
	 */
	public Integer monthExpireRecoveryDaysFourNine;
	
	/**
	 * Numero massimo di giorni di recupero in un anno per i livelli da 1 a 3
	 */
	public Integer maxRecoveryDaysOneThree;
	
	/**
	 * Numero massimo di giorni di recupero in un anno per i livelli da 4 a 9
	 */
	public Integer maxRecoveryDaysFourNine;
	
	/**
	 * Compensazione dei residui con quelli dell'anno precedente (è un enum dichiarato nel package it.cnr.iit.epas)
	 */
	public ResidualWithPastYear residual;
	
	/**
	 * Numero massimo di ore di straordinario mensili a persona
	 */
	public Integer maximumOvertimeHours;
	
	/**
	 * Selezionare "si" per permettere l'inserimento forzato di giorni di ferie/permessi che superano il massimo di giorni 
	 * usufruibili in un anno
	 */
	public boolean holydaysAndVacationsOverPermitted;
	
	/**
	 * Se c'è una uscita prima di questa ora e manca l'uscita finale del giorno precedente, considera lavoro a cavallo della mezzanotte
	 */
	public Integer hourMaxToCalculateWorkTime;
	
//	/**
//	 * Permette al personale di autodichiarare gli orari di lavoro (enum dichiarato nel package it.cnr.iit.epas
//	 */
//	public AutoDeclareWorkingTime autoDeclare;
//	
//	/**
//	 * Permette al personale di autodichiarare le assenze
//	 */
//	public AutoDeclareAbsences autoAbsence;
	
	/**
	 * Controllo capienza riposi compensativi livelli 1-3
	 */
	public CapacityCompensatoryRestOneThree capacityOneThree;
	
	/**
	 * Controllo capienza riposi compensativi livelli 4-8
	 */
	public CapacityCompensatoryRestFourEight capacityFourEight;
	
	/**
	 * Abilita possibilita' di inserire piu' di un codice di assenza per giorno
	 */
	
	public boolean canInsertMoreAbsenceCodeInDay;
	
	/**
	 * Ignora tempo di lavoro in un giorno con anche un codice di assenza che giustifica tutto il giorno (es. lavoro in ferie o missione ...)
	 */
	public boolean ignoreWorkingTimeWithAbsenceCode;
	
//	/**
//	 * Autorizzazione timbratura via web
//	 */
//	public AuthWebStamping authWeb;
	/**
	 * concede al personale di timbrare via web
	 */
	public boolean canPeopleUseWebStamping;
	
	/**
	 * concede al personale di autodichiarare l'orario di lavoro
	 */
	public boolean canPeopleAutoDeclareWorkingTime;
	
	/**
	 * concede al personale di autodichiarare le assenze
	 */
	public boolean canPeopleAutoDeclareAbsences;
	
	/**
	 * Eventuale testo da scrivere in fondo alla stampa in pdf della situazione mensile (codifica html)
	 */
	public String textForMonthlySituation;
	
	/**
	 * legame con la lista degli indirizzi ip che possono usufruire delle timbrature via web
	 */
	@OneToMany (mappedBy="confParameters", fetch = FetchType.LAZY)
	public List<WebStampingAddress> webStampingAddress;
	
	
	public static Configuration getConfiguration(Date date){
		return Configuration.find("Select conf from Configuration conf where conf.inUse = ? order by conf.endDate desc", true).first();
		//return Configuration.find("Select conf from Configuration conf where conf.beginDate <= ? and conf.endDate >= ?", date, date).first();
	}
	
	public static Configuration getCurrentConfiguration(){
		//TODO: metterla nella cache
		return getConfiguration(new Date());
	}
	
}
