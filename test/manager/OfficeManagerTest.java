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

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import common.injection.StaticInject;
import java.util.Set;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Configuration;
import models.Office;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * Verifica delle sedi abilitate alla timbratura web in base all'indirizzo ip del client.
 */
@StaticInject
public class OfficeManagerTest extends UnitTest {

  @Inject
  private static OfficeManager officeManager;

  @Inject
  private static ConfigurationManager configurationManager;

  private Office office;
  private Office otherOffice;
  private Object skipIpCheck;

  /**
   * Una sede con timbratura web abilitata dagli ip 192.168.1.10 e 192.168.1.11.
   */
  @Before
  public void createOffice() {
    skipIpCheck = Play.configuration.remove(OfficeManager.SKIP_IP_CHECK);
    office = createOffice("990099", "true", "192.168.1.10, 192.168.1.11");
    JPA.em().flush();
    JPA.em().clear();
    office = Office.findById(office.id);
  }

  private Office createOffice(String code, String webStamping, String addresses) {
    Office newOffice = new Office();
    newOffice.setName("Sede test timbrature web " + code);
    newOffice.setBeginDate(new LocalDate(2020, 1, 1));
    newOffice.setCodeId(code);
    newOffice.setCode(code);
    newOffice.save();
    configurationManager.updateConfigurations(newOffice);
    setConfiguration(newOffice, EpasParam.WEB_STAMPING_ALLOWED, webStamping);
    setConfiguration(newOffice, EpasParam.ADDRESSES_ALLOWED, addresses);
    return newOffice;
  }

  private void setConfiguration(Office target, EpasParam epasParam, String value) {
    Configuration configuration = Configuration
        .find("office = ?1 and epasParam = ?2", target, epasParam).first();
    configuration.setValue(value);
    configuration.save();
  }

  /**
   * Rimuove la sede di test e ripristina la configurazione.
   */
  @After
  public void deleteOffice() {
    if (skipIpCheck != null) {
      Play.configuration.put(OfficeManager.SKIP_IP_CHECK, skipIpCheck);
    }
    if (otherOffice != null) {
      Office.<Office>findById(otherOffice.id).delete();
    }
    if (office != null) {
      Office.<Office>findById(office.id).delete();
    }
  }

  @Test
  public void allowedClientAddress() {
    assertTrue(officeManager.getOfficesWithAllowedIp(
        Optional.of("192.168.1.10"), ImmutableList.of()).contains(office));
  }

  @Test
  public void notAllowedClientAddress() {
    assertFalse(officeManager.getOfficesWithAllowedIp(
        Optional.of("203.0.113.5"), ImmutableList.of()).contains(office));
    assertTrue(officeManager.getOfficesWithAllowedIp(
        Optional.absent(), ImmutableList.of("192.168.1.10")).isEmpty());
  }

  @Test
  public void allowedCompatibilityAddress() {
    // Catena "192.168.1.11, 10.0.0.2" con XForwardedSupport=all.
    assertTrue(officeManager.getOfficesWithAllowedIp(
        Optional.of("10.0.0.2"), ImmutableList.of("192.168.1.11")).contains(office));
  }

  @Test
  public void webStampingDisabled() {
    // Stesso indirizzo di una sede abilitata, ma timbratura web non abilitata.
    otherOffice = createOffice("990098", "false", "192.168.1.10");
    JPA.em().flush();
    JPA.em().clear();
    final Set<Office> offices = officeManager.getOfficesWithAllowedIp(
        Optional.of("192.168.1.10"), ImmutableList.of());
    assertTrue(offices.contains(office));
    assertFalse(offices.contains(Office.findById(otherOffice.id)));
  }

  @Test
  public void closedOffice() {
    otherOffice = createOffice("990097", "true", "192.168.1.20");
    otherOffice.setEndDate(new LocalDate(2021, 1, 1));
    otherOffice.save();
    JPA.em().flush();
    JPA.em().clear();
    assertTrue(officeManager.getOfficesWithAllowedIp(
        Optional.of("192.168.1.20"), ImmutableList.of()).isEmpty());
  }

  @Test
  public void moreOfficesWithSameAddress() {
    otherOffice = createOffice("990096", "true", "192.168.1.11,192.168.1.30");
    JPA.em().flush();
    JPA.em().clear();
    final Set<Office> offices = officeManager.getOfficesWithAllowedIp(
        Optional.of("192.168.1.11"), ImmutableList.of());
    assertEquals(2, offices.size());
    assertTrue(offices.contains(office));
    assertTrue(offices.contains(Office.findById(otherOffice.id)));
  }
}
