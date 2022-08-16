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

import com.google.common.base.Optional;
import it.cnr.iit.epas.DateUtility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Office;
import models.Person;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.joda.time.YearMonth;


/**
 * Manager per la gestione dei riepiloghi mensili.
 */
@Slf4j
public class MonthsRecapManager {

  private final PersonStampingRecapFactory stampingsRecapFactory;
  private final ConfigurationManager configurationManager;

  @Inject
  public MonthsRecapManager(PersonStampingRecapFactory stampingsRecapFactory,
      ConfigurationManager configurationManager) {
    this.stampingsRecapFactory = stampingsRecapFactory;
    this.configurationManager = configurationManager;
  }

  private final String covid19 = "COVID19";
  private final String covid19bp = "COVID19BP";
  private final String lagile = "LAGILE";
  private final String lagilebp = "LAGILEBP";
  
  /**
   * Genera il file da esportare contenente la situazione riepilogativa sulla sede nell'anno/mese
   * relativa a smart working.
   *
   * @return il file contenente le info su smart working/lavoro in sede.
   * @throws IOException eccezione di input/output
   */
  public InputStream buildFile(YearMonth yearMonth, List<Person> personList, Office office) 
      throws IOException {
    LocalDate beginDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];
    // genero il file excel...
    File file = File.createTempFile(
        "riepilogo-" + DateUtility.fromIntToStringMonth(beginDate.getMonthOfYear()) + "-"
        + beginDate.getYear(), ".xls");

    Workbook wb = new HSSFWorkbook();
    String sheetname = "RIEPILOGO " 
        + DateUtility.fromIntToStringMonth(yearMonth.getMonthOfYear()) + yearMonth.getYear();

    Sheet sheet = wb.createSheet(sheetname);
    Row row = sheet.createRow(0);
    sheet.setColumnWidth(0, 30 * 256);
    row.setHeightInPoints(30);
    Cell cell = row.createCell(0);
    cell.setCellValue("DIPENDENTE");
    cell.setCellStyle(createHeader(wb, Optional.absent(), office));
    LocalDate endDate = beginDate.dayOfMonth().withMaximumValue();
    for (int cellnum = 1; cellnum <= endDate.getDayOfMonth(); cellnum++) {
      cell = row.createCell(cellnum);
      LocalDate date = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), cellnum);     

      cell.setCellStyle(createHeader(wb, Optional.of(date), office));
      cell.setCellValue(date.dayOfWeek().getAsShortText() + "\n" + date.dayOfMonth().getAsText()); 
      
      
    }
    int rownum = 1;
    // scorro la lista delle persone per cui devo fare l'esportazione...
    for (Person person : personList) {
      //log.debug("Mi occupo di: {}", person.fullName());
      row = sheet.createRow(rownum);
      row.setHeightInPoints(30);
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, beginDate.getYear(),
          beginDate.getMonthOfYear(), false);
      // aggiorno il file aggiungendo un nuovo foglio per ogni persona...
      file = createFileXlsToExport(psDto, file, wb, row);

      rownum++;
    }
    // faccio lo stream da inviare al chiamante...
    FileInputStream in = new FileInputStream(file);
    try {
      zos.putNextEntry(new ZipEntry(file.getName()));
      int length;
      while ((length = in.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    in.close();
    file.delete();
    zos.closeEntry();
    zos.close();

    return new ByteArrayInputStream(out.toByteArray());
  }

  private File createFileXlsToExport(PersonStampingRecap psDto, File file, Workbook wb, Row row)
      throws IOException {
    
    CellStyle holiday = wb.createCellStyle();
    holiday.setAlignment(CellStyle.ALIGN_CENTER);
    holiday.setBorderLeft(CellStyle.VERTICAL_BOTTOM);
    holiday.setBorderRight(CellStyle.VERTICAL_BOTTOM);
    holiday.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    holiday.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    holiday.setFillPattern(CellStyle.SOLID_FOREGROUND);
    
    CellStyle workingDay = wb.createCellStyle();    
    workingDay.setAlignment(CellStyle.ALIGN_CENTER);

    
    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    try {
      FileOutputStream out = new FileOutputStream(file);
      Cell cell = row.createCell(0);
      cell.setCellStyle(createCell(wb));
      cell.setCellValue(psDto.person.fullName());

      for (PersonStampingDayRecap day : psDto.daysRecap) {
        cell = row.createCell(day.personDay.date.getDayOfMonth());
        cell.setCellStyle(cs);
        if (day.personDay.isHoliday) {
          cell.setCellStyle(holiday);
        } else {
          cell.setCellStyle(workingDay);
        }
        if (!day.personDay.absences.isEmpty() 
            && (day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(covid19) 
            || day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(covid19bp)
            || day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(lagile)
            || day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(lagilebp)
            )) {
          cell.setCellValue("SW");
        } else if (!day.personDay.stampings.isEmpty()) {
          cell.setCellValue("In sede");
        } else {
          cell.setCellValue("-");
        }        
      }

      try {
        wb.write(out);
        wb.close();
        out.close();
      } catch (IOException ex) {
        log.error("problema in chiusura stream");
        ex.printStackTrace();
      }
    } catch (IllegalArgumentException | FileNotFoundException ex) {
      log.error("Problema in riconoscimento file");
      ex.printStackTrace();
    }
    return file;
  }


  /**
   * Genera lo stile delle celle di intestazione.
   *
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createCell(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }

  /**
   * Genera lo stile delle celle di intestazione.
   *
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createHeader(Workbook wb, Optional<LocalDate> date, Office office) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    cs.setBorderLeft(CellStyle.VERTICAL_BOTTOM);
    cs.setBorderRight(CellStyle.VERTICAL_BOTTOM); 
    if (date.isPresent()) {
      MonthDay patron = (MonthDay) configurationManager
          .configValue(office, EpasParam.DAY_OF_PATRON, date.get());
      if (DateUtility.isGeneralHoliday(Optional.of(patron), date.get()) 
          || date.get().getDayOfWeek() == DateTimeConstants.SATURDAY 
          || date.get().getDayOfWeek() == DateTimeConstants.SUNDAY) {
        log.debug("Applico lo stile per il festivo");        
        cs.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        cs.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        cs.setFillPattern(CellStyle.SOLID_FOREGROUND);        
      } 
    }
    
    return cs;
  }

}