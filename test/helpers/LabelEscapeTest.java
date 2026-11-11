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
import play.mvc.Scope;
import play.templates.BaseTemplate;
import play.modules.gtengineplugin.TemplateLoader;
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
  public void nonTextArgumentsOfAMessageAreEscaped() {
    final Object person = new Object() {
      @Override
      public String toString() {
        return PAYLOAD;
      }
    };
    assertEquals("<b>Ciao</b> " + ESCAPED_PAYLOAD + ", hai 3 richieste",
        ((BaseTemplate.RawData) TemplateExtensions.label(ARGS_KEY, person, 3)).data);
  }

  @Test
  public void argumentsCannotReferenceOtherMessages() {
    final Object reference = new Object() {
      @Override
      public String toString() {
        return "&{" + HTML_KEY + "}";
      }
    };
    for (Object arg : new Object[] {reference, reference.toString()}) {
      final String data = ((BaseTemplate.RawData) TemplateExtensions.label(ARGS_KEY, arg, 3)).data;
      assertEquals("<b>Ciao</b> &amp;{" + HTML_KEY + "}, hai 3 richieste", data);
    }
  }

  @Test
  public void gstringLabelIsTranslated() {
    assertEquals("<p><b>Messaggio</b></p>",
        render("<p>${\"${text}\".label()}</p>", HTML_KEY));
  }

  @Test
  public void modalTitleIsEscaped() {
    assertTrue(render("#{modalAsync title:text}#{/modalAsync}", PAYLOAD)
        .contains("<h3>" + ESCAPED_PAYLOAD + "</h3>"));
  }

  @Test
  public void modalTitleKeyIsTranslated() {
    assertTrue(render("#{modalAsync title:text}#{/modalAsync}", HTML_KEY)
        .contains("<h3><b>Messaggio</b></h3>"));
  }

  @Test
  public void modalWithoutTitleIsRendered() {
    assertTrue(render("#{modalAsync}#{/modalAsync}", null).contains("<h3></h3>"));
  }

  @Test
  public void submitAndLinkLabelsAreEscaped() {
    assertTrue(render("#{b.submit text/}", PAYLOAD).contains(ESCAPED_PAYLOAD));
    assertFalse(render("#{b.submit text/}", PAYLOAD).contains(PAYLOAD));
    assertTrue(render("#{b.cancel '#', label:text/}", PAYLOAD).contains(ESCAPED_PAYLOAD));
    assertTrue(render("#{tabItem id:'t', title:text/}", PAYLOAD).contains(ESCAPED_PAYLOAD));
  }

  @Test
  public void flashTextIsEscaped() {
    final Scope.Flash flash = new Scope.Flash();
    flash.error("Eliminato controllo per %s", PAYLOAD);
    final String html = renderFlash(flash);
    assertTrue(html, html.contains("Eliminato controllo per " + ESCAPED_PAYLOAD));
    assertFalse(html, html.contains(PAYLOAD));
  }

  @Test
  public void flashMessageKeyIsTranslated() {
    final Scope.Flash flash = new Scope.Flash();
    flash.success("secure.logout");
    assertTrue(renderFlash(flash).contains(Messages.get("secure.logout")));
  }

  @Test
  public void textWithPercentIsEscaped() {
    assertEquals("<p>100% &lt;b&gt;</p>", render("<p>${text.label()}</p>", "100% <b>"));
  }

  private String renderFlash(Scope.Flash flash) {
    Map<String, Object> args = new HashMap<>();
    args.put("flash", flash);
    return TemplateLoader.load("LabelEscapeTestFlash.html", "#{pnotify /}").render(args);
  }

  /**
   * Usa fastergt, il motore dei template delle viste, per poter usare anche i tag. Il nome
   * .html attiva la codifica automatica di ${} come nelle viste.
   */
  private String render(String template, String text) {
    Map<String, Object> args = new HashMap<>();
    args.put("text", text);
    return TemplateLoader.load("LabelEscapeTest" + template.hashCode() + ".html", template)
        .render(args);
  }
}
