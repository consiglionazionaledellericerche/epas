package dao.wrapper;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import manager.CompetenceManager;
import manager.ContractManager;
import manager.PersonManager;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;
import dao.PersonDao;
import dao.PersonMonthRecapDao;

/**
 * @author marco
 *
 */
public class WrapperPerson implements IWrapperPerson {

	private final Person value;
	private final ContractManager contractManager;
	private final ContractDao contractDao;
	private final CompetenceManager competenceManager;
	private final PersonManager personManager;
	private final PersonDao personDao;
	private final PersonMonthRecapDao personMonthRecapDao;

	private Optional<Contract> currentContract = null;
	private Optional<WorkingTimeType> currentWorkingTimeType = null;
	private Optional<VacationPeriod> currentVacationPeriod = null;
	private Optional<ContractStampProfile> currentContractStampProfile = null;
	private Optional<ContractWorkingTimeType> currentContractWorkingTimeType = null;

	@Inject
	WrapperPerson(@Assisted Person person,	ContractManager contractManager,
			ContractDao contractDao, CompetenceManager competenceManager, 
			PersonManager personManager,PersonDao personDao,
			PersonMonthRecapDao personMonthRecapDao) {
		this.value = person;
		this.contractManager = contractManager;
		this.contractDao = contractDao;
		this.competenceManager = competenceManager;
		this.personManager = personManager;
		this.personDao = personDao;
		this.personMonthRecapDao = personMonthRecapDao;
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
			this.currentContract = Optional.fromNullable(
					contractDao.getContract(LocalDate.now(), value));

		return this.currentContract;
	}

	/**
	 * @param year
	 * @param month
	 * @return l'ultimo contratto attivo nel mese.
	 */
	public Optional<Contract> getLastContractInMonth(int year, int month) {

		List<Contract> contractInMonth = personManager.getMonthContracts(
				this.value, month, year);

		if ( contractInMonth.size() == 0) {
			return Optional.absent();
		}

		return Optional.fromNullable(contractInMonth.get(contractInMonth.size()-1));
	}

	/**
	 * @param year
	 * @param month
	 * @return il primo contratto attivo nel mese.
	 */
	public Optional<Contract> getFirstContractInMonth(int year, int month) {

		List<Contract> contractInMonth = personManager.getMonthContracts(
				this.value, month, year);

		if ( contractInMonth.size() == 0) {
			return Optional.absent();
		}

		return Optional.fromNullable(contractInMonth.get(0));
	}

	/**
	 * True se la persona è passata da determinato a indeterminato durante l'anno.
	 * 
	 * @param year
	 * @return
	 */
	public boolean hasPassToIndefiniteInYear(int year) {

		List<Contract> orderedContractInYear = personDao.getContractList(this.value,
				new LocalDate(year,1,1), new LocalDate(year,12,31));


		boolean hasDefinite = false;
		boolean hasPassToIndefinite = false;

		for (Contract contract : orderedContractInYear) {
			if(contract.expireContract != null) 
				hasDefinite = true;

			if (hasDefinite && contract.expireContract == null)
				hasPassToIndefinite = true;
		}

		return hasPassToIndefinite;
	}

	/**
	 * 
	 * @return
	 */
	public Optional<ContractStampProfile> getCurrentContractStampProfile() {

		if( this.currentContractStampProfile != null ) {
			return this.currentContractStampProfile;
		}

		if( this.currentContract == null ) {
			getCurrentContract();
		}

		if( ! this.currentContract.isPresent() ) {
			return Optional.absent();
		}

		this.currentContractStampProfile = contractManager.getContractStampProfileFromDate( 
				this.currentContract.get(), LocalDate.now());

		return this.currentContractStampProfile; 
	}

	/**
	 * 
	 * @return
	 */
	public Optional<WorkingTimeType> getCurrentWorkingTimeType(){

		if( this.currentWorkingTimeType != null ) {
			return this.currentWorkingTimeType;
		}

		if( this.currentContract == null ) {
			getCurrentContract();
		}

		if( !this.currentContract.isPresent())
			return Optional.absent();

		//ricerca
		for(ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				this.currentWorkingTimeType = Optional.fromNullable(cwtt.workingTimeType); 
				return this.currentWorkingTimeType;
			}
		}
		return Optional.absent();

	}

	/**
	 * 
	 * @return
	 */
	public Optional<ContractWorkingTimeType> getCurrentContractWorkingTimeType() {

		if( this.currentContractWorkingTimeType != null ) {
			return this.currentContractWorkingTimeType;
		}

		if( this.currentContract == null ) {
			getCurrentContract();
		}

		if( !this.currentContract.isPresent())
			return Optional.absent();

		//ricerca
		for(ContractWorkingTimeType cwtt : this.currentContract.get().contractWorkingTimeType)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(cwtt.beginDate, cwtt.endDate)))
			{
				this.currentContractWorkingTimeType = Optional.fromNullable(cwtt); 
				return this.currentContractWorkingTimeType;
			}
		}
		return Optional.absent();
	}

	/**
	 * 
	 * @return
	 */
	public Optional<VacationPeriod> getCurrentVacationPeriod() {

		if( this.currentVacationPeriod != null )
			return this.currentVacationPeriod;

		if( this.currentContract == null ) {
			getCurrentContract();
		}
		if( ! this.currentContract.isPresent() )
			return Optional.absent();

		//ricerca
		for(VacationPeriod vp : this.currentContract.get().vacationPeriods)
		{
			if(DateUtility.isDateIntoInterval(LocalDate.now(), new DateInterval(vp.beginFrom, vp.endTo)))
			{
				this.currentVacationPeriod = Optional.fromNullable(vp);
				return this.currentVacationPeriod;
			}
		}
		return Optional.absent();
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

		return competenceManager.positiveResidualInMonth(this.value, year, month)/60;
	}

	/**
	 * L'esito dell'invio attestati per la persona (null se non è ancora stato effettuato).
	 * @param year
	 * @param month
	 * @return 
	 */
	public CertificatedData getCertificatedData(int year, int month) {

		CertificatedData cd = personMonthRecapDao
				.getCertificatedDataByPersonMonthAndYear(this.value, month, year);
		return cd;
	}

}
