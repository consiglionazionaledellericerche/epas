package manager;

import com.google.common.base.Joiner;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.charts.ChartsManager.PersonStampingDayRecapHeader;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Person;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.joda.time.YearMonth;


@Slf4j
public class MonthsRecapManager {

  private final PersonStampingRecapFactory stampingsRecapFactory;

  @Inject
  public MonthsRecapManager(PersonStampingRecapFactory stampingsRecapFactory) {
    this.stampingsRecapFactory = stampingsRecapFactory;
  }

  private final String covid19 = "COVID19";
  private final String covid19bp = "COVID19BP";
  
  /**
   * Genera il file da esportare contenente la situazione riepilogativa sulla sede nell'anno/mese
   * relativa a smart working.
   * @return il file contenente le info su smart working/lavoro in sede.
   * @throws IOException eccezione di input/output
   */
  public InputStream buildFile(YearMonth yearMonth, List<Person> personList) throws IOException {
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
    cell.setCellStyle(createHeader(wb));
    LocalDate endDate = beginDate.dayOfMonth().withMaximumValue();
    for (int cellnum = 1; cellnum <= endDate.getDayOfMonth(); cellnum++) {
      cell = row.createCell(cellnum);
      LocalDate date = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), cellnum);     
      if (DateUtility.isGeneralHoliday(Optional.of(new MonthDay(6, 17)), date) 
          || date.getDayOfWeek() == DateTimeConstants.SATURDAY 
          || date.getDayOfWeek() == DateTimeConstants.SUNDAY) {
        log.debug("Applico lo stile per il festivo");
//        CellStyle cs = wb.createCellStyle();
//        cs.setAlignment(CellStyle.ALIGN_CENTER);
//        cs.setFillForegroundColor(IndexedColors.RED.getIndex());
//        cell.setCellStyle(cs);
        cell.setCellStyle(createHoliday(wb));
        
      } else {
        cell.setCellStyle(createWorkingday(wb));
      }
      cell.setCellStyle(createHeader(wb));
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
    try {
      FileOutputStream out = new FileOutputStream(file);
      Cell cell = row.createCell(0);
      cell.setCellStyle(createCell(wb));
      cell.setCellValue(psDto.person.fullName());

      for (PersonStampingDayRecap day : psDto.daysRecap) {
        cell = row.createCell(day.personDay.date.getDayOfMonth());
        cell.setCellStyle(createCell(wb));
        if (!day.personDay.absences.isEmpty() 
            && (day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(covid19) 
            || day.personDay.absences.get(0).absenceType.code.equalsIgnoreCase(covid19bp))) {
          cell.setCellValue("In sede");
        } else if (!day.personDay.stampings.isEmpty()) {
          cell.setCellValue("SW");
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
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createCell(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    //font.setColor((short) 0xa);
    //font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }
  
  /**
   * Genera lo stile delle celle di intestazione.
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createHeader(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    //font.setColor((short) 0xa);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }
  

  /**
   * Genera lo stile per una cella di giorno di vacanza.
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella che identifica un giorno di vacanza.
   */
  private final CellStyle createHoliday(Workbook wb) {
    CellStyle cs = wb.createCellStyle();
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    cs.setFillForegroundColor(IndexedColors.RED.getIndex());
    cs.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
    cs.setFillPattern(CellStyle.SOLID_FOREGROUND);

    return cs;
  }

  /**
   * Genera lo stile per una cella di un giorno lavorativo.
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella che identifica un giorno lavorativo.
   */
  private final CellStyle createWorkingday(Workbook wb) {
    CellStyle cs = wb.createCellStyle();
    Font font = wb.createFont();
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    cs.setFont(font);
    return cs;
  }
}
