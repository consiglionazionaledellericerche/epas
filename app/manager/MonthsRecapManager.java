package manager;

import com.google.common.base.Joiner;
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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import it.cnr.iit.epas.DateUtility;
import manager.charts.ChartsManager.PersonStampingDayRecapHeader;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Person;
import models.absences.Absence;

public class MonthsRecapManager {
  
  private final PersonStampingRecapFactory stampingsRecapFactory;
  
  @Inject
  public MonthsRecapManager(PersonStampingRecapFactory stampingsRecapFactory) {
    this.stampingsRecapFactory = stampingsRecapFactory;
  }

  /**
   * 
   * @return
   */
  public InputStream buildFile(YearMonth yearMonth, List<Person> personList) {
    LocalDate beginDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
    LocalDate endDate = beginDate.dayOfMonth().withMaximumValue();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];
    // genero il file excel...
    File file = File.createTempFile(
        "situazioneMensile" + DateUtility.fromIntToStringMonth(beginDate.getMonthOfYear())
        + beginDate.getYear() + "A"
        + DateUtility.fromIntToStringMonth(endDate.getMonthOfYear()) + endDate.getYear(),
        ".xls");

    Workbook wb = new HSSFWorkbook();
    // scorro la lista delle persone per cui devo fare l'esportazione...
    for (Person person : personList) {
      LocalDate tempDate = beginDate;
      while (!tempDate.isAfter(endDate)) {
        PersonStampingRecap psDto = stampingsRecapFactory.create(person, tempDate.getYear(),
            tempDate.getMonthOfYear(), false);
        // aggiorno il file aggiungendo un nuovo foglio per ogni persona...
        file = createFileXlsToExport(psDto, file, wb, onlyMission);
        tempDate = tempDate.plusMonths(1);
      }
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

  private File createFileXlsToExport(PersonStampingRecap psDto, File file, Workbook wb)
      throws IOException {
    try {
      FileOutputStream out = new FileOutputStream(file);
      Sheet sheet = null;
      String fullname = "";
      if (psDto.person.name.contains(" ")) {
        String[] names = psDto.person.name.split(" ");
        fullname = psDto.person.surname + " " + names[0];
      } else {
        fullname = psDto.person.fullName();
      }
      String sheetname = fullname + "_"
          + DateUtility.fromIntToStringMonth(psDto.month) + psDto.year;
      
      if (sheetname != null && sheetname.length() > 31) {
        sheetname = sheetname.substring(0, 31);
      }
      sheet = wb.createSheet(sheetname); 

      CellStyle cs = createHeader(wb);
      Row row = null;
      Cell cell = null;

      row = sheet.createRow(0);
      row.setHeightInPoints(30);
      for (int i = 0; i < 7; i++) {
        sheet.setColumnWidth((short) i, (short) ((50 * 8) / ((double) 1 / 20)));
        cell = row.createCell(i);
        cell.setCellStyle(cs);
        switch (i) {
          case 0:
            cell.setCellValue(PersonStampingDayRecapHeader.Data.getDescription());
            break;
          case 1:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_da_timbrature.getDescription());
            break;
          case 2:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_fuori_sede.getDescription());
            break;
          case 3:
            cell.setCellValue(PersonStampingDayRecapHeader.Lavoro_effettivo.getDescription());
            break;
          case 4:
            cell.setCellValue(
                PersonStampingDayRecapHeader.Ore_giustificate_da_assenza.getDescription());
            break;
          case 5:
            cell.setCellValue(PersonStampingDayRecapHeader.Codici_di_assenza_che_giustificano_ore
                .getDescription());
            break;
          case 6:
            cell.setCellValue(PersonStampingDayRecapHeader.Codici_di_assenza
                .getDescription());            
            break;
          default:
            break;
        }
      }
      int rownum = 1;
      CellStyle cellHoliday = createHoliday(wb);
      CellStyle cellWorkingDay = createWorkingday(wb);
      for (PersonStampingDayRecap day : psDto.daysRecap) {
        row = sheet.createRow(rownum);

        for (int cellnum = 0; cellnum < 7; cellnum++) {
          cell = row.createCell(cellnum);
          if (day.personDay.isHoliday) {
            cell.setCellStyle(cellHoliday);
          } else {
            cell.setCellStyle(cellWorkingDay);
          }
          switch (cellnum) {
            case 0:
              cell.setCellValue(day.personDay.date.toString());
              break;
            case 1:
              cell.setCellValue(
                  DateUtility.fromMinuteToHourMinute(day.personDay.getStampingsTime()));
              break;
            case 2:
              cell.setCellValue(
                  DateUtility.fromMinuteToHourMinute(getOutOfOfficeTime(day.personDay)));
              break;
            case 3:
              cell.setCellValue(DateUtility.fromMinuteToHourMinute(day.personDay.getTimeAtWork()));
              break;
            case 4:
              if (!day.personDay.absences.isEmpty()) {
                String code = "";
                int justifiedTime = 0;
                for (Absence abs : day.personDay.absences) {
                  code = code + " " + abs.absenceType.code;
                  justifiedTime += abs.justifiedTime();
                }
                cell.setCellValue(DateUtility.fromMinuteToHourMinute(justifiedTime));

                if (onlyMission && code != null && code.trim().equals("92")) {
                  cell = row.getCell(3);
                  cell.setCellValue(DateUtility.fromMinuteToHourMinute(day.wttd.get().workingTime));
                }
              } else {
                cell.setCellValue("00:00");
              }
              break;
            case 5:
              cell.setCellValue(Joiner.on(";")
                  .join(day.personDay.absences.stream().filter(a -> a.justifiedTime() > 0)
                      .map(a -> a.absenceType.code).collect(Collectors.toList())));
              break;
            case 6:
              cell.setCellValue(Joiner.on(";")
                  .join(day.personDay.absences.stream()
                      .map(a -> a.absenceType.code).collect(Collectors.toList())));              
              break;
            default:
              break;
          }
        }
        rownum++;
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
}
