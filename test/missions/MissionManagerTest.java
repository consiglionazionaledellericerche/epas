package missions;

import com.google.inject.Inject;
import dao.PersonDao;
import injection.StaticInject;
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

  private final static String PERSON_NUMBER = "9802";
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