import static org.junit.Assert.*;
import junit.framework.Assert;

import org.joda.time.LocalDate;
import org.junit.Test;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.PersonMonth;

/**
 * 
 * @author dario
 *
 */
@Slf4j
public class UltimateMonthResidualTest {

	private static final class PersonMonthUltimate extends PersonMonth {

		@Getter @Setter
		private int personLevel = 2;

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
		
		@Getter
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
		
		public int residuoDelMeseAllaData(LocalDate date) {
			//FIXME: da calcolare in funzione della data
			//residui in positivo alla data + residui in negativo alla data, ovvero il "progressivo"
			return residuoDelMese();
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
			} 
			
			return 0;
			
		}
		
		public int residuoAnnoPrecedenteDisponibileAllaFineDelMese() {
			int residuoAnnoPrecedenteDisponibileAllInizioDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese();
			System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllInizioDelMese = " + residuoAnnoPrecedenteDisponibileAllInizioDelMese);
			
			int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese + recuperiOreDaAnnoPrecedente + riposoCompensativiDaAnnoPrecedente;
			
			System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllaFineDelMese() = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
			return residuoAnnoPrecedenteDisponibileAllaFineDelMese;
		}
	
		public int totaleResiduoAnnoCorrenteAFineMese() {
			return residuoDelMese() + totaleResiduoAnnoCorrenteAlMesePrecedente() + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
		}
		
		public int totaleResiduoAnnoCorrenteAllaData(LocalDate date) {
			return residuoDelMeseAllaData(date) + totaleResiduoAnnoCorrenteAlMesePrecedente() + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
		}
		
		public int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese() {
			return totaleResiduoAnnoCorrenteAFineMese() + residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		}
		
		public void aggiornaRiepiloghi() {
			log.debug("Aggiornamento dei riepiloghi del mese {}", month);
			
			int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			
			if (residuoDelMeseInNegativo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese > 0) {
				
				 log.debug("mese = {}. Residuo del mese in negativo ({}) != 0 e residuoAnnoPrecedenteDisponibileAllaFineDelMese ({}) > 0, recupero dall'anno scorso il recuperabile",
						new Object[] { month , residuoDelMeseInNegativo, residuoAnnoPrecedenteDisponibileAllaFineDelMese });
				 
				if (residuoAnnoPrecedenteDisponibileAllaFineDelMese > -residuoDelMeseInNegativo) {
					log.debug("mese = {}. residuoAnnoPrecedenteDisponibileAllaFineDelMese > del residuo del mese in negativo, aumento i recuperiOreDaAnnoPrecedente (adesso {}) di {} minuti",
						new Object[] { month, recuperiOreDaAnnoPrecedente, residuoDelMeseInNegativo});
					
					recuperiOreDaAnnoPrecedente += residuoDelMeseInNegativo;
					
					log.debug("mese = {}. recuperiOreDaAnnoPrecedente = {} minuti",
							new Object[] { month, recuperiOreDaAnnoPrecedente });
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
		
		public int tempoDisponibilePerRecuperi(LocalDate date) {
			int totaleResiduoAnnoCorrenteAllaData = totaleResiduoAnnoCorrenteAllaData(date);
			
			System.out.println("totaleResiduoAnnoCorrenteAllaData = " + totaleResiduoAnnoCorrenteAllaData);
			
			int tempoDisponibile = totaleResiduoAnnoCorrenteAllaData + residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			
			if (tempoDisponibile <= 0) {
				tempoDisponibile = 0;
			}
			System.out.println("Data = " + date + ". Tempo disponibile per recuperi = " + tempoDisponibile);
			return tempoDisponibile;
			
		}
		
		public int tempoDisponibilePerStraordinari() {
					
			if (residuoDelMeseInPositivo <= 0) {
				return 0;
			}
			
			int residuoAllaDataRichiesta = residuoDelMese();
			
			int tempoDisponibile = residuoAnnoPrecedenteDisponibileAllaFineDelMese() + mesePrecedente.totaleResiduoAnnoCorrenteAFineMese() + residuoAllaDataRichiesta;
			
			if (tempoDisponibile <= 0) {
				return 0;
			}
			
			return Math.min(residuoDelMeseInPositivo, tempoDisponibile);
						
		}
		
		public boolean assegnaStraordinari(int ore) {
			if (tempoDisponibilePerStraordinari() > ore * 60) {
				straordinari = ore * 60;
				return true;
			}
			return false;
		}
		
		public boolean prendiRiposoCompensativo(LocalDate date) {
			int minutiRiposoCompensativo = minutiRiposoCompensativo();
			
			if (-minutiRiposoCompensativo > tempoDisponibilePerRecuperi(date)) {
				return false;
			}
			
			int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
			
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
				throw new IllegalStateException(
					String.format("Richiesto riposo compensativo per l'utente %s nella data %s: ci sono ore disponibili " +
						"ma il residuo dell'anno scorso è negativo, questo non dovrebbe essere possibile, contattare Dario <dario.tagliaferri@iit.cnr.it>",
						person, date));
			}
			
			System.out.println("residuoAnnoPrecedenteDisponibileAllaFineDelMese = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese == 0) {
				//Per esempio per i tecnici/amministrativi da aprile in poi
				riposiCompensativiDaAnnoCorrente += minutiRiposoCompensativo;
			} else {
				if (minutiRiposoCompensativo < residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
					riposoCompensativiDaAnnoPrecedente += minutiRiposoCompensativo;
				} else {
					riposoCompensativiDaAnnoPrecedente += residuoAnnoPrecedenteDisponibileAllaFineDelMese;
					riposiCompensativiDaAnnoCorrente += (minutiRiposoCompensativo + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
				}				
			}
			
			//Creare l'assenza etc....
			aggiornaRiepiloghi();
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
	public void lastYearPositiveResidualEndMarchPositiveTechnician() {
		//int lastYearResidual = 2 * 60;
		PersonMonthUltimate jan = new PersonMonthUltimate(2013,1);
		jan.setResiduoAnnoPrecedente(1528);
		jan.setResiduoDelMeseInNegativo(-760);
		jan.setResiduoDelMeseInPositivo(591);
		jan.setMesePrecedente(null);
		
		jan.aggiornaRiepiloghi();
		
		Assert.assertEquals("Residuo del mese", -169, jan.residuoDelMese());
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 591, jan.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 0, jan.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 1359, jan.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 768, jan.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
		
		
		PersonMonthUltimate feb = new PersonMonthUltimate(2013,2);
		feb.setMesePrecedente(jan);
		feb.setResiduoAnnoPrecedente(1528);
		
		feb.setResiduoDelMeseInNegativo(-389);
		feb.setResiduoDelMeseInPositivo(614);

		feb.aggiornaRiepiloghi();
		
		Assert.assertEquals("Residuo del mese", 225, feb.residuoDelMese());
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 1205, feb.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 591, feb.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 1584, feb.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 379, feb.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
		

		PersonMonthUltimate mar = new PersonMonthUltimate(2013,3);
		mar.setMesePrecedente(feb);
		mar.setResiduoAnnoPrecedente(1528);
		mar.setResiduoDelMeseInNegativo(-539);
		mar.setResiduoDelMeseInPositivo(731);
		
		mar.aggiornaRiepiloghi();
		
		Assert.assertTrue(mar.prendiRiposoCompensativo(new LocalDate(2012,3,19)));
		
		Assert.assertEquals("Riposi compensativi da anno precedente", 0, mar.riposoCompensativiDaAnnoPrecedente);
		Assert.assertEquals("Riposi compensativi da anno corrente", -432, mar.riposiCompensativiDaAnnoCorrente);
		
		Assert.assertTrue(mar.prendiRiposoCompensativo(new LocalDate(2012,3,28)));
		
		Assert.assertEquals("Riposi compensativi da anno precedente", 0, mar.riposoCompensativiDaAnnoPrecedente);
		Assert.assertEquals("Riposi compensativi da anno corrente", -864, mar.riposiCompensativiDaAnnoCorrente);
		
		Assert.assertEquals("Residuo del mese", 192, mar.residuoDelMese());
		
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 912, mar.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 1205, mar.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 912, mar.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 0, mar.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
		
		PersonMonthUltimate apr = new PersonMonthUltimate(2013,4);
		apr.setMesePrecedente(mar);
		apr.setResiduoAnnoPrecedente(1528);
		apr.setResiduoDelMeseInNegativo(-115);
		apr.setResiduoDelMeseInPositivo(685);

		apr.aggiornaRiepiloghi();

		Assert.assertEquals("Residuo del mese", 570, apr.residuoDelMese());
		
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 1482, apr.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 912, apr.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 1482, apr.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 0, apr.residuoAnnoPrecedenteDisponibileAllaFineDelMese());

		//Assegnazione straordinari
		Assert.assertTrue("Straordinari non assegnati, invece dovevano essere assegnati tutti", apr.assegnaStraordinari(5));

		Assert.assertEquals("Totale residuo anno corrente a fine mese", 1182, apr.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 1182, apr.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Straordinari assegnati", 300, apr.getStraordinari());

	}
	
	@Test
	public void lastYearPositiveResidualEndMarchPositiveTechnologist() {
		//int lastYearResidual = 2 * 60;
		PersonMonthUltimate jan = new PersonMonthUltimate(2013,1);
		jan.setResiduoAnnoPrecedente(26909);
		jan.setResiduoDelMeseInNegativo(0);
		jan.setResiduoDelMeseInPositivo(1097);
		jan.setMesePrecedente(null);
		
		jan.aggiornaRiepiloghi();
		
		Assert.assertEquals("Residuo del mese", 1097, jan.residuoDelMese());
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 1097, jan.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 0, jan.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 28006, jan.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 26909, jan.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
		
		
		PersonMonthUltimate feb = new PersonMonthUltimate(2013,2);
		feb.setMesePrecedente(jan);
		feb.setResiduoAnnoPrecedente(26909);
		
		feb.setResiduoDelMeseInNegativo(0);
		feb.setResiduoDelMeseInPositivo(704);

		feb.aggiornaRiepiloghi();
		
		Assert.assertEquals("Residuo del mese", 704, feb.residuoDelMese());
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 1801, feb.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 1097, feb.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 28710, feb.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 26909, feb.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
		

		PersonMonthUltimate mar = new PersonMonthUltimate(2013,3);
		mar.setMesePrecedente(feb);
		mar.setResiduoAnnoPrecedente(26909);
		mar.setResiduoDelMeseInNegativo(0);
		mar.setResiduoDelMeseInPositivo(701);
		
		mar.aggiornaRiepiloghi();
		
	//	Assert.assertTrue(mar.prendiRiposoCompensativo(new LocalDate(2012,3,19)));
		
	//	Assert.assertEquals("Riposi compensativi da anno precedente", 0, mar.riposoCompensativiDaAnnoPrecedente);
	//	Assert.assertEquals("Riposi compensativi da anno corrente", -432, mar.riposiCompensativiDaAnnoCorrente);
		
	//	Assert.assertTrue(mar.prendiRiposoCompensativo(new LocalDate(2012,3,28)));
		
	//	Assert.assertEquals("Riposi compensativi da anno precedente", 0, mar.riposoCompensativiDaAnnoPrecedente);
	//	Assert.assertEquals("Riposi compensativi da anno corrente", -864, mar.riposiCompensativiDaAnnoCorrente);
		
		Assert.assertEquals("Residuo del mese", 701, mar.residuoDelMese());
		
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 2502, mar.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 1801, mar.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 29411, mar.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 26909, mar.residuoAnnoPrecedenteDisponibileAllaFineDelMese());
//		
		PersonMonthUltimate apr = new PersonMonthUltimate(2013,4);
		apr.setMesePrecedente(mar);
		apr.setResiduoAnnoPrecedente(26909);
		apr.setResiduoDelMeseInNegativo(0);
		apr.setResiduoDelMeseInPositivo(659);

		apr.aggiornaRiepiloghi();

		Assert.assertEquals("Residuo del mese", 659, apr.residuoDelMese());
		
		Assert.assertEquals("Totale residuo anno corrente a fine mese", 3161, apr.totaleResiduoAnnoCorrenteAFineMese());
		Assert.assertEquals("Totale residuo anno corrente al mese precedente", 2502, apr.totaleResiduoAnnoCorrenteAlMesePrecedente());
		Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 30070, apr.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		Assert.assertEquals("Residuo anno precedente disponibile alla fine del mese", 26909, apr.residuoAnnoPrecedenteDisponibileAllaFineDelMese());

		//Assegnazione straordinari
		//Assert.assertTrue("Straordinari non assegnati, invece dovevano essere assegnati tutti", apr.assegnaStraordinari(5));

	//	Assert.assertEquals("Totale residuo anno corrente a fine mese", 1182, apr.totaleResiduoAnnoCorrenteAFineMese());
	//	Assert.assertEquals("Totale residuo anno corrente a fine mese più residuo anno precedente", 1182, apr.totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese());
		
		//Assert.assertEquals("Straordinari assegnati", 300, apr.getStraordinari());

	}
	

}
