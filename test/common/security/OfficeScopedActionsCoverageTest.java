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
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import play.Play;
import play.test.UnitTest;

/**
 * Le azioni concesse da una regola *_Generic (permesso sull'azione, target == null) che
 * ricevono l'id di un oggetto devono verificare i permessi sull'oggetto o sulla sua sede con
 * rules.checkIfPermitted / rules.check: altrimenti chi ha un ruolo su una sede può usarle sui
 * dati di tutte le sedi.
 *
 * <p>Il test analizza conf/permissions.drl e i controller di app/controllers: una nuova azione
 * di questo tipo senza controllo lo fa fallire. Le eccezioni sono elencate qui sotto.</p>
 */
public class OfficeScopedActionsCoverageTest extends UnitTest {

  /**
   * Azioni accettate senza controllo nel corpo del metodo.
   */
  private static final Set<String> ALLOWED = ImmutableSet.of(
      // corrette nella PR dell'advisory GHSA-52x6 e nelle PR #607/#609 sulle stesse azioni
      "AbsenceGroups.save", "AbsenceRequests.approval", "AbsenceRequests.disapproval",
      "Absences.removeAttach", "CompetenceRequests.updateNote",
      "InformationRequests.approval", "InformationRequests.disapproval",
      "PersonMonths.modifyPeopleTrainingHours", "PersonMonths.modifyTrainingHours",
      "PersonMonths.saveTrainingHours", "Stampings.saveServiceReasons",
      // i parametri sono tipi o configurazioni comuni a tutte le sedi, oppure la verifica è
      // fatta da un metodo chiamato (es. prepareNewRequest, certifications)
      "AbsenceGroups.editAbsenceType", "AbsenceGroups.editGroup", "AbsenceGroups.showGroups",
      "AbsenceRequests.list", "AbsenceRequests.listToApprove", "Certifications.processAll",
      "CompetenceRequests.list", "CompetenceRequests.listToApprove",
      "CompetenceRequests.listToApproveWithRequestInAdvance", "Competences.edit",
      "InformationRequests.editIllnessRequest", "InformationRequests.editServiceRequest",
      "InformationRequests.list", "InformationRequests.listToApprove",
      "InformationRequests.saveIllnessRequest", "InformationRequests.saveParentalLeaveRequest",
      "InformationRequests.saveServiceRequest", "TimeSlots.blank",
      // istituti e sedi, gestiti dagli amministratori tecnici: da verificare a parte
      "Institutes.delete", "Institutes.edit", "Institutes.save", "Offices.save");

  private static final Pattern RULE = Pattern.compile(
      "\\nrule\\s+\"?([^\\n\"]+?)\"?\\s*\\n(.*?)\\nend", Pattern.DOTALL);
  private static final Pattern ACTION = Pattern.compile("\"([A-Z]\\w+\\.\\w+)\"");
  private static final Pattern CHECK = Pattern.compile("checkIfPermitted|rules\\.check");
  private static final Pattern ID_PARAM = Pattern.compile("(?:Long|long)\\s+\\w*[iI]d\\b");
  private static final Pattern ENTITY_PARAM = Pattern.compile("\\b[A-Z]\\w+\\s+\\w+");
  private static final Pattern SIMPLE_TYPES = Pattern.compile(
      "\\b(String|Integer|Long|Boolean|LocalDate|YearMonth|Optional|List|Set|Blob|File|Double"
      + "|boolean|int|long)\\b");

  @Test
  public void genericActionsOnObjectsCheckPermissions() throws IOException {
    Set<String> uncovered = new TreeSet<>();
    for (String action : genericActions()) {
      String controller = action.substring(0, action.indexOf('.'));
      File source = new File(Play.applicationPath, "app/controllers/" + controller + ".java");
      if (!source.exists()) {
        continue;
      }
      String code = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
      if (lacksCheck(code, action.substring(action.indexOf('.') + 1))) {
        uncovered.add(action);
      }
    }
    Set<String> missing = Sets.difference(uncovered, ALLOWED);
    assertTrue("Azioni concesse da regole *_Generic che ricevono l'id di un oggetto senza "
        + "rules.checkIfPermitted: " + missing, missing.isEmpty());
  }

  private Set<String> genericActions() throws IOException {
    String drl = new String(Files.readAllBytes(
        new File(Play.applicationPath, "conf/permissions.drl").toPath()), StandardCharsets.UTF_8)
        .replaceAll("(?s)/\\*.*?\\*/", "");
    Set<String> actions = new TreeSet<>();
    Matcher rule = RULE.matcher(drl);
    while (rule.find()) {
      if (rule.group(1).trim().endsWith("_Generic") && rule.group(2).contains("target == null")) {
        Matcher action = ACTION.matcher(rule.group(2));
        while (action.find()) {
          actions.add(action.group(1));
        }
      }
    }
    return actions;
  }

  /**
   * True se un metodo pubblico con quel nome riceve un id o un'entità e non contiene controlli.
   */
  private boolean lacksCheck(String code, String method) {
    Matcher header = Pattern.compile("^\\s*public static \\S+ " + method
        + "\\((.*?)\\)\\s*(?:throws[^{]*)?\\{", Pattern.MULTILINE | Pattern.DOTALL).matcher(code);
    while (header.find()) {
      String params = header.group(1);
      String body = body(code, header.end() - 1);
      if (CHECK.matcher(body).find()) {
        continue;
      }
      if (ID_PARAM.matcher(params).find()
          || ENTITY_PARAM.matcher(SIMPLE_TYPES.matcher(params).replaceAll("")).find()) {
        return true;
      }
    }
    return false;
  }

  private String body(String code, int openBrace) {
    int depth = 0;
    for (int i = openBrace; i < code.length(); i++) {
      if (code.charAt(i) == '{') {
        depth++;
      } else if (code.charAt(i) == '}' && --depth == 0) {
        return code.substring(openBrace, i + 1);
      }
    }
    return code.substring(openBrace);
  }
}
