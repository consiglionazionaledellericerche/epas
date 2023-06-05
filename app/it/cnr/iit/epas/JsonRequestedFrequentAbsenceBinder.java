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

package it.cnr.iit.epas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import models.exports.FrequentAbsenceCode;
import models.exports.PeriodAbsenceCode;
import play.data.binding.TypeBinder;

/**
 * Binder per oggetti json sulle assenze frequenti.
 *
 * @author dario
 *
 */
@Slf4j
public class JsonRequestedFrequentAbsenceBinder implements TypeBinder<FrequentAbsenceCode> {

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
                     Class actualClass, Type genericType) throws Exception {

    try {

      log.debug("Aha!");
      PeriodAbsenceCode periodAbsenceCode = new PeriodAbsenceCode();

      log.debug("Inizio parsing del json...");
      JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

      String dataInizio = jsonObject.get("dateFrom").getAsString();
      String dataFine = jsonObject.get("dateTo").getAsString();
      periodAbsenceCode.dateFrom = dataInizio;
      periodAbsenceCode.dateTo = dataFine;
      log.debug("Il periodo va da {} a {}",
          periodAbsenceCode.dateFrom, periodAbsenceCode.dateTo);
      return periodAbsenceCode;
    } catch (Exception ex) {
      log.error("Errore durante il parsing del Json...(dei codici di assenza piu' frequenti?): {}",
          ex);
      return null;
    }

  }

}
