import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import dao.VacationCodeDao;

import manager.ContractManager;
import manager.services.vacations.IVacationsRecap;
import manager.services.vacations.impl.VacationsRecap;

import models.Absence;
import models.Contract;
import models.VacationCode;
import models.VacationPeriod;

import org.joda.time.LocalDate;
import org.mockito.Mock;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import static org.mockito.Mockito.*;

import java.util.List;

/**
 * Verifica di base degli algoritmi relativi ai resoconti ferie.
 *
 * @author cristian
 *
 */
public class VacationsRecapTest {

  @Test
  public void testComputedVacations() {

    final List<Absence> absencesToConsider = Lists.newArrayList();

    final LocalDate accruedDate = new LocalDate(2015,12,23);
    final LocalDate expireDateLastYear = new LocalDate(2015,8,31);
    final LocalDate expireDateCurrentYear = new LocalDate(2016,8,31);

    final VacationsRecap vacationRecap = VacationsRecap.builder()
        .year(2015)
        .contract(getLucchesiContract())
        .absencesToConsider(absencesToConsider)
        .accruedDate(accruedDate)
        .expireDateLastYear(expireDateLastYear)
        .expireDateCurrentYear(expireDateCurrentYear)
        .build();


    assertThat(vacationRecap.getVacationsLastYear().getTotal()).isEqualTo(28);
    assertThat(vacationRecap.getVacationsLastYear().getUsed()).isEqualTo(0);
    assertThat(vacationRecap.getVacationsLastYear().getNotYetUsedTotal()).isEqualTo(28);
    assertThat(vacationRecap.getVacationsLastYear().getNotYetUsedAccrued()).isEqualTo(0);
  }

  private Contract getLucchesiContract() {

    Contract contract = mock(Contract.class);
    when(contract.getSourceDateResidual()).thenReturn(new LocalDate(2012,12,31));
    when(contract.getSourceVacationLastYearUsed()).thenReturn(0);
    when(contract.getSourceVacationCurrentYearUsed()).thenReturn(0);
    when(contract.getSourcePermissionUsed()).thenReturn(0);
    when(contract.getBeginDate()).thenReturn(new LocalDate(2009,2,01));
    List<VacationPeriod> vacationPeriods = getContractManager().contractVacationPeriods(contract);
    when(contract.getVacationPeriods()).thenReturn(vacationPeriods);
    return contract;
  }

  private VacationCode vacationCode(String description) {
    VacationCode vc = new VacationCode();
    vc.description = description;
    //FIXME: questa parte non va bene perché non copre tutti i casi
    if (description.equals("26+4")) {
      vc.vacationDays = 26;
    } else {
      vc.vacationDays = 28;
    }
    return vc;
  }

  public ContractManager getContractManager() {
    return new ContractManager(null, getVacationCodeDao(), null);
  }

  private VacationCodeDao getVacationCodeDao() {
    VacationCodeDao vcd = mock(VacationCodeDao.class);

    when(vcd.getVacationCodeByDescription("26+4")).thenReturn(vacationCode("26+4"));
    when(vcd.getVacationCodeByDescription("28+4")).thenReturn(vacationCode("28+4"));
    return vcd;
  }

}
