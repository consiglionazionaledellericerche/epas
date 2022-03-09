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

package controllers;

import com.google.common.base.Optional;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.UploadSituationManager;
import models.Office;
import org.apache.commons.io.IOUtils;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Contiene i metodi necessari per estrarre i dati per la generazione delle buste paga.
 * Lasciato per eventuli utilizzi futuri (vedi INAF).
 *
 * @author Cristian Lucchesi
 */
@Slf4j
@With({Resecure.class})
public class UploadSituation extends Controller {

  public static final String FILE_PREFIX = "situazioneMensile";
  public static final String FILE_SUFFIX = ".txt";

  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static IWrapperFactory factory;
  @Inject
  private static UploadSituationManager updloadSituationManager;

  /**
   * Tab creazione file.
   *
   * @param officeId sede
   * @param year     anno
   * @param month    mese
   */
  public static void createFile(Long officeId, Integer year, Integer month) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    //TODO: costruire la lista degli anni sulla base di tutti gli uffici permessi 
    //e usarla nel template.

    IWrapperOffice wrOffice = factory.create(office);
    Optional<YearMonth> monthToUpload = wrOffice.nextYearMonthToUpload();
    render(wrOffice, monthToUpload);
  }

  /**
   * Tab creazione file.
   *
   * @param office sede
   * @param year   anno
   * @param month  mese
   * @throws IOException sollevata in caso non sia possibile chiudere il file.
   */
  public static void computeCreateFile(@Valid Office office,
      @Required Integer year, @Required Integer month) throws IOException {

    notFoundIfNull(office);
    rules.checkIfPermitted(office);

    IWrapperOffice wrOffice = factory.create(office);
    if (!Validation.hasErrors()) {
      //controllo che il mese sia uploadable
      if (!wrOffice.isYearMonthUploadable(new YearMonth(year, month))) {
        Validation.addError("year", "non può essere precedente al primo mese riepilogabile");
        Validation.addError("month", "non può essere precedente al primo mese riepilogabile");
      }
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      //flash.error(Web.msgHasErrors());
      log.warn("validation errors: {}", validation.errorsMap());
      Optional<YearMonth> monthToUpload = Optional.of(new YearMonth(year, month));
      render("@createFile", wrOffice, monthToUpload);
    }

    String body = updloadSituationManager.createFile(office, year, month);
    String fileName = FILE_PREFIX + office.codeId + " - " + year + month + FILE_SUFFIX;
    try (InputStream inputStream = IOUtils.toInputStream(body, Charset.defaultCharset())) {
      renderBinary(inputStream, fileName);
    }
  }

}
