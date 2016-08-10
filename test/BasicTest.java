import org.junit.Test;

import models.enumerate.LimitUnit;
import play.test.UnitTest;

public class BasicTest extends UnitTest {

  @Test
  public void aVeryImportantThingToTest() {
    assertEquals(LimitUnit.getByDescription("minuti"), LimitUnit.minutes);
  }

}
