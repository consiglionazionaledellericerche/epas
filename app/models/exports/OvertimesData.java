package models.exports;

import lombok.RequiredArgsConstructor;

/**
 * Classe di supporto per l'esportazione di dati realtivi agli straordinari e ore eccedenti -
 * yearResidualAtMonth: Totale residuo anno corrente a fine mese - monthResidual: Residuo del mese -
 * overtime: tempo disponibile per straordinario.
 *
 * @author arianna
 */
@RequiredArgsConstructor
public class OvertimesData {

  public final int yearResidualAtMonth;
  public final int monthResidual;
  public final int overtime;

}
