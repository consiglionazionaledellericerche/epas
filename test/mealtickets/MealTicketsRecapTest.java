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

package mealtickets;

import com.google.common.collect.Lists;
import it.cnr.iit.epas.DateInterval;
import java.util.List;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketRecapBuilder;
import models.Contract;
import models.MealTicket;
import models.Person;
import models.PersonDay;
import models.enumerate.BlockType;
import org.assertj.core.api.Assertions;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import play.test.UnitTest;

/**
 * Test sul calcolo del riepilogo dei buoni pasto di un contratto.
 */
public class MealTicketsRecapTest extends UnitTest {

  private static final LocalDate BEGIN = new LocalDate(2026, 2, 1);
  private static final LocalDate EXPIRE = new LocalDate(2026, 12, 31);

  private final MealTicketRecapBuilder builder = new MealTicketRecapBuilder();

  private Person person;
  private Contract contract;

  /**
   * Contratto di base senza inizializzazione dei buoni pasto.
   */
  @Before
  public void setUp() {
    person = new Person();
    contract = new Contract();
    contract.setPerson(person);
    contract.setBeginDate(BEGIN);
  }

  /**
   * I buoni consegnati vengono assegnati ai giorni partendo da quello con scadenza più imminente.
   */
  @Test
  public void mealTicketRecapTest() {
    MealTicket expiringLater = mealTicket("A", 1, BEGIN, EXPIRE);
    MealTicket expiringFirst = mealTicket("B", 1, BEGIN, new LocalDate(2026, 6, 30));
    MealTicket expiringSecond = mealTicket("B", 2, BEGIN, new LocalDate(2026, 6, 30));
    List<MealTicket> expireOrderedAsc =
        Lists.newArrayList(expiringFirst, expiringSecond, expiringLater);
    List<PersonDay> personDays = personDays(2);

    MealTicketRecap recap = build(personDays, expireOrderedAsc);

    Assertions.assertThat(recap.getRemaining()).isEqualTo(1);
    Assertions.assertThat(recap.getSourcedInInterval()).isZero();
    Assertions.assertThat(recap.getDateRunOut()).isNull();
    Assertions.assertThat(recap.getDateExpire()).isNull();
    Assertions.assertThat(personDays.get(0).getMealTicketAssigned()).isSameAs(expiringFirst);
    Assertions.assertThat(personDays.get(1).getMealTicketAssigned()).isSameAs(expiringSecond);
    Assertions.assertThat(expiringFirst.used).isTrue();
    Assertions.assertThat(expiringSecond.used).isTrue();
    Assertions.assertThat(expiringLater.used).isFalse();
  }

  /**
   * Senza giorni con diritto al buono nessun buono risulta consumato.
   */
  @Test
  public void noPersonDaysNoTicketUsed() {
    List<MealTicket> mealTickets = Lists.newArrayList(
        mealTicket("A", 1, BEGIN, EXPIRE), mealTicket("A", 2, BEGIN, EXPIRE));

    MealTicketRecap recap = build(Lists.newArrayList(), mealTickets);

    Assertions.assertThat(recap.getRemaining()).isEqualTo(2);
    Assertions.assertThat(recap.getNegativeResidual()).isEqualTo(-2);
    Assertions.assertThat(mealTickets).allMatch(mealTicket -> !mealTicket.used);
  }

  /**
   * Quando i buoni consegnati non bastano viene segnalata la data in cui sono esauriti.
   */
  @Test
  public void mealTicketsRunOut() {
    List<MealTicket> mealTickets = Lists.newArrayList(
        mealTicket("A", 1, BEGIN, EXPIRE), mealTicket("A", 2, BEGIN, EXPIRE));
    List<PersonDay> personDays = personDays(3);

    MealTicketRecap recap = build(personDays, mealTickets);

    Assertions.assertThat(recap.getRemaining()).isEqualTo(-1);
    Assertions.assertThat(recap.getNegativeResidual()).isEqualTo(1);
    Assertions.assertThat(recap.getDateRunOut()).isEqualTo(personDays.get(2).getDate());
    Assertions.assertThat(personDays.get(2).getMealTicketAssigned()).isNull();
  }

  /**
   * Viene segnalata la data in cui si inizia a consumare un buono già scaduto.
   */
  @Test
  public void expiredMealTicketUsed() {
    LocalDate expire = BEGIN.plusDays(1);
    List<MealTicket> mealTickets = Lists.newArrayList(
        mealTicket("A", 1, BEGIN, expire), mealTicket("A", 2, BEGIN, expire));
    List<PersonDay> personDays = personDays(2);

    MealTicketRecap recap = build(personDays, mealTickets);

    Assertions.assertThat(recap.getDateRunOut()).isNull();
    Assertions.assertThat(recap.getDateExpire()).isEqualTo(personDays.get(1).getDate());
  }

  /**
   * I buoni residui dell'inizializzazione vengono consumati prima di quelli consegnati.
   */
  @Test
  public void sourcedMealTicketsUsedFirst() {
    contract.setSourceDateMealTicket(BEGIN.minusDays(1));
    contract.setSourceRemainingMealTicket(2);
    MealTicket mealTicket = mealTicket("A", 1, BEGIN, EXPIRE);
    List<PersonDay> personDays = personDays(3);

    MealTicketRecap recap = build(personDays, Lists.newArrayList(mealTicket));

    Assertions.assertThat(recap.getSourcedInInterval()).isEqualTo(2);
    Assertions.assertThat(recap.getRemaining()).isZero();
    Assertions.assertThat(recap.getDateRunOut()).isNull();
    Assertions.assertThat(personDays.get(0).getMealTicketAssigned()).isNull();
    Assertions.assertThat(personDays.get(1).getMealTicketAssigned()).isNull();
    Assertions.assertThat(personDays.get(2).getMealTicketAssigned()).isSameAs(mealTicket);
  }

  /**
   * L'inizializzazione precedente all'intervallo dei buoni pasto non viene conteggiata.
   */
  @Test
  public void sourceOutsideIntervalIgnored() {
    contract.setSourceDateMealTicket(BEGIN.minusDays(10));
    contract.setSourceRemainingMealTicket(5);

    MealTicketRecap recap = build(personDays(1),
        Lists.newArrayList(mealTicket("A", 1, BEGIN, EXPIRE)));

    Assertions.assertThat(recap.getSourcedInInterval()).isZero();
    Assertions.assertThat(recap.getRemaining()).isZero();
  }

  /**
   * I buoni consegnati sono raggruppati in blocchi di codici consecutivi.
   */
  @Test
  public void blocksOfConsecutiveCodes() {
    List<MealTicket> mealTickets = Lists.newArrayList(
        mealTicket("A", 1, BEGIN, EXPIRE),
        mealTicket("A", 2, BEGIN, EXPIRE),
        mealTicket("A", 3, BEGIN, EXPIRE),
        mealTicket("A", 5, BEGIN, EXPIRE),
        mealTicket("B", 1, BEGIN, EXPIRE));

    MealTicketRecap recap = build(personDays(2), mealTickets);
    List<BlockMealTicket> blocks = recap.getBlockMealTicketReceived();

    Assertions.assertThat(blocks).hasSize(3);
    Assertions.assertThat(blocks.get(0).getCodeBlock()).isEqualTo("A");
    Assertions.assertThat(blocks.get(0).getFirst()).isEqualTo(1);
    Assertions.assertThat(blocks.get(0).getLast()).isEqualTo(3);
    Assertions.assertThat(blocks.get(0).getConsumed()).isEqualTo(2);
    Assertions.assertThat(blocks.get(0).getRemaining()).isEqualTo(1);
    Assertions.assertThat(blocks.get(1).getFirst()).isEqualTo(5);
    Assertions.assertThat(blocks.get(1).getConsumed()).isZero();
    Assertions.assertThat(blocks.get(2).getCodeBlock()).isEqualTo("B");
    Assertions.assertThat(blocks.get(2).getDimBlock()).isEqualTo(1);
  }

  private MealTicketRecap build(List<PersonDay> personDays, List<MealTicket> expireOrderedAsc) {
    return builder.buildMealTicketRecap(contract, DateInterval.build(BEGIN, null), personDays,
        expireOrderedAsc, expireOrderedAsc, Lists.reverse(expireOrderedAsc),
        Lists.newArrayList());
  }

  private List<PersonDay> personDays(int count) {
    List<PersonDay> personDays = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      personDays.add(new PersonDay(person, BEGIN.plusDays(i + 1)));
    }
    return personDays;
  }

  private MealTicket mealTicket(String block, int number, LocalDate date, LocalDate expireDate) {
    MealTicket mealTicket = new MealTicket();
    mealTicket.setContract(contract);
    mealTicket.setBlock(block);
    mealTicket.setNumber(number);
    mealTicket.setCode(block + number);
    mealTicket.setBlockType(BlockType.papery);
    mealTicket.setDate(date);
    mealTicket.setExpireDate(expireDate);
    return mealTicket;
  }
}
