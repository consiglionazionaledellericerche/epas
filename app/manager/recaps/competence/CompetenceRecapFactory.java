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

import dao.CompetenceDao;
import dao.PersonDao;
import javax.inject.Inject;
import manager.CompetenceManager;
import models.CompetenceCode;
import models.Office;

/**
 * Factory per i CompetenceRecap.
 */
public class CompetenceRecapFactory {

  private final PersonDao personDao;
  private final CompetenceManager competenceManager;
  private final CompetenceDao competenceDao;
  
  /**
   * Costruttore di default per l'injection.
   */
  @Inject
  CompetenceRecapFactory(PersonDao personDao, CompetenceManager competenceManager, 
      CompetenceDao competenceDao) {
    this.competenceDao = competenceDao;
    this.competenceManager = competenceManager;
    this.personDao = personDao;
  }
  
  /**
   * Fornisce una nuova istanza di CompetenceRecap.
   */
  public CompetenceRecap create(Office office, CompetenceCode code, 
      int year, int month) {
    return new CompetenceRecap(personDao, competenceManager, competenceDao, 
        year, month, office, code);
  }
  
}
