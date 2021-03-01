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

package models.dto;

import dao.history.HistoryValue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import manager.recaps.personstamping.PersonStampingRecap;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.User;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;

/**
 * DTO oer le informazioni necessarie alla generazione del PDF 
 * con la situazione mensile di un dipendente.
 */
@Data
@Builder
public class PrintTagsInfo {

  public Person person;
  
  public PersonStampingRecap psDto;
  
  @Builder.Default
  public List<List<HistoryValue<Stamping>>> historyStampingsList = Lists.newArrayList();
  
  @Builder.Default
  public List<OffSiteWorkingTemp> offSiteWorkingTempList = Lists.newArrayList();
  
  public boolean includeStampingDetails;
  
  @Builder.Default
  public Map<User, Set<LocalDate>> stampingOwnersInDays = Maps.newHashMap();
  
  @Builder.Default
  public List<PersonDay> holidaysInShift = Lists.newArrayList();
  
}
