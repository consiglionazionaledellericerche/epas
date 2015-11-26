/**
 *
 */
package models.exports;


/**
 * Classe di supporto per l'esportazione di dati realtivi agli straordinari e ore eccedenti -
 * yearResidualAtMonth: Totale residuo anno corrente a fine mese - monthResidual: Residuo del mese -
 * overtime: tempo disponibile per straordinario
 *
 * @author arianna
 */
public class OvertimesData {

  public final int yearResidualAtMonth;
  public final int monthResidual;
  public final int overtime;


  public OvertimesData(int yearResidualAtMonth, int monthResidual, int overtime) {
    this.yearResidualAtMonth = yearResidualAtMonth;
    this.monthResidual = monthResidual;
    this.overtime = overtime;
  }

}
