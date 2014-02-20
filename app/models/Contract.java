package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */

@Entity
@Table(name="contracts")
public class Contract extends Model {

	private static final long serialVersionUID = -4472102414284745470L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;

	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<VacationPeriod> vacationPeriods;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<ContractYearRecap> recapPeriods;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_contract")
	public LocalDate beginContract;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_contract")
	public LocalDate expireContract;

	//data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_contract")
	public LocalDate endContract;

	//TODO eliminare e configurare yaml
	public void setBeginContract(String date){
		this.beginContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setEndContract(String date){
		this.endContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setExpireContract(String date){
		this.expireContract = new LocalDate(date);
	}

	/**
	 * I contratti con onCertificate = true sono quelli dei dipendenti CNR e 
	 * corrispondono a quelli con l'obbligo dell'attestato di presenza 
	 * da inviare a Roma
	 */
	@Required
	public boolean onCertificate = false;

	@Transient
	public boolean isValidContract(){
		LocalDate date = new LocalDate();
		return endContract==null && beginContract.isBefore(date) && expireContract.isAfter(date);

	}

	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}

	/**
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public VacationPeriod getCurrentVacationPeriod()
	{
		List<VacationPeriod> vpList = this.getContractVacationPeriods();
		for(VacationPeriod vp : vpList)
		{

			LocalDate now = new LocalDate();

			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
		}
		return null;
	}

	/**
	 * @param contract
	 * @return i vacation period associati al contratto, ordinati in ordine crescente per data inizio
	 * 		 	null in caso di vacation period inesistente
	 */
	public List<VacationPeriod> getContractVacationPeriods()
	{
		//vacation period piu' recente per la persona
		List<VacationPeriod> vpList = VacationPeriod.find(  "SELECT vp "
				+ "FROM VacationPeriod vp "
				+ "WHERE vp.contract = ? "
				+ "ORDER BY vp.beginFrom",
				this).fetch();

		//se il piano ferie associato al contratto non esiste 
		if(vpList==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}


		return vpList;
	}

	/**
	 * Costruisce la struttura valida dei vacation period associati al contratto.
	 */
	public void setVacationPeriods(){


		//Distruggo i vacation period precedenti
		if(this.vacationPeriods != null){
			for(VacationPeriod vp : this.vacationPeriods)
			{
				vp.delete();
			}
		}

		//Contratto terminato nessun vacation period
		if(this.endContract!=null)
		{
			this.save();
			return;
		}

		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(this.expireContract == null)
		{

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(this.expireContract.isAfter(this.beginContract.plusYears(3).minusDays(1))){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = this.expireContract;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = this.beginContract;
		first.endTo = this.expireContract;
		first.contract = this;
		first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
		first.save();
		this.save();
	}
	
	
	public void setRecapPeriods(LocalDate initUse, InitializationTime initPerson)
	{
		//Calcolo intervallo con dati utili database
		DateInterval personDatabaseInterval;
		if(initPerson.date.isAfter(initUse))
			personDatabaseInterval = new DateInterval(initPerson.date, new LocalDate());
		else
			personDatabaseInterval = new DateInterval(initUse, new LocalDate());

		//Calcolo intervallo esistenza contratto
		DateInterval contractInterval;
		if(this.endContract!=null)
			contractInterval = new DateInterval(this.beginContract, this.endContract);
		else
			contractInterval = new DateInterval(this.beginContract, this.endContract);
		
		
		//Calcolo intersezione fra contratto e dati utili database
		DateInterval intersection = DateUtility.intervalIntersection(contractInterval, personDatabaseInterval);
		
		//se vuota non costruisco alcun contractYearRecap
		if(intersection==null)
			return;
		
		//altrimenti almeno uno lo devo costruire
		if(personDatabaseInterval.getBegin().isBefore(contractInterval.getBegin()))
		{
			//contratto interamente contenuto nel database
			
			
		}
		else
		{
			//una parte di contratto non ha dati presenti nel db, devo sfruttare init time se è relativo a tale contratto
			if(DateUtility.isDateIntoInterval(initPerson.date, contractInterval))
			{
				//se init è il primo gennaio costruisco 
			}
			
			
			
		}
			
		

	}

}
