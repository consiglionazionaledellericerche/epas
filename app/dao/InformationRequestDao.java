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

package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.UsersRolesOffices;
import models.enumerate.InformationType;
import models.flows.Group;
import models.flows.enumerate.AbsenceRequestType;
import models.informationrequests.IllnessRequest;
import models.informationrequests.ServiceRequest;
import models.informationrequests.TeleworkRequest;

/**
 * Dao per i flussi informativi.
 * 
 * @author dario
 *
 */
public class InformationRequestDao extends DaoBase {

  @Inject
  InformationRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  public List<IllnessRequest> toApproveIllnessResults() {
    return null;
  }
  
  public List<TeleworkRequest> toApproveTeleworkResults(List<UsersRolesOffices> uroList, 
      Optional<LocalDateTime> fromDate, Optional<LocalDateTime> toDate,
      InformationType informationType, List<Group> groups, Person signer) {
    return null;
  }
  
  public List<ServiceRequest> toApproveServiceResults() {
    return null;
  }
}
