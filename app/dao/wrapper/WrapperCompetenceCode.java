/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.wrapper;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dao.CompetenceDao;
import dao.OfficeDao;
import java.util.List;
import models.Competence;
import models.CompetenceCode;
import models.Office;


/**
 * Oggetto CompetenceCode con funzionalità aggiuntive.
 */
public class WrapperCompetenceCode implements IWrapperCompetenceCode {

  private final CompetenceCode value;
  private final CompetenceDao competenceDao;
  private final OfficeDao officeDao;

  @Inject
  WrapperCompetenceCode(
      @Assisted CompetenceCode cc, OfficeDao officeDao, CompetenceDao competenceDao) {
    this.value = cc;
    this.competenceDao = competenceDao;
    this.officeDao = officeDao;
  }

  @Override
  public CompetenceCode getValue() {
    return value;
  }

  /**
   * Il totale delle competenze per quel mese.
   *
   * @return il totale per quel mese e quell'anno di ore/giorni relativi a quel codice competenza.
   */
  public int totalFromCompetenceCode(int month, int year, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);

    int totale = 0;
    List<String> competenceCodeList = Lists.newArrayList();
    competenceCodeList.add(this.value.getCode());

    List<Competence> compList = competenceDao.getCompetencesInOffice(year, month,
            competenceCodeList, office, false);

    for (Competence comp : compList) {
      totale = totale + comp.getValueApproved();
    }
    return totale;
  }

}
