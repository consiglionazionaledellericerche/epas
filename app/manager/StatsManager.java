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

package manager;

import com.google.common.collect.Sets;
import dao.InstituteDao;
import dao.OfficeDao;
import dao.PersonDao;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import lombok.val;
import models.Institute;
import models.Office;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Fornisce statistiche sui dati presenti nell'anagrafica di ePAS.
 *
 * @author Cristian Lucchesi
 *
 */
public class StatsManager {

  InstituteDao instituteDao;
  OfficeDao officeDao;
  PersonDao personDao;
  
  private List<Institute> allInstitutes = null;
  private List<Office> allOffices = null;
  
  private static final String DIPARTIMENTO_NAME_PREFIX = "Dip";
  
  private static final String CDS_HEAD_QUARTER_PREFIX = "000";
  
  /**
   * Costruttore predefinito (per l'injection).
   */
  @Inject
  public StatsManager(InstituteDao instituteDao, OfficeDao officeDao,
      PersonDao personDao) {
    this.instituteDao = instituteDao; 
    this.officeDao = officeDao;
    this.personDao = personDao;
  }
  
  /**
   * Lista di tutti gli uffici presenti in ePAS.
   */
  public List<Institute> getAllInstitutes() {
    return allInstitutes != null ? allInstitutes : instituteDao.getAllInstitutes();
  }
  
  /**
   * Lista degli istituti (escluso i dipartimenti).
   */
  public List<Institute> getInstitutes() {
    return getAllInstitutes().stream()
        .filter(i -> !i.getName().startsWith(DIPARTIMENTO_NAME_PREFIX) 
            && !i.getCds().startsWith(CDS_HEAD_QUARTER_PREFIX))
        .collect(Collectors.toList());
  }
  
  /**
   * Lista dei dipartimenti presenti.
   */
  public List<Institute> getDepartments() {
    return getAllInstitutes().stream()
        .filter(o -> o.getName().startsWith(DIPARTIMENTO_NAME_PREFIX))
        .collect(Collectors.toList());
  }
  
  /**
   * Lista di tutti gli uffici presenti.
   */
  public List<Office> getAllOffices() {
    return allOffices != null ? allOffices : officeDao.getAllOffices();
  }
  
  /**
   * Tutti gli ufficio della sede centrale.
   */
  public Set<Office> getHeadQuarterOffices() {
    return getAllOffices().stream()
    .filter(o -> o.getCode().startsWith(CDS_HEAD_QUARTER_PREFIX))
    .collect(Collectors.toSet());
  }

  /**
   * Tutti gli ufficio della rete scientifica.
   */
  public Set<Office> getInstitutesOffices() {
    return getAllOffices().stream()
    .filter(o -> !o.getCode().startsWith(CDS_HEAD_QUARTER_PREFIX))
    .collect(Collectors.toSet());
  }
  
  /**
   * Crea il file xls con i dati degli istituti, dipartimenti ed uffici 
   * presenti in ePAS.
   */
  public File createFileXlsToExport() throws IOException {
    
    // genero il file excel...
    File file = File.createTempFile(
        "statistiche_utilizzo_epas-" + LocalDate.now(), ".xls");

    Workbook wb = new HSSFWorkbook();
    
    try (FileOutputStream out = new FileOutputStream(file)) {
      buildGeneralStatsSheet(wb);
      buildInstitutesSheet(wb, "Istituti", getInstitutes());
      buildInstitutesSheet(wb, "Dipartimenti", getDepartments());
      buildOfficesSheet(wb, "Uffici sede centrale", getHeadQuarterOffices());
      buildOfficesSheet(wb, "Uffici rete scientifica", getInstitutesOffices());
      wb.write(out);
    }
    
    wb.close();
    return file;
  }
  
  private void buildGeneralStatsSheet(Workbook wb) {
    Sheet sheet = wb.createSheet("Dati generali");
    
    Row row = null;
    int rowNumber = 0;
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Istituti");
    row.createCell(1).setCellValue(getInstitutes().size());
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Dipartimenti");
    row.createCell(1).setCellValue(getDepartments().size());
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Uffici totali");
    row.createCell(1).setCellValue(getAllOffices().size());
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Uffici SAC");
    row.createCell(1).setCellValue(getHeadQuarterOffices().size());

    row = sheet.createRow(rowNumber++);
    
    val numberOfPersons = personDao.getActivePersonInMonth(
        Sets.newHashSet(), YearMonth.now()).size();

    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Numero dipendenti in anagrafica");
    row.createCell(1).setCellValue(numberOfPersons);

    val numberOfTechnicians = personDao.getActiveTechnicianInMonth(
        Sets.newHashSet(), YearMonth.now()).size();
    val numberOfTopLevels = numberOfPersons - numberOfTechnicians;
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Numero dipendenti livelli I - III");
    row.createCell(1).setCellValue(numberOfTopLevels);
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Numero dipendenti livelli IV - VIII");
    row.createCell(1).setCellValue(numberOfTechnicians);
    
    val numberOfHeadQuarterPersons = 
        personDao.getActivePersonInMonth(getHeadQuarterOffices(), YearMonth.now()).size();
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Numero dipendenti della SAC");
    row.createCell(1).setCellValue(numberOfHeadQuarterPersons);
    
    IntStream.of(0, 1).forEach(column -> sheet.autoSizeColumn(column));
    
  }
  
  private void buildInstitutesSheet(Workbook wb, String sheetName, List<Institute> institutes) {
    Sheet sheet = wb.createSheet(sheetName);
    
    Row row = null;
    int rowNumber = 0;
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Sigla/Codice");
    row.createCell(1).setCellValue("Nome");
    row.createCell(2).setCellValue("Cds");
    
    Cell cellCode = null;
    Cell cellName = null;
    Cell cellCds = null;

    for (Institute institute : institutes) {
      row = sheet.createRow(rowNumber++);
      cellCode = row.createCell(0);
      cellCode.setCellValue(institute.getCode());
      cellName = row.createCell(1);
      cellName.setCellValue(institute.getName());
      cellCds = row.createCell(2);
      cellCds.setCellValue(institute.getCds());
    }
    IntStream.of(0, 1, 2).forEach(column -> sheet.autoSizeColumn(column));
  }
  
  private void buildOfficesSheet(Workbook wb, String sheetName, Set<Office> offices) {
    Sheet sheet = wb.createSheet(sheetName);
    
    Row row = null;
    int rowNumber = 0;
    
    row = sheet.createRow(rowNumber++);
    row.createCell(0).setCellValue("Codice Sede");
    row.createCell(1).setCellValue("Nome");
    row.createCell(2).setCellValue("Sede id");
    row.createCell(3).setCellValue("Indirizzo");

    Cell cellCode = null;
    Cell cellName = null;
    Cell cellCds = null;

    for (Office office : offices) {
      row = sheet.createRow(rowNumber++);
      cellCode = row.createCell(0);
      cellCode.setCellValue(office.getCode());
      cellName = row.createCell(1);
      cellName.setCellValue(office.getName());
      cellCds = row.createCell(2);
      cellCds.setCellValue(office.getCodeId());
      cellCds = row.createCell(3);
      cellCds.setCellValue(office.getAddress());      
    }
    IntStream.of(0, 1, 2, 3).forEach(column -> sheet.autoSizeColumn(column));
  }

}