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

package dao.wrapper;

import com.google.common.base.Optional;
import java.util.List;
import models.Office;
import models.UsersRolesOffices;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Office potenziato.
 *
 * @author Alessandro Martelli
 */
public interface IWrapperOffice extends IWrapperModel<Office> {

  /**
   * La data di creazione della sede.
   *
   * @return la data di installazione della sede.
   */
  LocalDate initDate();
  
  /**
   * Il primo mese per cui è possibile effettuare l'upload degli attestati.
   *
   * @return yearMonth se esiste.
   */
  public Optional<YearMonth> getFirstMonthUploadable();
  
  /**
   * La lista degli anni di cui è possibile effettuare l'invio degli attestati per la sede.
   *
   * @return lista degli anni
   */
  public List<Integer> getYearUploadable();
  
  /**
   * La lista dei mesi di cui è possibile effettuare l'invio degli attestati.
   *
   * @return lista dei mesi
   */
  public List<Integer> getMonthUploadable();

  /**
   * Il mese di cui presumubilmente occorre fare l'invio attestati. (Il precedente rispetto a 
   * quello attuale se non è precedente al primo mese per attestati).
   *
   * @return prossimo mese da inviare
   */
  public Optional<YearMonth> nextYearMonthToUpload();
  
  /**
   * Se il mese passato come parametro è inviabile.
   *
   * @param yearMonth mese da verificare
   * @return esito
   */
  public boolean isYearMonthUploadable(YearMonth yearMonth);
  
  /**
   * La lista dei responsabile sede. 
   */
  public List<UsersRolesOffices> getSeatSupervisor();
  
  /**
   * Gli amministratori tecnici dell'office.
   */
  public List<UsersRolesOffices> getTechnicalAdmins();

  /**
   * Gli amministratori dell'office.
   */
  public List<UsersRolesOffices> getPersonnelAdmins();

  /**
   * I mini amministratori dell'office.
   */
  public List<UsersRolesOffices> getMiniAdmins();

}
