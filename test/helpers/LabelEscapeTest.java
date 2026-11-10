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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import play.templates.BaseTemplate;
import play.templates.TemplateLoader;
import play.test.UnitTest;

/**
 * Verifica che label() restituisca come HTML solo il testo dei messaggi dell'applicazione e
 * codifichi i testi che non sono chiavi dei messaggi (es. inseriti dagli utenti).
 */
public class LabelEscapeTest extends UnitTest {

  private static final String PAYLOAD = "<img src=x onerror=alert(document.domain)>";
  private static final String ESCAPED_PAYLOAD =
      "&lt;img src=x onerror=alert(document.domain)&gt;";
  private static final String HTML_KEY = "test.labelEscape.html";
  private static final String ARGS_KEY = "test.labelEscape.args";

  @Before
  public void addMessages() {
    Messages.defaults.setProperty(HTML_KEY, "<b>Messaggio</b>");
    Messages.defaults.setProperty(ARGS_KEY, "<b>Ciao</b> %s, hai %d richieste");
  }

  @After
  public void removeMessages() {
    Messages.defaults.remove(HTML_KEY);
    Messages.defaults.remove(ARGS_KEY);
  }

  @Test
  public void textThatIsNotAMessageKeyIsEscaped() {
    assertEquals("<p>" + ESCAPED_PAYLOAD + "</p>", render("<p>${text.label()}</p>", PAYLOAD));
  }

  @Test
  public void messageKeyKeepsItsHtml() {
    assertEquals("<p><b>Messaggio</b></p>", render("<p>${text.label()}</p>", HTML_KEY));
  }

  @Test
  public void existingMessageKeyIsTranslated() {
    assertTrue(TemplateExtensions.isMessageKey("notify.error"));
    assertEquals("<p>" + Messages.get("notify.error") + "</p>",
        render("<p>${text.label()}</p>", "notify.error"));
  }

  @Test
  public void textArgumentsOfAMessageAreEscaped() {
    assertEquals("<b>Ciao</b> " + ESCAPED_PAYLOAD + ", hai 3 richieste",
        ((BaseTemplate.RawData) TemplateExtensions.label(ARGS_KEY, PAYLOAD, 3)).data);
  }

  @Test
  public void textWithPercentIsEscaped() {
    assertEquals("<p>100% &lt;b&gt;</p>", render("<p>${text.label()}</p>", "100% <b>"));
  }

  /**
   * Il nome .html attiva la codifica automatica di ${} come nelle viste.
   */
  private String render(String template, String text) {
    Map<String, Object> args = new HashMap<>();
    args.put("text", text);
    return TemplateLoader.load("LabelEscapeTest" + template.hashCode() + ".html", template)
        .render(args);
  }
}
