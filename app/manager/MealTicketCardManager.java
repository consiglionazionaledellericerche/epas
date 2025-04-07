/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import dao.ContractDao;
import dao.MealTicketDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
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
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import manager.charts.ChartsManager.PersonStampingDayRecapHeader;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import models.Contract;
import models.MealTicket;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import models.absences.Absence;
import models.enumerate.BlockType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.LocalDate;

/**
 * Gestore delle informazioni sui buoni pasto elettronici.
 */
@Slf4j
public class MealTicketCardManager {

  private MealTicketDao mealTicketDao;
  private ContractDao contractDao;
  private final PersonStampingRecapFactory stampingsRecapFactory;

  /**
   * Construttore predefinito per l'injection.
   */
  @Inject
  public MealTicketCardManager(MealTicketDao mealTicketDao,
      ContractDao contractDao, PersonStampingRecapFactory stampingsRecapFactory) {
    this.mealTicketDao = mealTicketDao;
    this.contractDao = contractDao;
    this.stampingsRecapFactory = stampingsRecapFactory;
  }

  /**
   * Salva la card elettronica.
   *
   * @param mealTicketCard la card da persistere
   * @param person la persona associata
   * @param office la sede della persona
   */
  public void saveMealTicketCard(MealTicketCard mealTicketCard, Person person, Office office) {

    MealTicketCard previous = person.actualMealTicketCard();
    if (previous != null) {
      log.info("Termino la validità della precedente tessera per {}", person.getFullname());
      previous.setActive(false);
      previous.setEndDate(LocalDate.now().minusDays(1));
      previous.save();
    }
    mealTicketCard.setActive(true);
    mealTicketCard.setPerson(person);
    mealTicketCard.setDeliveryOffice(office);
    mealTicketCard.setBeginDate(LocalDate.now());
    mealTicketCard.setEndDate(null);
    mealTicketCard.save();
    log.info("Aggiunta nuova tessera con identificativo {} a {}", 
        mealTicketCard.getNumber(), person.getFullname());
  }

  /**
   * Assegna i buoni pasto elettronici inseriti su epas finora alla scheda 
   * attuale assegnata al dipendente.
   *
   * @param card l'attuale scheda elettronica per i buoni pasto elettronici
   * @return true se i buoni sono stati assegnati correttamente, false altrimenti.
   */
  public boolean assignOldElectronicMealTicketsToCard(Optional<Contract> actualContract, 
      MealTicketCard card) {
   
    if (!actualContract.isPresent()) {
      return false;
    }
    List<MealTicket> electronicMealTickets = mealTicketDao
        .getUnassignedElectronicMealTickets(actualContract.get());
    for (MealTicket mealTicket : electronicMealTickets) {
      mealTicket.setMealTicketCard(card);
      mealTicket.save();
    }
    return true;    
  }

  /**
   * Persiste i buoni sulla card.
   *
   * @param card la tessera su cui salvare i buoni
   * @param deliveryDate la data di rilascio
   * @param tickets il numero di buoni da salvare
   * @param admin l'amministratore che fa l'operazione
   * @param expireDate la data di scadenza
   * @param office la sede su cui associare i buoni pasto
   */
  public void saveElectronicMealTicketBlock(MealTicketCard card, LocalDate deliveryDate, 
      Integer tickets, User admin, LocalDate expireDate, Office office) {
    String block = "" + card.getNumber() + deliveryDate.getYear() + deliveryDate.getMonthOfYear();
    for (Integer i = 1; i <= tickets; i++) {
      MealTicket mealTicket = new MealTicket();
      mealTicket.setBlock(block);
      mealTicket.setBlockType(BlockType.electronic);
      mealTicket.setContract(contractDao.getContract(deliveryDate, card.getPerson()));
      mealTicket.setMealTicketCard(card);
      mealTicket.setAdmin(admin.getPerson());
      mealTicket.setDate(deliveryDate);
      mealTicket.setExpireDate(expireDate);
      mealTicket.setOffice(office);
      mealTicket.setNumber(i);
      if (i < 10) {
        mealTicket.setCode(block + "0" + i);
      } else {
        mealTicket.setCode("" + block + i);
      }
      mealTicket.save();
    }
  }

  /**
   * Metodo che costruisce il file per esportare i buoni maturati nell'anno/mese.
   *
   * @param personList la lista delle persone per cui trovare i buoni maturati
   * @param year l'anno 
   * @param month il mese dell'esportazione
   *
   * @return il file contenente il conteggio dei buoni maturati da begindate a enddate.
   */
  public InputStream buildFile(Office office, List<Person> personList, 
      Integer year, Integer month) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(out);
    byte[] buffer = new byte[1024];

    File file = createFileXlsToExport(office, personList, year, month);
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

  private File createFileXlsToExport(Office office,
      List<Person> personList, Integer year, Integer month) throws FileNotFoundException {
    File file = null;
    String monthOfYear = DateUtility.fromIntToStringMonth(month);
    try {
      file = File.createTempFile(
          "Esportazione_" + monthOfYear + "_" + year.intValue() + "", ".xls");
    } catch (IOException e) {
      log.error("Problema durante creazione file temporenea per exportazione in excel", e);
    }

    Workbook wb = new HSSFWorkbook();
    FileOutputStream out = new FileOutputStream(file);
    Sheet sheet = wb.createSheet("CalcoloBuoni"); 

    CellStyle cs = createHeader(wb);
    Row row = null;
    Cell cell = null;
    row = sheet.createRow(0);
    row.setHeightInPoints(30);
    for (int i = 0; i < 5; i++) {
      sheet.setColumnWidth((short) i, (short) ((50 * 8) / ((double) 1 / 20)));
      cell = row.createCell(i);
      cell.setCellStyle(cs);
      switch (i) {
        case 0:
          cell.setCellValue(ExportRecapHeader.Codice_sede.getDescription());
          break;
        case 1:
          cell.setCellValue(ExportRecapHeader.Matricola.getDescription());
          break;
        case 2:
          cell.setCellValue(ExportRecapHeader.Cognome.getDescription());
          break;
        case 3:
          cell.setCellValue(ExportRecapHeader.Nome.getDescription());
          break;
        case 4:
          cell.setCellValue(ExportRecapHeader.Numero_buoni.getDescription());
          break;
        default:
          break;
      }
    }
    int rownum = 1;
    CellStyle style = wb.createCellStyle();
    style.setAlignment(CellStyle.ALIGN_CENTER);
    for (Person person : personList) {
      PersonStampingRecap psDto = stampingsRecapFactory.create(person, year,
          month, false, Optional.absent());
      row = sheet.createRow(rownum);
      for (int cellnum = 0; cellnum < 5; cellnum++) {
        cell = row.createCell(cellnum);
        cell.setCellStyle(style);
        switch (cellnum) {
          case 0:
            cell.setCellValue(office.getCodeId().toString());
            break;
          case 1:
            cell.setCellValue(person.getNumber());
            break;
          case 2:
            cell.setCellValue(person.getSurname());
            break;
          case 3:
            cell.setCellValue(person.getName());
            break;
          case 4:
            cell.setCellValue(psDto.getNumberOfMealTicketToUse());
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
    return file;
  }

  /**
   * Intestazioni per il report con i riepiloghi mensili ore e assenze.
   */
  @RequiredArgsConstructor
  public enum ExportRecapHeader {
    Codice_sede("Codice sede"), 
    Matricola("Matricola"), 
    Cognome("Cognome"), 
    Nome("Nome"), 
    Numero_buoni("N.Buoni");

    @Getter
    private final String description;

    /**
     * Lista delle label delle intestazioni.
     */
    public static List<String> getLabels() {
      return Stream.of(values()).map(v -> v.description).collect(Collectors.toList());
    }
  }

  /**
   * Genera lo stile delle celle di intestazione.
   *
   * @param wb il workbook su cui applicare lo stile
   * @return lo stile per una cella di intestazione.
   */
  private CellStyle createHeader(Workbook wb) {

    Font font = wb.createFont();
    font.setFontHeightInPoints((short) 12);
    font.setColor((short) 0xc);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    CellStyle cs = wb.createCellStyle();
    cs.setFont(font);
    cs.setBorderBottom(CellStyle.BORDER_DOUBLE);
    cs.setAlignment(CellStyle.ALIGN_CENTER);
    return cs;
  }

}