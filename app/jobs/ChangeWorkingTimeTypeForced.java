/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

package jobs;

import java.util.List;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.ContractDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import helpers.Web;
import lombok.extern.slf4j.Slf4j;
import manager.ContractManager;
import manager.PeriodManager;
import manager.recaps.recomputation.RecomputeRecap;
import models.Contract;
import models.ContractWorkingTimeType;
import models.base.IPropertyInPeriod;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@SuppressWarnings("rawtypes")
@Slf4j
@OnApplicationStart(async = true)
public class ChangeWorkingTimeTypeForced extends Job<Void> {

  private static String allattamento = "Allattam";
  private static String maternita = "Materni";


  @Inject
  static WorkingTimeTypeDao wttDao;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static ContractDao contractDao;
  @Inject
  static ContractManager contractManager;

  public void doJob() {
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    List<ContractWorkingTimeType> allattamentoList = wttDao.cwttListByDate(LocalDate.now(), allattamento);
    List<ContractWorkingTimeType> maternitaList = wttDao.cwttListByDate(LocalDate.now(), maternita);
    List<ContractWorkingTimeType> list = Lists.newArrayList();
    list.addAll(maternitaList);
    list.addAll(allattamentoList);

    for (ContractWorkingTimeType cwtt : list) {
      if (cwtt.getWorkingTimeType().getOffice() != null) {
        IWrapperContract wrappedContract = wrapperFactory.create(cwtt.getContract());
        Contract contract = cwtt.getContract();
        List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(cwtt, false);
        RecomputeRecap recomputeRecap =
            periodManager.buildRecap(wrappedContract.getContractDateInterval().getBegin(),
                Optional.fromNullable(wrappedContract.getContractDateInterval().getEnd()),
                periodRecaps, Optional.fromNullable(contract.getSourceDateResidual()));

        recomputeRecap.initMissing = wrappedContract.initializationMissing();

        periodManager.updatePeriods(cwtt, true);
        contract = contractDao.getContractById(contract.id);
        contract.getPerson().refresh();
        if (recomputeRecap.needRecomputation) {
          contractManager.recomputeContract(contract,
              Optional.fromNullable(recomputeRecap.recomputeFrom), false, false);
        }

      } else {
        log.info("Non occorre modificare un orario di un contratto che già contiene l'orario generico.");
      }
    }

  }
}
