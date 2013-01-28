import static org.junit.Assert.*;
import junit.framework.Assert;

import org.joda.time.LocalDate;
import org.junit.Test;
import lombok.Getter;
import lombok.Setter;
import models.Person;
import models.PersonMonth;

/**
 * 
 * @author dario
 *
 */
public class UltimateMonthResidualTest {

	private static final class PersonMonthUltimate extends PersonMonth {

		@Getter @Setter
		private int personLevel = 4;

		@Getter @Setter
		private PersonMonthUltimate mesePrecedente = null;

		@Getter @Setter
		private int residuoAnnoPrecedente = 0;

		@Getter @Setter
		public int residuoDelMeseInPositivo;
		
		@Getter @Setter
		public int residuoDelMeseInNegativo;
		
		public int recuperiOreDaAnnoPrecedente;
		public int riposoCompensativiDaAnnoPrecedente;
		public int riposiCompensativiDaAnnoCorrente;
		
		@Getter @Setter
		public int straordinari;

			
		public PersonMonthUltimate(int year, int month) {
			super(new Person(),year,month);
		}

		public boolean possibileUtilizzareResiduoAnnoPrecedente() {
			//Dipende dal livello.... e da
			return month <= 3 || personLevel <= 3;
		}

		public int residuoDelMese() {
			return residuoDelMeseInPositivo + residuoDelMeseInNegativo;
		}
		
		/**
		 * @return il tempo di lavoro dai mese precedenti eventualmente comprensivo di quello derivante
		 * 	dall'anno precedente
		 */
		public int totaleResiduoAnnoCorrenteAlMesePrecedente() {
			//Deve esistere un mese precedente ed essere dello stesso anno (quindi a gennaio il mese precedente di questo anno non esiste)
			if (mesePrecedente != null && month != 1) {
				return mesePrecedente.totaleResiduoAnnoCorrenteAFineMese();
			}
			return 0;

		}

		public int residuoAnnoPrecedenteDisponibileAllInizioDelMese() {
			if (possibileUtilizzareResiduoAnnoPrecedente()) {
				if (month.equals(1)) {
					return residuoAnnoPrecedente;
				} 
				if (mesePrecedente == null) {
					return 0;
				}
				return mesePrecedente.residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			} else {
				return 0;
			}
		}
		
		public int residuoAnnoPrecedenteDisponibileAllaFineDelMese() {
			return residuoAnnoPrecedenteDisponibileAllInizioDelMese() + recuperiOreDaAnnoPrecedente + riposoCompensativiDaAnnoPrecedente;
		}
		
		public int totaleResiduoAnnoCorrenteAFineMese() {
			return residuoDelMese() + totaleResiduoAnnoCorrenteAlMesePrecedente() + riposiCompensativiDaAnnoCorrente - straordinari;  
		}
		
		public int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese() {
			return totaleResiduoAnnoCorrenteAFineMese() + residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		}
		
		public void aggiornaRiepiloghi() {
			int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			
			if (residuoDelMeseInNegativo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese > 0) {
				  
				if (residuoAnnoPrecedenteDisponibileAllaFineDelMese > -residuoDelMeseInNegativo) {
					recuperiOreDaAnnoPrecedente += residuoDelMeseInNegativo;
				} else {
					recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
				}
			}
			
			if (residuoDelMeseInPositivo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
				if (residuoDelMeseInPositivo > -residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
					recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
				} else {
					recuperiOreDaAnnoPrecedente += residuoDelMeseInPositivo;
				}
			}
		}
		
		public int tempoDisponibilePerStraordinariORecuperi(LocalDate date) {
			
			//Prendere il residuoInPositivo alla data indicata
			int residuoPositivoAllaDataRichiesta = residuoDelMeseInPositivo;
			
			if (residuoPositivoAllaDataRichiesta <= 0) {
				return 0;
			}
			
			int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese = totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese();
			
			if (totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese < 0) {
				return 0;
			}
			
			return Math.min(residuoPositivoAllaDataRichiesta, totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese);
						
		}
		
		public boolean prendiRiposoCompensativo(LocalDate date) {
			int minutiRiposoCompensativo = minutiRiposoCompensativo();
			
			if (minutiRiposoCompensativo > tempoDisponibilePerStraordinariORecuperi(date)) {
				return false;
			}
			int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
				throw new IllegalStateException(
					String.format("Richiesto riposo compensativo per l'utente %s nella data %s: ci sono ore disponibili " +
						"ma il residuo dell'anno scorso è negativo, questo non dovrebbe essere possibile, contattare Dario <dario.tagliaferri@iit.cnr.it>",
						person, date));
			}
			
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese == 0) {
				//Per esempio per i tecnici/amministrativi da aprile in poi
				riposiCompensativiDaAnnoCorrente = minutiRiposoCompensativo;
			} else {
				if (minutiRiposoCompensativo < residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
					riposoCompensativiDaAnnoPrecedente += minutiRiposoCompensativo;
				} else {
					riposoCompensativiDaAnnoPrecedente += residuoAnnoPrecedenteDisponibileAllaFineDelMese;
					riposiCompensativiDaAnnoCorrente += (minutiRiposoCompensativo + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
				}				
			}
			
			//Creare l'assenza etc....
			return true;
		}
		
		/**
		 * @return il valore (negativo) dei minuti a cui corrisponde un riposo compensativo
		 */
		public int minutiRiposoCompensativo() {
			//Cambia in funzione del tipo di orario di lavoro
			return -432;
		}
	}

	@Test
	public void lastYearPositiveResidualEndMarchPositive() {
		//int lastYearResidual = 2 * 60;
		PersonMonthUltimate jan = new PersonMonthUltimate(2013,1);
		jan.setResiduoAnnoPrecedente(600);
		jan.setStraordinari(120);
		jan.setResiduoDelMeseInNegativo(-60);
		jan.setResiduoDelMeseInPositivo(240);
		jan.setMesePrecedente(null);
				
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 60, jan.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 0, jan.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 660, jan.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		
//		PersonMonthUltimate feb = new PersonMonthUltimate(2013,2);
//		feb.setResiduoAnnoPrecedente(600);
//
//		PersonMonthUltimate mar = new PersonMonthUltimate(2013,3);
//		mar.setResiduoAnnoPrecedente(600);
//
//	
//		PersonMonthUltimate apr = new PersonMonthUltimate(2013,4);


	}

}
