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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import dao.OfficeDao;
import dao.UsersRolesOfficesDao;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType;
import manager.configurations.EpasParam.EpasParamValueType.IpList;
import models.Configuration;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import play.Play;

/**
 * Manager per la gestione degli uffici.
 */
@Slf4j
public class OfficeManager {

  public static final String SKIP_IP_CHECK = "skip.ip.check";

  // Evita di ripetere nel log lo stesso avviso ad ogni richiesta.
  private static final Cache<String, Boolean> COMPATIBILITY_WARNINGS = CacheBuilder.newBuilder()
      .expireAfterWrite(1, TimeUnit.HOURS).maximumSize(1000).build();

  private final UsersRolesOfficesDao usersRolesOfficesDao;
  private final ConfigurationManager configurationManager;
  private final OfficeDao officeDao;

  /**
   * Default constructor.
   */
  @Inject
  public OfficeManager(
      UsersRolesOfficesDao usersRolesOfficesDao,
      ConfigurationManager configurationManager,
      OfficeDao officeDao) {
    this.usersRolesOfficesDao = usersRolesOfficesDao;
    this.configurationManager = configurationManager;
    this.officeDao = officeDao;
  }

  /**
   * True se il permesso sull'ufficio viene creato, false se è esistente.
   *
   * @return true Se il permesso su quell'ufficio viene creato, false se è già esistente.
   */
  public boolean setUro(User user, Office office, Role role) {

    Optional<UsersRolesOffices> uro = 
        usersRolesOfficesDao.getUsersRolesOffices(user, role, office);

    if (!uro.isPresent()) {

      UsersRolesOffices newUro = new UsersRolesOffices();
      newUro.setUser(user);
      newUro.setOffice(office);
      newUro.setRole(role);
      newUro.save();
      return true;
    }

    return false;
  }


  /**
   * Le sedi che hanno la timbratura web abilitata e l'indirizzo ip passato, oppure
   * uno degli indirizzi accettati per compatibilità, tra quelli abilitati per le timbrature.
   *
   * <p>Le sedi abilitate solo grazie agli indirizzi di compatibilità (falsificabili tramite
   * l'header X-Forwarded-For) vengono segnalate nel log, per individuare le installazioni
   * in cui è necessario configurare XForwardedSupport con gli ip dei reverse proxy.</p>
   *
   * @param ipAddress indirizzo ip del client da verificare (vedi ClientAddress)
   * @param compatibilityAddresses gli altri indirizzi della catena X-Forwarded-For accettati
   *     per compatibilità (vedi ClientAddress#compatibilityAddresses)
   * @return Set di uffici abilitati dagli indirizzi ip passati come parametro
   */
  public Set<Office> getOfficesWithAllowedIp(final Optional<String> ipAddress,
      final List<String> compatibilityAddresses) {

    Preconditions.checkNotNull(ipAddress);
    Preconditions.checkNotNull(compatibilityAddresses);

    if ("true".equals(Play.configuration.getProperty(SKIP_IP_CHECK))) {
      log.debug("Skipped IP check");
      return new HashSet<>(officeDao.getAllOffices());
    }

    if (!ipAddress.isPresent()) {
      log.debug("Remote address not present, web stamping not permitted");
      return Sets.newHashSet();
    }

    List<Office> officesWebStampingEnabled = officeDao.getOfficesWebStampingEnabled();
    log.debug("officesWebStampingEnabled= {}", officesWebStampingEnabled);
    
    Set<Office> offices = Sets.newHashSet();
    Set<Office> compatibilityOffices = Sets.newHashSet();
    List<Configuration> configurationWithType = configurationManager
        .configurationWithType(EpasParam.ADDRESSES_ALLOWED);

    for (Configuration configuration : configurationWithType) {
      IpList ipList = (IpList) EpasParamValueType.parseValue(EpasParamValueType.IP_LIST,
          (String) configuration.getValue());
      if (ipList == null || !officesWebStampingEnabled.contains(configuration.getOffice())) {
        continue;
      }
      if (ipList.ipList.contains(ipAddress.get())) {
        offices.add(configuration.getOffice());
      } else if (compatibilityAddresses.stream().anyMatch(ipList.ipList::contains)) {
        compatibilityOffices.add(configuration.getOffice());
      }
    }
    compatibilityOffices.removeAll(offices);
    if (!compatibilityOffices.isEmpty()) {
      warnCompatibilityAccess(ipAddress.get(), compatibilityAddresses, compatibilityOffices);
      offices.addAll(compatibilityOffices);
    }
    return offices;
  }

  private static void warnCompatibilityAccess(String ipAddress,
      List<String> compatibilityAddresses, Set<Office> offices) {
    final String key = ipAddress + "|" + compatibilityAddresses;
    if (COMPATIBILITY_WARNINGS.asMap().putIfAbsent(key, Boolean.TRUE) != null) {
      return;
    }
    log.warn("Timbratura web abilitata per le sedi {} solo per compatibilità tramite gli "
        + "indirizzi {} dell'header X-Forwarded-For (ip attendibile {}). Questi indirizzi sono "
        + "falsificabili dal client: impostare in XForwardedSupport (X_FORWARDED_SUPPORT nel "
        + "docker) gli ip dei reverse proxy. In una prossima versione non saranno più accettati.",
        offices.stream().map(Office::getName).collect(Collectors.toList()),
        compatibilityAddresses, ipAddress);
  }

}