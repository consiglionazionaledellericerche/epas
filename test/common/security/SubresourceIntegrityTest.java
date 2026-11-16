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

package common.security;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import play.Play;
import play.test.UnitTest;

/**
 * Gli script e i fogli di stile caricati da un CDN devono avere gli attributi integrity
 * (Subresource Integrity) e crossorigin: il browser esegue il file solo se corrisponde
 * all'hash, quindi un file modificato sul CDN non viene caricato.
 *
 * <p>Per aggiungere una libreria, l'hash si calcola con:
 * {@code curl -s URL | openssl dgst -sha512 -binary | openssl base64 -A}
 * e si scrive come {@code integrity="sha512-..." crossorigin="anonymous"}.</p>
 */
public class SubresourceIntegrityTest extends UnitTest {

  /**
   * Host i cui file non hanno un contenuto fisso, quindi non possono avere un hash: il
   * loader di Google Charts non è versionato e il css di Google Fonts dipende dal browser.
   */
  private static final Set<String> HOSTS_WITHOUT_FIXED_CONTENT =
      ImmutableSet.of("www.gstatic.com", "fonts.googleapis.com");

  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern TAG = Pattern.compile(
      "<(script|link)\\b[^>]*?\\b(?:src|href)=\"((?:https?:)?//([^/\"]+)[^\"]*)\"[^>]*>",
      Pattern.DOTALL);

  @Test
  public void externalScriptsAndStylesheetsHaveIntegrity() throws IOException {
    final List<Path> views;
    try (Stream<Path> files = Files.walk(Play.getFile("app/views").toPath())) {
      views = files.filter(path -> path.toString().endsWith(".html"))
          .collect(Collectors.toList());
    }
    assertFalse("nessuna view trovata", views.isEmpty());

    final List<String> problems = new ArrayList<>();
    int checked = 0;
    for (Path view : views) {
      final String content = COMMENT.matcher(
          new String(Files.readAllBytes(view), StandardCharsets.UTF_8)).replaceAll("");
      final Matcher matcher = TAG.matcher(content);
      while (matcher.find()) {
        final String tag = matcher.group();
        if ("link".equals(matcher.group(1)) && !tag.contains("stylesheet")) {
          continue;
        }
        if (HOSTS_WITHOUT_FIXED_CONTENT.contains(matcher.group(3))) {
          continue;
        }
        checked++;
        final String url = matcher.group(2);
        if (!url.startsWith("https://")) {
          problems.add(view + ": " + url + " non è caricato in https");
        }
        if (!tag.matches("(?s).*\\bintegrity=\"sha(384|512)-[A-Za-z0-9+/=]+\".*")) {
          problems.add(view + ": " + url + " senza attributo integrity");
        }
        if (!tag.contains("crossorigin=\"anonymous\"")) {
          problems.add(view + ": " + url + " senza crossorigin=\"anonymous\"");
        }
      }
    }
    assertTrue("nessuno script o foglio di stile esterno trovato", checked > 0);
    assertTrue(String.join("\n", problems), problems.isEmpty());
  }
}
