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

import java.text.Normalizer;
import models.Person;
import models.absences.Absence;
import org.joda.time.LocalDate;

/**
 * Nomi dei file degli allegati delle assenze. Il file è salvato con un UUID come nome, il nome
 * leggibile viene costruito al momento dello scaricamento (singolo o nello zip).
 */
public final class AttachmentFileNames {

  private AttachmentFileNames() {
  }

  /**
   * Il nome del file dell'allegato di un'assenza: anno-mese-giorno-codice-cognome-nome seguito
   * dall'estensione dedotta dal tipo del file (es. 2026-11-18-111-Rossi-Mario.pdf).
   *
   * @param absence l'assenza con allegato
   * @return il nome del file
   */
  public static String forAbsence(Absence absence) {
    LocalDate date = absence.getAbsenceDate();
    Person person = absence.getPersonDay().getPerson();
    String name = String.format("%s-%s-%s-%s", date.toString("yyyy-MM-dd"),
        sanitize(absence.getAbsenceType().getCode()), sanitize(person.getSurname()),
        sanitize(person.getName()));
    if (absence.getAbsenceFile() != null) {
      String extension = ImageUtils.fileExtension(absence.getAbsenceFile()).orElse("");
      name = name + extension;
    }
    return name;
  }

  /**
   * Rende il testo utilizzabile in un nome di file: toglie gli accenti e sostituisce con un
   * trattino i caratteri diversi da lettere, cifre, punto e trattino basso.
   */
  static String sanitize(String text) {
    if (text == null) {
      return "";
    }
    String plain = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return plain.replaceAll("[^A-Za-z0-9._]+", "-").replaceAll("^-+|-+$", "");
  }
}
