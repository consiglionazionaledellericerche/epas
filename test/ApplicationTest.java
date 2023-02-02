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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import play.mvc.Http.Response;
import play.test.FunctionalTest;

/**
 * Test di base per vedere che l'applicazione di avvi.
 *
 * @author Cristian Lucchesi
 *
 */
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
