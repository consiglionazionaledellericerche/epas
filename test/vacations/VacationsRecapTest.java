package vacations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import dao.VacationCodeDao;

import manager.ContractManager;
import manager.services.vacations.VacationsRecap;
import manager.services.vacations.VacationsRecapBuilder;

import models.Absence;
import models.Contract;
import models.VacationCode;
import models.VacationPeriod;

import org.joda.time.LocalDate;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import mocker.MockContract;

/**
 * Verifica di base degli algoritmi relativi ai resoconti ferie.
 *
 * @author cristian
 *
 */
public class VacationsRecapTest {
  
  @Test
  public void testTotalVacations() {

    final List<Absence> absencesToConsider = Lists.newArrayList();

    final LocalDate accruedDate = new LocalDate(2016, 9, 1);    //recap date
    final LocalDate expireDateLastYear = new LocalDate(2016, 8, 31);
    final LocalDate expireDateCurrentYear = new LocalDate(2017, 8, 31);

    //Un tempo indeterminato
    Contract contract = MockContract.builder()
        .contractManager(getContractManager())
        .beginDate(new LocalDate(2009,2,01))
        .build();
    
    final VacationsRecap recapIndef = new VacationsRecapBuilder().buildVacationRecap(
        2016, contract, absencesToConsider, accruedDate, expireDateLastYear, expireDateCurrentYear);

    assertThat(recapIndef.getVacationsLastYear().isExpired()).isEqualTo(true);
    assertThat(recapIndef.getVacationsLastYear().getTotal()).isEqualTo(28);
    assertThat(recapIndef.getVacationsLastYear().getNotYetUsedTotal()).isEqualTo(28);
    assertThat(recapIndef.getVacationsLastYear().getNotYetUsedTakeable()).isEqualTo(0);
    
    assertThat(recapIndef.getVacationsCurrentYear().isExpired()).isEqualTo(false);
    assertThat(recapIndef.getVacationsCurrentYear().getTotal()).isEqualTo(28);
    assertThat(recapIndef.getVacationsCurrentYear().getNotYetUsedTotal()).isEqualTo(28);
    assertThat(recapIndef.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(28);
    
    assertThat(recapIndef.getPermissions().isExpired()).isEqualTo(false);
    assertThat(recapIndef.getPermissions().getTotal()).isEqualTo(4);
    assertThat(recapIndef.getPermissions().getNotYetUsedTotal()).isEqualTo(4);
    assertThat(recapIndef.getPermissions().getNotYetUsedTakeable()).isEqualTo(4);
    
    //Un tempo determinato
    contract = MockContract.builder()
        .contractManager(getContractManager())
        .beginDate(new LocalDate(2011, 10, 1))
        .endDate(new LocalDate(2016, 10, 1))
        .build();
    
    final VacationsRecap recapDef = new VacationsRecapBuilder().buildVacationRecap(
        2016, contract, absencesToConsider, accruedDate, expireDateLastYear, expireDateCurrentYear);
    
    assertThat(recapDef.getVacationsLastYear().isExpired()).isEqualTo(true);
    assertThat(recapDef.getVacationsLastYear().getTotal()).isEqualTo(28);
    assertThat(recapDef.getVacationsLastYear().getNotYetUsedTotal()).isEqualTo(28);
    assertThat(recapDef.getVacationsLastYear().getNotYetUsedTakeable()).isEqualTo(0);
    
    assertThat(recapDef.getVacationsCurrentYear().isExpired()).isEqualTo(false);
    assertThat(recapDef.getVacationsCurrentYear().getTotal()).isEqualTo(21);
    assertThat(recapDef.getVacationsCurrentYear().getNotYetUsedTotal()).isEqualTo(21);
    assertThat(recapDef.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(18);
    
    assertThat(recapDef.getPermissions().isExpired()).isEqualTo(false);
    assertThat(recapDef.getPermissions().getTotal()).isEqualTo(3);
    assertThat(recapDef.getPermissions().getNotYetUsedTotal()).isEqualTo(3);
    assertThat(recapDef.getPermissions().getNotYetUsedTakeable()).isEqualTo(3);
  }

  private VacationCode vacationCode(String description) {
    VacationCode vc = new VacationCode();
    vc.description = description;
    if (description.equals("26+4")) {
      vc.vacationDays = 26;
      vc.permissionDays = 4;
    } else if (description.equals("28+4")) {
      vc.vacationDays = 28;
      vc.permissionDays = 4;
    } else if (description.equals("21+3")) {
      vc.vacationDays = 21;
      vc.permissionDays = 3;
    } else if (description.equals("22+3")) {
      vc.vacationDays = 22;
      vc.permissionDays = 3;
    }
    return vc;
  }

  public ContractManager getContractManager() {
    return new ContractManager(null, getVacationCodeDao(), null, null);
  }

  private VacationCodeDao getVacationCodeDao() {
    VacationCodeDao vcd = mock(VacationCodeDao.class);

    when(vcd.getVacationCodeByDescription("26+4")).thenReturn(vacationCode("26+4"));
    when(vcd.getVacationCodeByDescription("28+4")).thenReturn(vacationCode("28+4"));
    
    when(vcd.getVacationCodeByDescription("21+3")).thenReturn(vacationCode("21+3"));
    when(vcd.getVacationCodeByDescription("22+3")).thenReturn(vacationCode("22+3"));
    return vcd;
  }
  
  List<Absence> getAbsences(ImmutableList<LocalDate> dates) {
    return null;
  }

}
