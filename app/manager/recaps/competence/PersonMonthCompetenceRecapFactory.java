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

package manager.recaps.competence;

import com.google.common.base.Optional;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.wrapper.IWrapperFactory;
import javax.inject.Inject;
import models.Contract;

/**
 * Factory per i PersonMonthCompetenceRecap.
 */
public class PersonMonthCompetenceRecapFactory {

  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final IWrapperFactory wrapperFactory;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  PersonMonthCompetenceRecapFactory(CompetenceCodeDao competenceCodeDao,
                                    CompetenceDao competenceDao, IWrapperFactory wrapperFactory) {
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Il riepilogo competenze per il dipendente.
   *
   * @param contract requires not null.
   */
  public Optional<PersonMonthCompetenceRecap> create(Contract contract, int month, int year) {

    try {
      return Optional.fromNullable(new PersonMonthCompetenceRecap(competenceCodeDao,
          competenceDao, wrapperFactory, contract, month, year));

    } catch (Exception ex) {
      
      //impossibile costruire il recap... inizializzazione mancante.
      return Optional.<PersonMonthCompetenceRecap>absent();
    }
  }

}
