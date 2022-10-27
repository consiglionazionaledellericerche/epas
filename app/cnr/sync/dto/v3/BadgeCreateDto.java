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

package cnr.sync.dto.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import common.injection.StaticInject;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.PersonDao;
import javax.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;
import models.Badge;
import org.modelmapper.ModelMapper;
import play.data.validation.Required;

/**
 * Dati per la creazione via REST di un badge associato ad una persona.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
@Data
@EqualsAndHashCode(callSuper = true)
public class BadgeCreateDto extends BadgeUpdateDto {

  @Required
  private Long personId;

  @Required
  private Long badgeSystemId;

  @Required
  private Long badgeReaderId;

  @Inject
  @JsonIgnore
  static ModelMapper modelMapper;

  @Inject
  @JsonIgnore
  static PersonDao personDao;

  @Inject
  @JsonIgnore
  static BadgeReaderDao badgeReaderDao;

  @Inject
  @JsonIgnore
  static BadgeSystemDao badgeSystemDao;

  /**
   * Nuova istanza di un oggetto Badge a partire dai 
   * valori presenti nel rispettivo DTO.
   */
  public static Badge build(BadgeCreateDto badgeCreateDto) {
    val badge = modelMapper.map(badgeCreateDto, Badge.class);
    badge.setPerson(personDao.getPersonById(badgeCreateDto.getPersonId()));
    badge.setBadgeReader(badgeReaderDao.byId(badgeCreateDto.getBadgeReaderId()));
    badge.setBadgeSystem(badgeSystemDao.byId(badgeCreateDto.getBadgeSystemId()));
    return badge;
  }

}