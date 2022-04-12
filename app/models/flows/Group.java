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

import com.google.common.collect.Lists;
import it.cnr.iit.epas.NullStringBinder;
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
import models.Office;
import models.Person;
import models.base.MutableModel;
import org.hibernate.envers.Audited;
import play.data.binding.As;
import play.data.validation.Required;
import play.data.validation.Unique;

/**
 * Rappresenta un gruppo di Persone.
 */
@Getter
@Audited
@Entity
@Table(name = "groups")
public class Group extends MutableModel {

  private static final long serialVersionUID = -5169540784395404L;

  @Unique(value = "office, name")
  public String name;

  public String description;

  @Column(name = "send_flows_email")
  public boolean sendFlowsEmail;

  @ManyToOne
  @JoinColumn(name = "office_id", nullable = false)
  public Office office;

  @ManyToOne
  @JoinColumn(name = "manager", nullable = false)
  @Required
  public Person manager;

  @OneToMany(mappedBy = "group")
  public List<Affiliation> affiliations = Lists.newArrayList();

  @As(binder = NullStringBinder.class)
  @Unique(value = "office, externalId")
  public String externalId;

  public LocalDate endDate;

  /**
   * Verificat se un gruppo è sempre attivo alla data attuale.
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
  
  public String getLabel() {
    return name;
  }
  
}