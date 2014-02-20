package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.Stamping.WayType;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
//@Audited
@Table(name="person_months")
@Entity
public class PersonMonth extends Model {

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false, updatable = false)
	public Person person;	

	
	public Integer year;
	
	public Integer month;

	@Transient
	private PersonMonth mesePrecedenteCache;

	@Column(name = "recuperi_ore_da_anno_precedente")
	public int recuperiOreDaAnnoPrecedente;

	@Column(name = "riposi_compensativi_da_anno_precedente")
	public int riposiCompensativiDaAnnoPrecedente;

	@Column(name = "riposi_compensativi_da_anno_corrente")
	public int riposiCompensativiDaAnnoCorrente;
	
	@Column(name = "riposi_compensativi_da_inizializzazione")
	public int riposiCompensativiDaInizializzazione;
	
	public int straordinari;

	@Transient
	public InitializationTime initializationTime;

	/**
	 * Minuti derivanti dalla somma dei progressivi giornalieri del mese 
	 */
	@Column
	@Deprecated
	public Integer progressiveAtEndOfMonthInMinutes = 0;

	/**
	 * Totale residuo minuti alla fine del mese
	 * 
	 * Per i livelli I - III, deriva da:
	 * 
	 *  progressiveAtEndOfMonthInMinutes + 
	 *  residuo mese precedente (che non si azzera all'inizio dell'anno) -
	 *  recuperi (codice 91)
	 * 
	 *  
	 *  Per i livelli IV - IX, deriva da:
	 *  
	 *  progressiveAtEndOfMonthInMinutes +
	 *  residuo anno precedente (totale anno precedente se ancora utilizzabile) -
	 *  remainingMinutePastYearTaken -
	 *  totale remainingMinutePastYearTaken dei mesi precedenti a questo +
	 *  residuo mese precedente -
	 *  recuperi (91) -
	 *  straordinari (notturni, diurni, etc)  
	 */
	@Column(name = "total_remaining_minutes")
	@Deprecated
	public Integer totalRemainingMinutes = 0;

	/**
	 * Minuti di tempo residuo dell'anno passato utilizzati questo
	 * mese (come riposo compensativo o come ore in negativo)
	 */
	@Column(name = "remaining_minute_past_year_taken")
	public Integer remainingMinutesPastYearTaken = 0;

	@Column(name = "compensatory_rest_in_minutes")
	public Integer compensatoryRestInMinutes = 0;

	@Column(name = "residual_past_year")
	public Integer residualPastYear = 0;

	@Transient
	public List<PersonDay> days = null;

	@Transient
	private Map<AbsenceType, Integer> absenceCodeMap;

	@Transient
	private List<StampModificationType> stampingCodeList = new ArrayList<StampModificationType>();

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminare
	 * @param person
	 * @param year
	 * @param month
	 */
	public PersonMonth(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;

	}


	@Override
	public String toString() {
		//FIXME: da sistemare
		return String.format("PersonMonth[%d] - person.id = %d, year = %s, month = %d, totalRemainingMinutes = %d, " +
				"progressiveAtEndOfMonthInMinutes = %d, compensatoryRestInMinutes = %d, remainingMinutesPastYearTakes = %d",
				id, person.id, year, month, totalRemainingMinutes, progressiveAtEndOfMonthInMinutes, compensatoryRestInMinutes, remainingMinutesPastYearTaken);
	}

	public static PersonMonth build(Person person, int year, int month){
		PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
		if(pm == null){
			pm = new PersonMonth(person, year, month);
			pm.create();
		}
		
		//pm.aggiornaRiepiloghi();
		return pm;

	}

	
	public static PersonMonth getInstance(Person person, int year, int month) {
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
		return personMonth;
	}



		
	/**
	 * La lista delle assenze restituite è prelevata in FETCH JOIN con le absenceType i personDay e la person 
	 * in modo da non effettuare ulteriori select.
	 * 
	 * @return la lista delle assenze che non sono di tipo internalUse effettuate in questo mese dalla persona relativa
	 * 	a questo personMonth.
	 * 
	 */
	public List<Absence> getAbsencesNotInternalUseInMonth() {
		return Absence.find(
				"SELECT abs from Absence abs JOIN FETCH abs.absenceType abt JOIN FETCH abs.personDay pd JOIN FETCH pd.person p "
					+ "WHERE p = ? AND pd.date BETWEEN ? AND ? AND abt.internalUse = false ORDER BY abt.code, pd.date, abs.id", 
					person, new LocalDate(year,month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public List<Competence> getCompetenceInMonthForUploadSituation(){
		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? " +
				"and comp.year = ? and comp.valueApproved > 0", person, month, year).fetch();
		Logger.trace("Per la persona %s %s trovate %d competenze approvate nei mesi di %d/%d", person.surname, person.name, competenceList.size(), month, year );
		return competenceList;
	}
	
}
