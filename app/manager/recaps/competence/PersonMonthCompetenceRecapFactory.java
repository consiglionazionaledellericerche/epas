package manager.recaps.competence;

import javax.inject.Inject;

import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Contract;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;

public class PersonMonthCompetenceRecapFactory {

	private final CompetenceCodeDao competenceCodeDao;
	private final CompetenceDao competenceDao;
	private final PersonResidualYearRecapFactory yearFactory;
	
	@Inject
	PersonMonthCompetenceRecapFactory(CompetenceCodeDao competenceCodeDao,
			CompetenceDao competenceDao, PersonResidualYearRecapFactory yearFactory) {
		this.competenceCodeDao = competenceCodeDao;
		this.competenceDao = competenceDao;
		this.yearFactory = yearFactory;
	}
	
	/**
	 * Il riepilogo competenze per il dipendente.
	 * @param contract requires not null.
	 * @param month
	 * @param year
	 * @return
	 */
	public PersonMonthCompetenceRecap create(Contract contract, int month,
			int year) {
		
		return new PersonMonthCompetenceRecap(competenceCodeDao,
				competenceDao, yearFactory,
				contract, month, year);
	}
	
}
