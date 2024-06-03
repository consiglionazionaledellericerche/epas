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

package models.flows;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import models.GroupOvertime;
import models.Office;
import models.Person;
import models.TotalOvertime;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Rappresenta un gruppo di Persone.
 */
@Getter
@Setter
@Audited
@Entity
@Table(name = "groups")
public class Group extends MutableModel {

  private static final long serialVersionUID = -5169540784395404L;

  @Unique(value = "office, name")
  private String name;

  private String description;

  @Column(name = "send_flows_email")
  private boolean sendFlowsEmail;

  @ManyToOne
  @JoinColumn(name = "office_id", nullable = false)
  private Office office;

  @ManyToOne
  @JoinColumn(name = "manager", nullable = false)
  @Required
  private Person manager;

  @OneToMany(mappedBy = "group")
  private List<Affiliation> affiliations = Lists.newArrayList();

  @OneToMany(mappedBy = "group")
  private List<GroupOvertime> groupOvertimes = Lists.newArrayList();

  @Unique(value = "office, externalId")
  private String externalId;

  private LocalDate endDate;

  /**
   * Rimuove eventuali spazi prima e dopo e trasforma le stringhe
   * vuote in null.
   */
  public void setExternalId(String externalId) {
    externalId = Strings.emptyToNull(externalId) == null ? null : externalId.trim();
  }

  /**
   * Verifica se un gruppo è sempre attivo alla data attuale.
   *
   * @return true se il gruppo non ha una data di fine passata.
   */
  public boolean isActive() {
    return endDate == null || endDate.isAfter(LocalDate.now());
  }

  /**
   * La lista delle persone che appartengono al gruppo
   * ad una certa data.
   */
  @Transient
  public List<Person> getPeople(LocalDate date) {
    return affiliations.stream()
        .filter(a -> !a.getBeginDate().isAfter(date) 
            && (a.getEndDate() == null || a.getEndDate().isAfter(date)))
        .map(a -> a.getPerson())
        .collect(Collectors.toList());
  }

  /**
   * La lista delle persone che appartengono al gruppo
   * alla data odierna.
   */
  @Transient
  public List<Person> getPeople() {
    return getPeople(LocalDate.now());
  }

  @Transient
  public List<Person> getPeopleOvertimes() {
    return getPeople().stream()
        .filter(p -> !p.isTopQualification()).collect(Collectors.toList());
  }

  public String getLabel() {
    return name;
  }

}