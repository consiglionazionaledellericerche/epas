package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import manager.ContractManager;
import manager.recaps.PersonResidualMonthRecap;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;
import dao.PersonMonthRecapDao;

/**
 * @author marco
 *
 */
public class WrapperPerson implements IWrapperPerson {

	private final Person value;
	private final ContractManager contractManager;
	private final ContractDao contractDao;

	private Optional<Contract> currentContract;
	private WorkingTimeType currentWorkingTimeType = null;
	private VacationCode currentVacationCode = null;

	@Inject
	WrapperPerson(@Assisted Person person,	ContractManager contractManager,
			ContractDao contractDao) {
		this.value = person;
		this.contractManager = contractManager;
		this.contractDao = contractDao;
	}

	@Override
	public Person getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return il contratto attualmente attivo per quella persona
	 */
	public Optional<Contract> getCurrentContract() {
		
		if( this.currentContract != null )
			return this.currentContract;
		
		if( this.currentContract == null )
			this.currentContract = Optional.fromNullable(contractDao.getContract(LocalDate.now(), value));

		return this.currentContract;
	}

	public ContractStampProfile getCurrentContractStampProfile() {
		
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		
		if( !this.currentContract.isPresent() ) {
			return null;
		}
		
		return contractManager.getContractStampProfileFromDate( this.currentContract.get(),
				LocalDate.now());
	}
	
	public  WorkingTimeType getCurrentWorkingTimeType(){
		
		if( this.currentWorkingTimeType != null ) {
			return this.currentWorkingTimeType;
		}
		
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		
		if( !this.currentContract.isPresent())
			return null;
		
		//ricerca
		for(ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				this.currentWorkingTimeType = cwtt.workingTimeType; 
				return currentWorkingTimeType;
			}
		}
		return null;

	}

	public VacationCode getCurrentVacationCode() {

		if( this.currentVacationCode != null )
			return this.currentVacationCode;
				
		if( this.currentContract == null ) {
			getCurrentContract();
		}
		if( ! this.currentContract.isPresent() )
			return null;
		
		//ricerca
		for(VacationPeriod vp : this.currentContract.get().vacationPeriods)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(vp.beginFrom, vp.endTo)))
			{
				this.currentVacationCode = vp.vacationCode;
				return this.currentVacationCode;
			}
		}
		return null;
	}
	
	
	/**
	 * Getter per la competenza della persona <CompetenceCode, year, month>
	 * @param code
	 * @return la competenza della person nell'anno year e mese month con il codice competenza code
	 */
	public Competence competence(final CompetenceCode code, final int year, final int month) {
		if (value.competenceCode.contains(code)) {
			Optional<Competence> o = FluentIterable.from(value.competences)
					.firstMatch(new Predicate<Competence>() {

				@Override
				public boolean apply(Competence input) {

					return input.competenceCode.equals(code) && input.year == year && input.month == month;
				}

			});
			return o.orNull();
		} else {
			return null;
		}
	}
	
	/**
	 * Il residuo positivo del mese fatto dalla person.
	 * @param year
	 * @param month
	 * @return 
	 */
	public Integer getPositiveResidualInMonth(int year, int month) {

		return PersonResidualMonthRecap
				.positiveResidualInMonth(this.value, year, month)/60;
	}
	
	/**
	 * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
	 * @param year
	 * @param month
	 * @return 
	 */
	public CertificatedData getCertificatedData(int year, int month) {

		CertificatedData cd = PersonMonthRecapDao
				.getCertificatedDataByPersonMonthAndYear(this.value, month, year);
		return cd;
	}

	
}
