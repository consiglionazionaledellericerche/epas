import models.Person;
import models.PersonMonth;

/**
 * 
 * @author dario
 *
 */
public class UltimateMonthResidualTest {

	public static int RESIDUO_ANNO_PRECEDENTE = 500;

	private static final class PersonMonthUltimate extends PersonMonth{
		private int residuoDelMese;
		private int residuoAnnoPrecedenteDisponibileAllInizioDelMese;
		private int recuperiOreDaAnnoPrecedente;
		private int residuoAnnoPrecedenteDisponibileAllaFineDelMese;
		private int riposoCompensativiDaAnnoPrecedente;
		private int riposiCompensativiDaAnnoCorrente;
		private int straordinari;
		private int residuoMesePrecedenteDaAnnoCorrente;
		private int totaleResiduoAnnoCorrenteAlMesePrecedente;
		private int totaleResiduoAnnoCorrenteAFineMese;
		private int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese;
		
		public PersonMonthUltimate(int year, int month) {
			super(new Person(),year,month);
		}
	}


}
