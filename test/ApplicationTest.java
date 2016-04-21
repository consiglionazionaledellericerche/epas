import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import play.mvc.Http.Response;
import play.test.FunctionalTest;

@Slf4j
public class ApplicationTest extends FunctionalTest {

  @Test
  public void testThatLoginPageWorks() {
    Response response = GET("/login", true);
    assertIsOk(response);
    log.debug("contentType = {}", response.contentType);
    log.debug("Response content = {}", getContent(response));
    log.debug("Response status = {}", response.status);
    //FIXME: verificare come mai il contentType non è impostato in alcuni casi
    //assertContentType("text/html", response);
  }

}
