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

package manager.recaps.recomputation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.ToString;
import manager.configurations.EpasParam;
import models.base.IPropertyInPeriod;
import org.joda.time.LocalDate;

/**
 * Riepilogo dei dati ricalcolati.
 */
@ToString
public class RecomputeRecap {

  public List<IPropertyInPeriod> periods = Lists.newArrayList();
  
  public LocalDate recomputeFrom;
  public Optional<LocalDate> recomputeTo;
  public boolean onlyRecaps;

  //Dato da utilizzare in caso di modifica contratto.
  public boolean initMissing;
  
  //Dato da utilizzare in caso di modifica configurazione.
  public EpasParam epasParam; 
  
  
  public boolean needRecomputation;

  public int days;
}
