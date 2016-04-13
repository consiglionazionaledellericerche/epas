import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

public class ApplicationTest extends FunctionalTest {

  @Test
  public void testThatLoginPageWorks() {
    Response response = GET("/login");
    assertIsOk(response);
  }

}
