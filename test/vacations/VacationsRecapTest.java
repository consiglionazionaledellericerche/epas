package vacations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import manager.ContractManager;
import manager.services.vacations.VacationsRecap;
import manager.services.vacations.VacationsRecapBuilder;

import models.Absence;
import models.AbsenceType;
import models.Contract;
import models.PersonDay;
import models.enumerate.AbsenceTypeMapping;
import models.enumerate.VacationCode;

import org.joda.time.LocalDate;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import mocker.MockAbsence;
import mocker.MockAbsenceType;
import mocker.MockContract;
import mocker.MockPersonDay;

/**
 * Verifica di base degli algoritmi relativi ai resoconti ferie.
 *
 * @author cristian
 *
 */
public class VacationsRecapTest {
  
  static AbsenceType code31 = MockAbsenceType.builder()
      .code(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE.getCode())
      .build();
  
  static AbsenceType code32 = MockAbsenceType.builder()
      .code(AbsenceTypeMapping.FERIE_ANNO_CORRENTE.getCode())
      .build();
  
  static AbsenceType code37 = MockAbsenceType.builder()
      .code(AbsenceTypeMapping.FERIE_ANNO_PRECEDENTE_DOPO_31_08.getCode())
      .build();
  
  static AbsenceType code94 = MockAbsenceType.builder()
      .code(AbsenceTypeMapping.FESTIVITA_SOPPRESSE.getCode())
      .build();
  
  /**
   * Test funzionalità di base per contratti indeterminati e determinati.
   */
  @Test
  public void vacationsTestBase() {

    final List<Absence> absencesToConsider = Lists.newArrayList();
    
    absencesToConsider.addAll(getAbsences(ImmutableList.<LocalDate>builder()
        .add(new LocalDate(2016, 1, 1))
        .build(), code31));
    
    absencesToConsider.addAll(getAbsences(ImmutableList.<LocalDate>builder()
        .add(new LocalDate(2016, 9, 1))
        .build(), code37));
    
    absencesToConsider.addAll(getAbsences(ImmutableList.<LocalDate>builder()
        .add(new LocalDate(2016, 9, 10))
        .build(), code32));
    
    absencesToConsider.addAll(getAbsences(ImmutableList.<LocalDate>builder()
        .add(new LocalDate(2016, 9, 11))
        .build(), code94));

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
    assertThat(recapIndef.getVacationsLastYear().getUsed()).isEqualTo(2);
    assertThat(recapIndef.getVacationsLastYear().getNotYetUsedTotal()).isEqualTo(26);
    assertThat(recapIndef.getVacationsLastYear().getNotYetUsedTakeable()).isEqualTo(0);
    
    assertThat(recapIndef.getVacationsCurrentYear().isExpired()).isEqualTo(false);
    assertThat(recapIndef.getVacationsCurrentYear().getTotal()).isEqualTo(28);
    assertThat(recapIndef.getVacationsCurrentYear().getUsed()).isEqualTo(1);
    assertThat(recapIndef.getVacationsCurrentYear().getNotYetUsedTotal()).isEqualTo(27);
    assertThat(recapIndef.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(27);
    
    assertThat(recapIndef.getPermissions().isExpired()).isEqualTo(false);
    assertThat(recapIndef.getPermissions().getTotal()).isEqualTo(4);
    assertThat(recapIndef.getPermissions().getUsed()).isEqualTo(1);
    assertThat(recapIndef.getPermissions().getNotYetUsedTotal()).isEqualTo(3);
    assertThat(recapIndef.getPermissions().getNotYetUsedTakeable()).isEqualTo(3);
    
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
    assertThat(recapDef.getVacationsLastYear().getUsed()).isEqualTo(2);
    assertThat(recapDef.getVacationsLastYear().getNotYetUsedTotal()).isEqualTo(26);
    assertThat(recapDef.getVacationsLastYear().getNotYetUsedTakeable()).isEqualTo(0);
    
    assertThat(recapDef.getVacationsCurrentYear().isExpired()).isEqualTo(false);
    assertThat(recapDef.getVacationsCurrentYear().getTotal()).isEqualTo(21);
    assertThat(recapDef.getVacationsCurrentYear().getUsed()).isEqualTo(1);
    assertThat(recapDef.getVacationsCurrentYear().getNotYetUsedTotal()).isEqualTo(20);
    assertThat(recapDef.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(17);
    
    assertThat(recapDef.getPermissions().isExpired()).isEqualTo(false);
    assertThat(recapDef.getPermissions().getTotal()).isEqualTo(3);
    assertThat(recapDef.getPermissions().getUsed()).isEqualTo(1);
    assertThat(recapDef.getPermissions().getNotYetUsedTotal()).isEqualTo(2);
    assertThat(recapDef.getPermissions().getNotYetUsedTakeable()).isEqualTo(2);
  }
  
  /**
   * Cambiando piano ferie nel corso dell'anno 2015 per quell'anno disponeva di soli 25 giorni.
   * Quindi gliene diamo uno in più che viene maturata immediatamente.
   * Il test costruisce il recap al primo giorno del 2015 e dimostra che in quel momento taverniti
   * ha un giorno di ferie immediatamente maturato, e 26 totali (subito prendibili perchè è un tempo
   * indeterminato).
   * Piano Ferie 26+4
   * Intervallo  Dal 01/01/2015 al 29/10/2015
   * Giorni considerati  302
   * Giorni calcolati    21
   * Piano Ferie 28+4
   * Intervallo  Dal 30/10/2015 al 31/12/2015
   * Giorni considerati  63
   * Giorni calcolati    4
   */
  @Test
  public void theCuriousCaseOfMariaTaverniti() {
    
    final List<Absence> absencesToConsider = Lists.newArrayList();

    final LocalDate accruedDate = new LocalDate(2015, 1, 1);    //recap date
    final LocalDate expireDateLastYear = new LocalDate(2014, 8, 31);
    final LocalDate expireDateCurrentYear = new LocalDate(2015, 8, 31);

    Contract contract = MockContract.builder()
        .contractManager(getContractManager())
        .beginDate(new LocalDate(2012,10,30))
        .build();
    
    final VacationsRecap recap = new VacationsRecapBuilder().buildVacationRecap(
        2015, contract, absencesToConsider, accruedDate, expireDateLastYear, expireDateCurrentYear);

    assertThat(recap.getVacationsCurrentYear().getTotalResult().getAccrued()).isEqualTo(25);
    assertThat(recap.getVacationsCurrentYear().getTotalResult().getFixed()).isEqualTo(1);
    assertThat(recap.getVacationsCurrentYear().getAccruedResult().getAccrued()).isEqualTo(0);
    
    assertThat(recap.getVacationsCurrentYear().getTotal()).isEqualTo(26);
    assertThat(recap.getVacationsCurrentYear().getAccrued()).isEqualTo(1);
    assertThat(recap.getVacationsCurrentYear().getNotYetUsedAccrued()).isEqualTo(1);
    assertThat(recap.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(26);
  }
  
  /**
   * Quando il cambio di piano durante l'anno porta ad avere un numero superiore di ferie 
   * rispetto al valore massimo fra i piani ferie.
   * Si adotta l'aggiustamento.
   */
  @Test
  public void tooLucky() {
    
    final List<Absence> absencesToConsider = Lists.newArrayList();

    final LocalDate accruedDate = new LocalDate(2016, 1, 1);    //recap date
    final LocalDate expireDateLastYear = new LocalDate(2015, 8, 31);
    final LocalDate expireDateCurrentYear = new LocalDate(2016, 8, 31);

    Contract contract = MockContract.builder()
        .contractManager(getContractManager())
        .beginDate(new LocalDate(2013,4,17))
        .build();
    
    final VacationsRecap recap = new VacationsRecapBuilder().buildVacationRecap(
        2016, contract, absencesToConsider, accruedDate, expireDateLastYear, expireDateCurrentYear);

    assertThat(recap.getVacationsCurrentYear().getTotalResult().getAccrued()).isEqualTo(29);
    assertThat(recap.getVacationsCurrentYear().getTotalResult().getFixed()).isEqualTo(-1);
    assertThat(recap.getVacationsCurrentYear().getAccruedResult().getAccrued()).isEqualTo(0);
    
    assertThat(recap.getVacationsCurrentYear().getTotal()).isEqualTo(28);
    // FIXME: le accrued dovrebbero avere il limite inferiore zero. Modificare l'algoritmo.
    assertThat(recap.getVacationsCurrentYear().getAccrued()).isEqualTo(-1);  
    assertThat(recap.getVacationsCurrentYear().getNotYetUsedTakeable()).isEqualTo(28);
  }
  
  

  public ContractManager getContractManager() {
    return new ContractManager(null, null, null, null);
  }
    
  List<Absence> getAbsences(ImmutableList<LocalDate> dates, AbsenceType absenceType) {
    
    List<Absence> absences = Lists.newArrayList();
    
    for (LocalDate date :  dates) {
      PersonDay personDay = MockPersonDay.builder()
          .date(date)
          .build();
      Absence absence = MockAbsence.builder()
          .absenceType(absenceType)
          .personDay(personDay)
          .date(date)
          .build();
      absences.add(absence);
    }
    return absences;
  }

}
