import java.util.Arrays;
import java.util.Collection;

import junit.framework.Assert;

import lombok.Getter;
import lombok.Setter;
import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import com.mchange.util.AssertException;

/**
 * 
 */

/**
 * @author cristian
 *
 */
//@RunWith(Parameterized.class)
public class MonthResidualTest  {

	@Getter @Setter
	public static int MINUTE_IN_EXCESS_WHEN_REST = 500;
	
	private final static class PersonMonthNG extends PersonMonth {

		@Getter @Setter
		private int personLevel = 4;
		
		@Getter @Setter
		private int monthResidual = 0;
		
		@Getter
		private int compensatoryRest = 0;
		
		@Getter
		private Integer totalResidualEndOfMonth = null;
			
		@Getter @Setter
		private PersonMonthNG previousMonth = null;
		
		@Getter @Setter
		private int residualPastYear = 0;
		
		@Getter @Setter
		private int residualPastYearTaken = 0;
		
		@Getter @Setter
		private int compensatoryRestFromPastYearTaken = 0;
			
		@Getter @Setter
		private boolean built = false;


		
		public PersonMonthNG(int year, int month) {
			super(new Person(),year,month);
		}
		
	
		/**
		 * @return il tempo di lavoro dai mese precedenti eventualmente comprensivo di quello derivante
		 * 	dall'anno precedente
		 */
		public int getResidualFromLastMonths() {
			if (previousMonth != null) {
				return previousMonth.monthResidual + getResidualFromLastMonthsDerivedFromLastYear();
			}
			return getResidualFromLastMonthsDerivedFromLastYear();
		}

		/**
		 * @return il tempo di lavoro residuo dall'anno precedente ancora da utilizzare (se positivo) o
		 * 	da compensare (se negativo)
		 */

		public int getResidualFromLastMonthsDerivedFromLastYear() {
			if (!canUseResidualPastYear()) {
				return 0;
			}
			int residualAvailable = residualPastYear;
			PersonMonthNG currentPreviousMonth = previousMonth;
			while (currentPreviousMonth != null && previousMonth.year.equals(year)) {
				residualAvailable -= (currentPreviousMonth.residualPastYearTaken + currentPreviousMonth.compensatoryRestFromPastYearTaken);
				currentPreviousMonth = currentPreviousMonth.previousMonth;
			}
			return residualAvailable;
		}
		
		public boolean takeCompensatoryRest(LocalDate date) {
			int residualPastYearAvailable = getResidualPastYearAvailable();
//			System.out.println("residualPastYearAvailable = " + residualPastYearAvailable);
			
			//Il valore 432 in realtà dipende da dipendente a dipendente
			if (MINUTE_IN_EXCESS_WHEN_REST + residualPastYearAvailable < 432) {
				return false;
			}
			
			if (residualPastYearAvailable > 0) {
				if(residualPastYearAvailable > 432) {
					compensatoryRestFromPastYearTaken += 432;
				} else {
					compensatoryRestFromPastYearTaken += residualPastYearAvailable;
					compensatoryRest += (432 - residualPastYearAvailable);
				}
			} else {
				compensatoryRest += 432;
			}
			return true;
		}
		
		public boolean canUseResidualPastYear() {
			//Dipende dal livello.... e da
			return month <= 3 || personLevel <= 3;
		}
		
		public int getResidualPastYearAvailable() {
			return getResidualFromLastMonthsDerivedFromLastYear() - residualPastYearTaken - compensatoryRestFromPastYearTaken;
		}
		
		
		public void buildTotalsOfMonth() {
			int residualPastYearAvailable = getResidualPastYearAvailable();
			
			if (residualPastYearAvailable < 0) {
				if (monthResidual > 0) {
					if (monthResidual > -residualPastYearAvailable) {
						residualPastYearTaken -= residualPastYearAvailable;
					} else {
						residualPastYearTaken -= monthResidual;
					}
				}
			} else {
				if (monthResidual < 0 && canUseResidualPastYear()) {
//					System.out.println("residualPastYearAvailable = " + residualPastYearAvailable);
					if (residualPastYearAvailable > -monthResidual) {
						residualPastYearTaken += -monthResidual;
//						System.out.println("residualPastYearTaken = " + residualPastYearTaken);
						
					} else {
						residualPastYearTaken += residualPastYearAvailable;
					}
				}
				
			}

			
			if (residualPastYearAvailable >= 0) {
				
				System.out.println("residualPastYearAvailable >= 0, = " + residualPastYearAvailable);
				totalResidualEndOfMonth = monthResidual - compensatoryRest - compensatoryRestFromPastYearTaken + residualPastYearTaken;
				System.out.println("residualPastYearTaken = " + residualPastYearTaken);

			} else{
				System.out.println("residualPastYearAvailable < 0, = " + residualPastYearAvailable);
				System.out.println("monthResidual = " + monthResidual);
				totalResidualEndOfMonth = monthResidual + residualPastYearTaken + residualPastYearAvailable;
				System.out.println("1) totalResidualEndOfMonth = " + totalResidualEndOfMonth);
			}
			
			if (previousMonth != null && !month.equals(1)) {
				totalResidualEndOfMonth += previousMonth.getTotalResidualEndOfMonth();
			}
			
			if (month.equals(1)) {
				totalResidualEndOfMonth += (residualPastYear - residualPastYearTaken);
				System.out.println("2) totalResidualEndOfMonth = " + totalResidualEndOfMonth);
			}			
			
			if (!canUseResidualPastYear()) {
//				System.out.println("1) totalResidualEndOfMonth = " + totalResidualEndOfMonth);
				
				totalResidualEndOfMonth -= previousMonth != null ? previousMonth.getResidualPastYearAvailable() : 0;
				
//				System.out.println("2a) residualFromLastMonthsDerivedFromLastYear() = " + getResidualFromLastMonthsDerivedFromLastYear());
//				System.out.println("2b) totalResidualEndOfMonth = " + totalResidualEndOfMonth);
			}

		}
		

	}
	
	@Test
	public void lastYearPositiveResidualEndMarchPositive() {
		int lastYearResidual = 2 * 60;
		PersonMonthNG jan = new PersonMonthNG(2013,1);
		jan.setResidualPastYear(lastYearResidual);
		jan.setMonthResidual(-7 * 60);
		jan.buildTotalsOfMonth();

		Assert.assertEquals("residualPastYearPreviousMonth", lastYearResidual, jan.getResidualFromLastMonthsDerivedFromLastYear());
		Assert.assertEquals("monthResidual", -7 * 60, jan.getMonthResidual());
		
		Assert.assertEquals("compensatoryRest", 0, jan.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, jan.getCompensatoryRestFromPastYearTaken());
		Assert.assertTrue(jan.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 2 * 60, jan.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, jan.getResidualPastYearAvailable());		
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (-5 * 60), jan.getTotalResidualEndOfMonth());
		
		PersonMonthNG feb = new PersonMonthNG(2013,2);
		feb.setResidualPastYear(lastYearResidual);
		feb.previousMonth = jan;
		feb.setMonthResidual(6 * 60);
		feb.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 0, feb.getResidualFromLastMonthsDerivedFromLastYear());
		Assert.assertEquals("monthResidual", 6 * 60, feb.getMonthResidual());
		
		Assert.assertEquals("compensatoryRest", 0, feb.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, feb.getCompensatoryRestFromPastYearTaken());
		Assert.assertTrue(feb.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, feb.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, feb.getResidualPastYearAvailable());
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (1 * 60), feb.getTotalResidualEndOfMonth());

		PersonMonthNG mar = new PersonMonthNG(2013,3);
		mar.setResidualPastYear(lastYearResidual);
		mar.previousMonth = feb;
		mar.setMonthResidual(7 * 60);
		mar.takeCompensatoryRest(new LocalDate(2013,3,1));
		mar.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 0, mar.getResidualFromLastMonthsDerivedFromLastYear());
		Assert.assertEquals("monthResidual", 7 * 60, mar.getMonthResidual());
		
		Assert.assertEquals("compensatoryRest", 432, mar.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, mar.getCompensatoryRestFromPastYearTaken());
		Assert.assertTrue(mar.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, mar.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, mar.getResidualPastYearAvailable());
			
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) ((1 * 60) - 12), mar.getTotalResidualEndOfMonth());
	
		PersonMonthNG apr = new PersonMonthNG(2013,4);
		apr.setResidualPastYear(lastYearResidual);
		apr.setPreviousMonth(mar);
		apr.setMonthResidual(-2 * 60);
		apr.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 0, apr.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("compensatoryRest", 0, apr.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, apr.getCompensatoryRestFromPastYearTaken());
		Assert.assertFalse(apr.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, apr.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, apr.getResidualPastYearAvailable());
			
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (-72), apr.getTotalResidualEndOfMonth());
	}
	
	@Test
	public void lastYearPositiveResidualEndMarchPositiveCompensatoryRestLastYearTaken() {
		int lastYearResidual = 8 * 60;
		PersonMonthNG jan = new PersonMonthNG(2013,1);
		jan.setResidualPastYear(lastYearResidual);
		jan.setMonthResidual(-7 * 60);
		jan.buildTotalsOfMonth();

		Assert.assertEquals("residualPastYearPreviousMonth", lastYearResidual, jan.getResidualFromLastMonthsDerivedFromLastYear());
		Assert.assertEquals("monthResidual", -7 * 60, jan.getMonthResidual());
		
		Assert.assertEquals("compensatoryRest", 0, jan.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, jan.getCompensatoryRestFromPastYearTaken());
		Assert.assertTrue(jan.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 7 * 60, jan.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 60, jan.getResidualPastYearAvailable());		
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (1 * 60), jan.getTotalResidualEndOfMonth());
		
		PersonMonthNG feb = new PersonMonthNG(2013,2);
		feb.setResidualPastYear(lastYearResidual);
		feb.previousMonth = jan;
		feb.setMonthResidual(6 * 60);
		feb.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 60, feb.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, feb.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 60, feb.getResidualPastYearAvailable());
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (7 * 60), feb.getTotalResidualEndOfMonth());

		PersonMonthNG mar = new PersonMonthNG(2013,3);
		mar.setResidualPastYear(lastYearResidual);
		mar.previousMonth = feb;
		mar.setMonthResidual(7 * 60);
		mar.takeCompensatoryRest(new LocalDate(2013,3,1));
		mar.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 60, mar.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("compensatoryRest", 372, mar.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 60, mar.getCompensatoryRestFromPastYearTaken());
		
		Assert.assertTrue(mar.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, mar.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, mar.getResidualPastYearAvailable());
																	// = 408
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (420 + 420 - 432), mar.getTotalResidualEndOfMonth());
	
		PersonMonthNG apr = new PersonMonthNG(2013,4);
		apr.setResidualPastYear(lastYearResidual);
		apr.setPreviousMonth(mar);
		apr.setMonthResidual(-2 * 60);
		apr.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 0, apr.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("compensatoryRest", 0, apr.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, apr.getCompensatoryRestFromPastYearTaken());
		Assert.assertFalse(apr.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, apr.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, apr.getResidualPastYearAvailable());
			
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (288), apr.getTotalResidualEndOfMonth());
	}
	
	@Test
	public void lastYearPositiveResidualEndMarchPositiveCompensatoryResidualLastYearPositiveInApril() {
		int lastYearResidual = 18 * 60;
		PersonMonthNG jan = new PersonMonthNG(2013,1);
		jan.setResidualPastYear(lastYearResidual);
		jan.setMonthResidual(-7 * 60);
		jan.buildTotalsOfMonth();

		Assert.assertEquals("residualPastYearPreviousMonth", lastYearResidual, jan.getResidualFromLastMonthsDerivedFromLastYear());
			
		Assert.assertEquals("residualPastYearTaken", 7 * 60, jan.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 11 * 60, jan.getResidualPastYearAvailable());		
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (11 * 60), jan.getTotalResidualEndOfMonth());
		
		PersonMonthNG feb = new PersonMonthNG(2013,2);
		feb.setResidualPastYear(lastYearResidual);
		feb.previousMonth = jan;
		feb.setMonthResidual(6 * 60);
		feb.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 11 * 60, feb.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, feb.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 11 * 60, feb.getResidualPastYearAvailable());
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (17 * 60), feb.getTotalResidualEndOfMonth());

		PersonMonthNG mar = new PersonMonthNG(2013,3);
		mar.setResidualPastYear(lastYearResidual);
		mar.previousMonth = feb;
		mar.setMonthResidual(7 * 60);
		mar.takeCompensatoryRest(new LocalDate(2013,3,1));
		mar.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 11* 60, mar.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("compensatoryRest", 0, mar.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 432, mar.getCompensatoryRestFromPastYearTaken());
		
		Assert.assertTrue(mar.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, mar.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 228, mar.getResidualPastYearAvailable());

		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (1008), mar.getTotalResidualEndOfMonth());
	
		PersonMonthNG apr = new PersonMonthNG(2013,4);
		apr.setResidualPastYear(lastYearResidual);
		apr.setPreviousMonth(mar);
		apr.setMonthResidual(-2 * 60);
		apr.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", 0, apr.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("compensatoryRest", 0, apr.getCompensatoryRest());
		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, apr.getCompensatoryRestFromPastYearTaken());
		Assert.assertFalse(apr.canUseResidualPastYear());
		
		Assert.assertEquals("residualPastYearTaken", 0, apr.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", 0, apr.getResidualPastYearAvailable());
			
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (660), apr.getTotalResidualEndOfMonth());
	}
	
	@Test
	public void lastYearNegativeResidualEndMarchPositive() {
		int lastYearResidual = -10 * 60;
		PersonMonthNG jan = new PersonMonthNG(2013,1);
		jan.setResidualPastYear(lastYearResidual);
		jan.setMonthResidual(-5 * 60);
		jan.buildTotalsOfMonth();

		Assert.assertEquals("residualPastYearPreviousMonth", lastYearResidual, jan.getResidualFromLastMonthsDerivedFromLastYear());
			
		Assert.assertEquals("residualPastYearTaken", 0, jan.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", -10 * 60, jan.getResidualPastYearAvailable());		
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (-15 * 60), jan.getTotalResidualEndOfMonth());
		
		PersonMonthNG feb = new PersonMonthNG(2013,2);
		feb.setResidualPastYear(lastYearResidual);
		feb.previousMonth = jan;
		feb.setMonthResidual(6 * 60);
		feb.buildTotalsOfMonth();
		
		Assert.assertEquals("residualPastYearPreviousMonth", -10 * 60, feb.getResidualFromLastMonthsDerivedFromLastYear());
		
		Assert.assertEquals("residualPastYearTaken", -6 * 60, feb.residualPastYearTaken);
		Assert.assertEquals("residualPastYearAvailable", -4 * 60, feb.getResidualPastYearAvailable());
		
		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (-9 * 60), feb.getTotalResidualEndOfMonth());
//
//		PersonMonthNG mar = new PersonMonthNG(2013,3);
//		mar.setResidualPastYear(lastYearResidual);
//		mar.previousMonth = feb;
//		mar.setMonthResidual(7 * 60);
//		mar.takeCompensatoryRest(new LocalDate(2013,3,1));
//		mar.buildTotalsOfMonth();
//		
//		Assert.assertEquals("residualPastYearPreviousMonth", 11* 60, mar.getResidualFromLastMonthsDerivedFromLastYear());
//		
//		Assert.assertEquals("compensatoryRest", 0, mar.getCompensatoryRest());
//		Assert.assertEquals("compensatoryRestFromPastYearTaken", 432, mar.getCompensatoryRestFromPastYearTaken());
//		
//		Assert.assertTrue(mar.canUseResidualPastYear());
//		
//		Assert.assertEquals("residualPastYearTaken", 0, mar.residualPastYearTaken);
//		Assert.assertEquals("residualPastYearAvailable", 228, mar.getResidualPastYearAvailable());
//
//		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (1008), mar.getTotalResidualEndOfMonth());
//	
//		PersonMonthNG apr = new PersonMonthNG(2013,4);
//		apr.setResidualPastYear(lastYearResidual);
//		apr.setPreviousMonth(mar);
//		apr.setMonthResidual(-2 * 60);
//		apr.buildTotalsOfMonth();
//		
//		Assert.assertEquals("residualPastYearPreviousMonth", 0, apr.getResidualFromLastMonthsDerivedFromLastYear());
//		
//		Assert.assertEquals("compensatoryRest", 0, apr.getCompensatoryRest());
//		Assert.assertEquals("compensatoryRestFromPastYearTaken", 0, apr.getCompensatoryRestFromPastYearTaken());
//		Assert.assertFalse(apr.canUseResidualPastYear());
//		
//		Assert.assertEquals("residualPastYearTaken", 0, apr.residualPastYearTaken);
//		Assert.assertEquals("residualPastYearAvailable", 0, apr.getResidualPastYearAvailable());
//			
//		Assert.assertEquals("totalResidualEndOfMonth", (Integer) (660), apr.getTotalResidualEndOfMonth());
	}
}
