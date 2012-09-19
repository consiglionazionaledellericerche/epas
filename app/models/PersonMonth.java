package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

import lombok.Data;

@Audited
@Table(name="person_months")
@Entity
public class PersonMonth extends Model {
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;	
	
	@Column
	public Integer year;
	@Column
	public Integer month;	
	
	@Column
	public Integer remainingHours;
	
	@Column
	public Integer compensatoryRest;
	
	@Transient
	public List<PersonMonth> persons = null;
	
	@Transient
	public List<PersonDay> days = null;
	
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
	

	
	/**
	 * 
	 * @param month, year
	 * @return il residuo di ore all'ultimo giorno del mese se visualizzo un mese passato, al giorno attuale se visualizzo il mese
	 * attuale
	 */
	public int getMonthResidual(){
		int residual = 0;
		LocalDate date = new LocalDate();
		
		if(month == date.getMonthOfYear() && year == date.getYear()){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date < ? and pd.progressive != ? " +
					"order by pd.date desc", person, date, 0).first();
			if(pd == null){
				pd = new PersonDay(person, date.minusDays(1));
			}
			residual = pd.progressive;
		}
		else{
			LocalDate hotDate = new LocalDate(year,month,1).dayOfMonth().withMaximumValue();
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, hotDate).first();
			residual = pd.progressive;
		}
		return residual;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return il numero di giorni di riposo compensativo utilizzati in quel mese 
	 */
	public int getCompensatoryRest(){
		if (compensatoryRest != null) {
			return compensatoryRest;
		}
		
		compensatoryRest = 0;
		LocalDate beginMonth = new LocalDate(year, month, 1);

		return ((Long) Absence.find("Select count(abs) from Absence abs where abs.person = ? and abs.date between ? and ? and abs.absenceType.code = ?", 
				person, beginMonth, beginMonth.dayOfMonth().withMaximumValue(), "91").first()).intValue();
		
	}
	
	/**
	 * 
	 * @return il numero di minuti residui dell'anno precedente per quella persona
	 */
	public int getResidualPastYear(){
		
		int residual = 0;
		PersonYear py = PersonYear.findById(person);
		residual = py.remainingHours;
		return residual;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale derivante dalla differenza tra le ore residue e le eventuali ore di riposo compensativo
	 */
	public int getTotalOfMonth(){
		int total = 0;
		int compensatoryRest = getCompensatoryRest();
		int monthResidual = getMonthResidual();
		LocalDate date = new LocalDate(year, month, 1);
		int residualFromPastMonth = PersonUtility.getResidual(person, date.dayOfMonth().withMaximumValue());
		total = residualFromPastMonth+monthResidual-(compensatoryRest*432); //numero di giorni di riposo compensativo moltiplicati 
		//per il numero di minuti presenti in 7 ore e 12 minuti, ovvero il tempo di lavoro.
		
		return total;
	}
	
	/**
	 * 
	 * @return il numero massimo di coppie di colonne ingresso/uscita ricavato dal numero di timbrature di ingresso e di uscita di quella
	 * persona per quel mese
	 */
	public long getMaximumCoupleOfStampings(){
		EntityManager em = em();
		LocalDate begin = new LocalDate(year, month, 1);
		
		Query q1 = em.createNativeQuery("select count(*) from stampings as st where st.stamp_type_id in (:in1,:in2) and st.person_id = :per "+
				"and st.date between :beg and :end group by cast(date as Date) order by count(*) desc")
				.setParameter("in1", 1L)
				.setParameter("in2", 4L)
				.setParameter("per", person.id)
				.setParameter("beg", begin.toDate())
				.setParameter("end", begin.dayOfMonth().withMaximumValue().toDate())
				.setMaxResults(1);
		
		BigInteger exitStamp = (BigInteger)q1.getSingleResult();
		
		
		q1.setParameter("in1", 2L).setParameter("in2", 3L);
		BigInteger inStamp = (BigInteger)q1.getSingleResult();
		return Math.max(exitStamp.longValue(),inStamp.longValue());
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
	 * !!!!IMPORTANTE!!!! QUANDO SI PASSA A UN NUOVO CONTRATTO NELL'ARCO DI UN MESE, DEVO RICORDARMI DI AZZERARE I RESIDUI DELLE ORE PERCHÈ
	 * NON SONO CUMULABILI CON QUELLE CHE EVENTUALMENTE AVRÒ COL NUOVO CONTRATTO
	 * 
	 * 
	 */
	
	public void update(){
		
			LocalDate date = new LocalDate(year, month, 1);
			PersonDay lastPersonDayOfMonth = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?",person, date.dayOfMonth().withMaximumValue()).first();
			if(date.getMonthOfYear() == DateTimeConstants.JANUARY){
				int yearProgressive = 0;
				PersonYear py = new PersonYear(person, date.getYear()-1);
				List<PersonMonth> personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
						"pm.year = ? and pm.month between ? and ?", person, date.getYear()-1, DateTimeConstants.APRIL, DateTimeConstants.DECEMBER).fetch();
				for(PersonMonth permon : personMonth){
					yearProgressive = yearProgressive+permon.remainingHours;
					
				}
//				py.remainingHours = yearProgressive;
//				py.save();
//				pm = new PersonMonth(person, date.getYear()-1, 12);
//			}
//			else{
//				pm = new PersonMonth(person,date.getYear(),date.getMonthOfYear()-1);
//			}
//			pm.compensatoryRest = pm.getCompensatoryRest();
//
//			pm.remainingHours = lastPersonDayOfMonth.progressive - pm.compensatoryRest*(432); //durata in minuti di 7:12 ore relative al giorno di riposo compensativo
//			
//			pm.save();
//		
			}
	}
}
