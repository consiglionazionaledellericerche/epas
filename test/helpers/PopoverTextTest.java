/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package helpers;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import play.templates.TemplateLoader;
import play.test.UnitTest;

/**
 * Verifica che il testo inserito dagli utenti nel data-content di un webui-popover (che lo
 * mostra come HTML) non venga interpretato come markup.
 */
public class PopoverTextTest extends UnitTest {

  private static final String PAYLOAD = "<img src=x onerror=alert(document.domain)>";

  @Test
  public void popoverTextIsEncodedTwiceInTheAttribute() {
    String html = render("<i webui-popover-hover data-content=\"${note?.popoverText()}\"></i>",
        PAYLOAD);
    // il browser decodifica l'attributo in "&lt;img ...&gt;", che il popover mostra come testo
    assertEquals("<i webui-popover-hover data-content=\""
        + "&amp;lt;img src=x onerror=alert(document.domain)&amp;gt;\"></i>", html);
  }

  @Test
  public void nullNoteRendersEmpty() {
    assertEquals("<i data-content=\"\"></i>",
        render("<i data-content=\"${note?.popoverText()}\"></i>", null));
  }

  /**
   * Il nome .html attiva la codifica automatica di ${} come nelle viste.
   */
  private String render(String template, String note) {
    Map<String, Object> args = new HashMap<>();
    args.put("note", note);
    return TemplateLoader.load("PopoverTextTest" + template.hashCode() + ".html", template)
        .render(args);
  }
}
