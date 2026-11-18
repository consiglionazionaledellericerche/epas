/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

package persons;

import com.google.common.collect.Lists;
import java.util.List;
import models.Office;
import models.Person;
import models.PersonOffice;
import org.joda.time.LocalDate;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Test per verificare l'integrità della relazione molti-a-molti tra Person e Office
 * storicizzata tramite la tabella PersonOffice.
 *
 * @author Dario Tagliaferri
 */
public class PersonOfficeTest extends UnitTest {

  /**
   * Crea un Office di test con il nome dato.
   */
  private Office createOffice(String name) {
    Office office = new Office();
    office.setName(name);
    office.setCodeId("TEST_" + name);
    return office;
  }

  /**
   * Crea una PersonOffice con le date date.
   */
  private PersonOffice createPersonOffice(Person person, Office office,
      LocalDate beginDate, LocalDate endDate) {
    PersonOffice po = new PersonOffice();
    po.setPerson(person);
    po.setOffice(office);
    po.setBeginDate(beginDate);
    po.setEndDate(endDate);
    return po;
  }

  /**
   * Crea una PersonOffice senza data di fine (corrente).
   */
  private PersonOffice createPersonOffice(Person person, Office office, LocalDate beginDate) {
    return createPersonOffice(person, office, beginDate, null);
  }

  /**
   * Costruisce una persona con la lista di afferenze specificata.
   */
  private Person buildPersonWithOffices(List<PersonOffice> personOffices) {
    Person person = new Person();
    person.setName("Mario");
    person.setSurname("Rossi");
    person.setEmail("mario.rossi@test.it");
    person.getPersonOffices().addAll(personOffices);
    return person;
  }

  /**
   * Verifica che getOffice() ritorni l'ufficio corrente (afferenza attiva oggi).
   */
  @Test
  public void testGetOfficeReturnsCurrentOffice() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");

    LocalDate today = LocalDate.now();
    LocalDate twoYearsAgo = today.minusYears(2);
    LocalDate oneYearAgo = today.minusYears(1);

    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, twoYearsAgo, oneYearAgo.minusDays(1)),
        createPersonOffice(null, office2, oneYearAgo, null)
    );

    Person person = buildPersonWithOffices(afferenze);

    assertNotNull("L'ufficio corrente non deve essere null", person.getOffice());
    assertEquals("Deve ritornare la sede attuale", office2, person.getOffice());
  }

  /**
   * Verifica che getOffice() ripieghi sull'ultima afferenza iniziata quando oggi non esiste
   * nessuna afferenza attiva.
   */
  @Test
  public void testGetOfficeFallsBackToLastOfficeWhenNoActiveOffice() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");

    LocalDate today = LocalDate.now();

    // Afferenze già terminate
    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, today.minusYears(3), today.minusYears(2)),
        createPersonOffice(null, office2, today.minusYears(2).plusDays(1), today.minusYears(1))
    );

    Person person = buildPersonWithOffices(afferenze);

    assertEquals("Senza afferenze attive deve essere restituita l'ultima afferenza iniziata",
        office2, person.getOffice());
    assertNull("Alla data odierna la persona non afferisce a nessuna sede",
        person.getOfficeAt(today));
  }

  /**
   * Verifica che getOffice() restituisca la prima afferenza futura se non ci sono afferenze
   * iniziate.
   */
  @Test
  public void testGetOfficeReturnsFutureOfficeWhenOnlyFutureOffices() {
    Office office1 = createOffice("Sede di Pisa");
    LocalDate today = LocalDate.now();
    Person person = buildPersonWithOffices(Lists.newArrayList(
        createPersonOffice(null, office1, today.plusMonths(1))));
    assertEquals(office1, person.getOffice());
  }

  /**
   * Verifica le afferenze nel mese di un trasferimento: la persona compare in entrambe le sedi.
   */
  @Test
  public void testOfficesInPeriodDuringTransferMonth() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");
    Person person = buildPersonWithOffices(Lists.newArrayList(
        createPersonOffice(null, office1, new LocalDate(2026, 1, 1), new LocalDate(2026, 3, 14)),
        createPersonOffice(null, office2, new LocalDate(2026, 3, 15))));

    LocalDate begin = new LocalDate(2026, 3, 1);
    LocalDate end = new LocalDate(2026, 3, 31);
    assertEquals(Lists.newArrayList(office1, office2), person.getOfficesInPeriod(begin, end));
    assertTrue(person.isInOfficeInPeriod(office1, begin, end));
    assertFalse(person.isInOfficeInPeriod(office1, new LocalDate(2026, 4, 1),
        new LocalDate(2026, 4, 30)));
    assertEquals(office1, person.getOfficeAt(new LocalDate(2026, 3, 14)));
    assertEquals(office2, person.getOfficeAt(new LocalDate(2026, 3, 15)));
  }

  /**
   * Verifica che changeOffice chiuda l'afferenza in corso il giorno precedente.
   */
  @Test
  public void testChangeOfficeClosesCurrentOffice() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");
    Person person = buildPersonWithOffices(Lists.newArrayList(
        createPersonOffice(null, office1, new LocalDate(2026, 1, 1))));
    person.getPersonOffices().forEach(po -> po.setPerson(person));

    person.changeOffice(office2, new LocalDate(2026, 3, 15));

    assertEquals(2, person.getPersonOffices().size());
    assertEquals(new LocalDate(2026, 3, 14), person.getPersonOffices().get(0).getEndDate());
    assertEquals(office2, person.getOfficeAt(new LocalDate(2026, 3, 15)));
  }

  /**
   * Verifica che getOffice() ritorni null quando la lista di afferenze è vuota.
   */
  @Test
  public void testGetOfficeReturnsNullWhenNoOffices() {
    Person person = buildPersonWithOffices(Lists.newArrayList());

    assertNull("L'ufficio deve essere null per una persona senza afferenze", person.getOffice());
  }

  /**
   * Verifica che getOfficeAt(date) ritorni l'ufficio corretto alla data indicata.
   */
  @Test
  public void testGetOfficeAtReturnsCorrectOfficeForDate() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");

    LocalDate today = LocalDate.now();
    LocalDate twoYearsAgo = today.minusYears(2);
    LocalDate oneYearAgo = today.minusYears(1);

    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, twoYearsAgo, oneYearAgo.minusDays(1)),
        createPersonOffice(null, office2, oneYearAgo, null)
    );

    Person person = buildPersonWithOffices(afferenze);

    // Verifica afferenza durante il primo periodo
    LocalDate dataNelPrimoPeriodo = twoYearsAgo.plusMonths(6);
    assertEquals("Alla data del primo periodo deve ritornare il primo ufficio",
        office1, person.getOfficeAt(dataNelPrimoPeriodo));

    // Verifica afferenza durante il secondo periodo
    LocalDate dataNelSecondoPeriodo = oneYearAgo.plusMonths(3);
    assertEquals("Alla data del secondo periodo deve ritornare il secondo ufficio",
        office2, person.getOfficeAt(dataNelSecondoPeriodo));
  }

  /**
   * Verifica che getOfficeAt(date) ritorni null per una data fuori da tutti i periodi.
   */
  @Test
  public void testGetOfficeAtReturnsNullForDateOutsideAllPeriods() {
    Office office1 = createOffice("Sede di Pisa");

    LocalDate today = LocalDate.now();
    LocalDate twoYearsAgo = today.minusYears(2);
    LocalDate oneYearAgo = today.minusYears(1);

    // Afferenza già terminata
    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, twoYearsAgo, oneYearAgo)
    );

    Person person = buildPersonWithOffices(afferenze);

    // Data successiva alla fine dell'afferenza
    assertNull("Deve ritornare null per una data dopo la fine dell'afferenza",
        person.getOfficeAt(today));

    // Data precedente all'inizio dell'afferenza
    LocalDate primaDelPeriodo = twoYearsAgo.minusDays(1);
    assertNull("Deve ritornare null per una data prima dell'inizio dell'afferenza",
        person.getOfficeAt(primaDelPeriodo));
  }

  /**
   * Verifica il metodo isActive() di PersonOffice.
   */
  @Test
  public void testPersonOfficeIsActive() {
    Office office = createOffice("Sede test");
    LocalDate today = LocalDate.now();

    PersonOffice attiva = createPersonOffice(null, office, today.minusMonths(6), null);
    assertTrue("L'afferenza senza data di fine e con inizio passato deve essere attiva",
        attiva.isActive());

    PersonOffice terminata = createPersonOffice(null, office, today.minusYears(2), today.minusDays(1));
    assertFalse("L'afferenza con data di fine passata non deve essere attiva",
        terminata.isActive());

    PersonOffice futura = createPersonOffice(null, office, today.plusDays(1), null);
    assertFalse("L'afferenza con data di inizio futura non deve essere attiva",
        futura.isActive());
  }

  /**
   * Verifica la gestione di afferenze multiple nello stesso periodo (deve restituire la prima).
   */
  @Test
  public void testGetOfficeWithMultipleActiveOffices() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");

    LocalDate today = LocalDate.now();
    LocalDate unAnnoFa = today.minusYears(1);

    // Caso limite: due afferenze attive contemporaneamente (dati non coerenti ma gestiti)
    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, unAnnoFa, null),
        createPersonOffice(null, office2, unAnnoFa, null)
    );

    Person person = buildPersonWithOffices(afferenze);

    // Deve restituire una delle due (la prima trovata)
    assertNotNull("Deve restituire almeno un ufficio", person.getOffice());
  }

  /**
   * Verifica che getOffice() e getOfficeAt(today) restituiscano lo stesso risultato.
   */
  @Test
  public void testGetOfficeConsistentWithGetOfficeAtToday() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");

    LocalDate today = LocalDate.now();
    LocalDate twoYearsAgo = today.minusYears(2);
    LocalDate oneYearAgo = today.minusYears(1);

    List<PersonOffice> afferenze = Lists.newArrayList(
        createPersonOffice(null, office1, twoYearsAgo, oneYearAgo.minusDays(1)),
        createPersonOffice(null, office2, oneYearAgo, null)
    );

    Person person = buildPersonWithOffices(afferenze);

    assertEquals("getOffice() e getOfficeAt(today) devono essere coerenti",
        person.getOffice(), person.getOfficeAt(today));
  }

  /**
   * Verifica il metodo contains() di PersonOffice con afferenza a tempo indeterminato.
   */
  @Test
  public void testPersonOfficeContainsWithOpenEnd() {
    Office office = createOffice("Sede test");
    LocalDate today = LocalDate.now();
    LocalDate beginDate = today.minusYears(1);

    PersonOffice po = createPersonOffice(null, office, beginDate, null);

    assertTrue("L'afferenza aperta deve contenere oggi", po.contains(today));
    assertTrue("L'afferenza aperta deve contenere date future",
        po.contains(today.plusYears(10)));
    assertTrue("L'afferenza aperta deve contenere la data di inizio", po.contains(beginDate));
    assertFalse("L'afferenza aperta non deve contenere date precedenti all'inizio",
        po.contains(beginDate.minusDays(1)));
  }

  /**
   * Verifica il metodo contains() di PersonOffice con afferenza con data di fine.
   */
  @Test
  public void testPersonOfficeContainsWithClosedEnd() {
    Office office = createOffice("Sede test");
    LocalDate today = LocalDate.now();
    LocalDate beginDate = today.minusYears(2);
    LocalDate endDate = today.minusYears(1);

    PersonOffice po = createPersonOffice(null, office, beginDate, endDate);

    assertTrue("L'afferenza deve contenere la data di inizio", po.contains(beginDate));
    assertTrue("L'afferenza deve contenere la data di fine", po.contains(endDate));
    assertTrue("L'afferenza deve contenere date intermedie",
        po.contains(beginDate.plusMonths(6)));
    assertFalse("L'afferenza non deve contenere date precedenti all'inizio",
        po.contains(beginDate.minusDays(1)));
    assertFalse("L'afferenza non deve contenere date successive alla fine",
        po.contains(endDate.plusDays(1)));
  }

  /**
   * Verifica i giorni di afferenza di ciascuna sede nel mese del trasferimento e la sede
   * considerata dalle regole dei permessi alla data del giorno.
   */
  @Test
  public void testDaysAndPermissionOfficeDuringTransferMonth() {
    Office office1 = createOffice("Sede di Pisa");
    Office office2 = createOffice("Sede di Roma");
    Person person = buildPersonWithOffices(Lists.newArrayList(
        createPersonOffice(null, office1, new LocalDate(2026, 1, 1), new LocalDate(2026, 3, 13)),
        createPersonOffice(null, office2, new LocalDate(2026, 3, 14))));
    LocalDate begin = new LocalDate(2026, 3, 1);
    LocalDate end = new LocalDate(2026, 3, 31);

    assertEquals(13, person.getDaysInOffice(office1, begin, end).size());
    assertEquals(18, person.getDaysInOffice(office2, begin, end).size());
    assertTrue(person.getDaysInOffice(office1, begin, end).contains(new LocalDate(2026, 3, 13)));
    assertFalse(person.getDaysInOffice(office1, begin, end).contains(new LocalDate(2026, 3, 14)));

    // ogni sede gestisce i giorni di propria afferenza
    assertTrue(office1.isOfficeOf(person, new LocalDate(2026, 3, 5)));
    assertFalse(office2.isOfficeOf(person, new LocalDate(2026, 3, 5)));
    assertTrue(office2.isOfficeOf(person, new LocalDate(2026, 3, 20)));
    // senza data vale la sede corrente
    assertTrue(office2.isOfficeOf(person, null));
    assertFalse(office1.isOfficeOf(person, null));

    assertEquals(office1, person.getOfficeAtOrCurrent(new LocalDate(2026, 2, 1)));
    assertEquals(office2, person.getOfficeAtOrCurrent(new LocalDate(2025, 1, 1)));
  }
}
