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

package cnr.sync.dto.v2;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Dati esportati in Json per le informazioni mensili per la generazione
 * della busta paga.
 *
 * @author Cristian Lucchesi
 *
 */
@Builder
@Data
public class CertificationObject {

  @Builder.Default
  public List<CertificationAbsenceDto> absences = Lists.newArrayList();
  @Builder.Default
  public List<CertificationCompetencesDto> competences = Lists.newArrayList();
  @Builder.Default
  public List<CertificationMealTicketDto> tickets = Lists.newArrayList();
  @Builder.Default
  public List<CertificationTrainingHoursDto> trainingHours = Lists.newArrayList();
}
