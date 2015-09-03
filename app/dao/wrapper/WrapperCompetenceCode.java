package dao.wrapper;

import java.util.List;

import models.Competence;
import models.CompetenceCode;
import models.Office;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.CompetenceDao;
import dao.OfficeDao;

/**
 * @author alessandro
 *
 */
public class WrapperCompetenceCode implements IWrapperCompetenceCode {

	private final CompetenceCode value;
	private final CompetenceDao competenceDao;
	private final OfficeDao officeDao;
	
	@Inject
	WrapperCompetenceCode(@Assisted CompetenceCode cc, OfficeDao officeDao, CompetenceDao competenceDao) {
		this.value = cc;
		this.competenceDao = competenceDao;
		this.officeDao = officeDao;
	}

	@Override
	public CompetenceCode getValue() {
		return value;
	}
	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale per quel mese e quell'anno di ore/giorni relativi a quel codice competenza
	 */
	public int totalFromCompetenceCode(int month, int year, Long officeId){
		
		Office office = officeDao.getOfficeById(officeId);

		int totale = 0;
		List<String> competenceCodeList = Lists.newArrayList();
		competenceCodeList.add(this.value.code);
		
		List<Competence> compList = competenceDao.getCompetencesInOffice(year, month, 
				competenceCodeList, office, false);

		for(Competence comp : compList){
			totale = totale+comp.valueApproved;
		}
		return totale;
	}

}
