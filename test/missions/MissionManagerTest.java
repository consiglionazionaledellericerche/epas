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

package missions;

import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.PersonDao;
import lombok.val;
import manager.MissionManager;
import manager.services.absences.AbsenceService;
import models.exports.MissionFromClient;
import org.assertj.core.api.Assertions;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import org.junit.Test;
import play.test.UnitTest;

@StaticInject
public class MissionManagerTest extends UnitTest {

  private static final String PERSON_NUMBER = "9802";
  @Inject
  private static MissionManager missionManager;

  @Inject
  private static AbsenceService absenceService;
  
  @Inject
  private static PersonDao personDao;
  
  @Test
  public void createMission() {
    absenceService.enumInitializator();
    
    val currentYear = YearMonth.now().getYear();
    val person = personDao.getPersonByNumber(PERSON_NUMBER);
    
    val mission = MissionFromClient.builder()
        .id(currentYear * 100000 + 10101L)
        .anno(currentYear).codiceSede("222300")
        .destinazioneMissione("ITALIA").matricola(PERSON_NUMBER)
        .person(person)
        .dataInizio(LocalDateTime.now()).dataFine(LocalDateTime.now().plusDays(2))
        .build();

    
    val missionCreated = missionManager.createMissionFromClient(mission, true);
    Assertions.assertThat(missionCreated).isTrue();
    
  }
}