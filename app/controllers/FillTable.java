package controllers;

import play.*;
import play.db.jpa.JPA;
import play.mvc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.persistence.EntityManager;

import models.*;

/**
 * 
 * @author dario
 *
 */
public class FillTable extends Controller{

	/**
	 * @param args
	 */
	protected static String mySqldriver = "com.mysql.jdbc.Driver";	
	
	public static void riempiTabelle() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		Connection mysqlconn = null;
		PreparedStatement stmt;
		
		try{
			Class.forName(mySqldriver).newInstance();
			mysqlconn = DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT","root", "orologio");
			
			stmt = mysqlconn.prepareStatement("SELECT * FROM orari_di_lavoro ");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			WorkingTimeType wtt = null;
						
			while(rs.next()){
				
				wtt = new WorkingTimeType();
				
				wtt.description = rs.getString("nome");
				
				wtt.mondayWorkingTime = rs.getInt("lu_tempo_lavoro");
				wtt.mondayHoliday = rs.getByte("lu_festa");
				wtt.mondayTimeSlotEntranceFrom = rs.getInt("lu_fascia_ingresso");
				wtt.mondayTimeSlotEntranceTo = rs.getInt("lu_fascia_ingresso1");
				wtt.mondayTimeSlotExitFrom = rs.getInt("lu_fascia_uscita");
				wtt.mondayTimeSlotExitTo = rs.getInt("lu_fascia_uscita1");
				wtt.mondayTimeMealFrom = rs.getInt("lu_fascia_pranzo");
				wtt.mondayTimeMealTo = rs.getInt("lu_fascia_pranzo1");
				wtt.mondayBreakTicketTime = rs.getString("Gruppo"); //capire quale campo è
				wtt.mondayMealTicketTime = rs.getInt("Valore"); // capire quale campo è
				wtt.tuesdayWorkingTime = rs.getInt("ma_tempo_lavoro");
				wtt.tuesdayHoliday = rs.getByte("ma_festa");
				wtt.tuesdayTimeSlotEntranceFrom = rs.getInt("ma_fascia_ingresso");
				wtt.tuesdayTimeSlotEntranceTo = rs.getInt("ma_fascia_ingresso1");
				wtt.tuesdayTimeSlotExitFrom = rs.getInt("ma_fascia_uscita");
				wtt.tuesdayTimeSlotExitTo = rs.getInt("ma_fascia_uscita1");
				wtt.tuesdayTimeMealFrom = rs.getInt("ma_fascia_pranzo");
				wtt.tuesdayTimeMealTo = rs.getInt("ma_fascia_pranzo1");
				wtt.tuesdayBreakTicketTime = rs.getString("CodiceSost"); //capire quale campo è
				wtt.tuesdayMealTicketTime = rs.getByte("IgnoraTimbr"); //capire quale campo è
				wtt.wednesdayWorkingTime = rs.getInt("me_tempo_lavoro");
				wtt.wednesdayHoliday = rs.getByte("me_festa");
				wtt.wednesdayTimeSlotEntranceFrom = rs.getInt("me_fascia_ingresso");
				wtt.wednesdayTimeSlotEntranceTo = rs.getInt("me_fascia_ingresso1");
				wtt.wednesdayTimeSlotExitFrom = rs.getInt("me_fascia_uscita");
				wtt.wednesdayTimeSlotExitTo = rs.getInt("me_fascia_uscita1");
				wtt.wednesdayTimeMealFrom = rs.getInt("me_fascia_pranzo");
				wtt.wednesdayTimeMealTo = rs.getInt("me_fascia_pranzo1");
				wtt.wednesdayBreakTicketTime = rs.getInt(""); //capire quale campo è
				wtt.wednesdayMealTicketTime = rs.getInt(""); //capire quale campo è
				wtt.thursdayWorkingTime = rs.getInt("gi_tempo_lavoro");
				wtt.thursdayHoliday = rs.getByte("gi_festa");
				wtt.thursdayTimeSlotEntranceFrom = rs.getInt("gi_fascia_ingresso");
				wtt.thursdayTimeSlotEntranceTo = rs.getInt("gi_fascia_ingresso1");
				wtt.thursdayTimeSlotExitFrom = rs.getInt("gi_fascia_uscita");
				wtt.thursdayTimeSlotExitTo = rs.getInt("gi_fascia_uscita1");
				wtt.thursdayTimeMealFrom = rs.getInt("gi_fascia_pranzo");
				wtt.thursdayTimeMealTo = rs.getInt("gi_fascia_pranzo1");
				wtt.thursdayBreakTicketTime = rs.getInt(""); //capire quale campo è
				wtt.thursdayMealTicketTime = rs.getInt(""); //capire quale campo è
				wtt.fridayWorkingTime = rs.getInt("ve_tempo_lavoro");
				wtt.fridayHoliday = rs.getByte("ve_festa");
				wtt.fridayTimeSlotEntranceFrom = rs.getInt("ve_fascia_ingresso");
				wtt.fridayTimeSlotEntranceTo = rs.getInt("ve_fascia_ingresso1");
				wtt.fridayTimeSlotExitFrom = rs.getInt("ve_fascia_uscita");
				wtt.fridayTimeSlotExitTo = rs.getInt("ve_fascia_uscita1");
				wtt.fridayTimeMealFrom = rs.getInt("ve_fascia_pranzo");
				wtt.fridayTimeMealTo = rs.getInt("ve_fascia_pranzo1");
				wtt.fridayBreakTicketTime = rs.getInt(""); //capire quale campo è
				wtt.fridayMealTicketTime = rs.getInt(""); //capire quale campo è
				wtt.saturdayWorkingTime = rs.getInt("sa_tempo_lavoro");
				wtt.saturdayHoliday = rs.getByte("sa_festa");
				wtt.saturdayTimeSlotEntranceFrom = rs.getInt("sa_fascia_ingresso");
				wtt.saturdayTimeSlotEntranceTo = rs.getInt("sa_fascia_ingresso1");
				wtt.saturdayTimeSlotExitFrom = rs.getInt("sa_fascia_uscita");
				wtt.saturdayTimeSlotExitTo = rs.getInt("sa_fascia_uscita1");
				wtt.saturdayTimeMealFrom = rs.getInt("sa_fascia_pranzo");
				wtt.saturdayTimeMealTo = rs.getInt("sa_fascia_pranzo1");
				wtt.saturdayBreakTicketTime = rs.getInt(""); //capire quale campo è
				wtt.saturdayMealTicketTime = rs.getInt(""); //capire quale campo è
				wtt.sundayWorkingTime = rs.getInt("do_tempo_lavoro");
				wtt.sundayHoliday = rs.getByte("do_festa");
				wtt.sundayTimeSlotEntranceFrom = rs.getInt("do_fascia_ingresso");
				wtt.sundayTimeSlotEntranceTo = rs.getInt("do_fascia_ingresso1");
				wtt.sundayTimeSlotExitFrom = rs.getInt("do_fascia_uscita");
				wtt.sundayTimeSlotExitTo = rs.getInt("do_fascia_uscita1");
				wtt.sundayTimeMealFrom = rs.getInt("do_fascia_pranzo");
				wtt.sundayTimeMealTo = rs.getInt("do_fascia_pranzo1");
				wtt.sundayBreakTicketTime = rs.getInt(""); //capire quale campo è
				wtt.sundayMealTicketTime = rs.getInt(""); //capire quale campo è
				
				wtt._save();	
				
			}
			em.persist(wtt);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}		
		try{
			stmt = mysqlconn.prepareStatement("SELECT * FROM ferie");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			VacationType vt = null;
			while(rs.next()){
				vt = new VacationType();
				vt.description = rs.getString("nome");
				vt.vacationDays = rs.getInt("giorni_ferie");
				vt.permissionDays = rs.getInt("giorni_pl");
				vt._save();
			}
			em.persist(vt);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			stmt = mysqlconn.prepareStatement("SELECT * FROM ferie_pers");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			PersonVacation pv = null;
			while(rs.next()){
				pv = new PersonVacation();
				pv.fid = rs.getByte("fid");
				pv.pid = rs.getShort("pid");
				pv.beginFrom = rs.getDate("data_inizio");
				pv.endTo = rs.getDate("data_fine");
				pv._save();
			}
			em.persist(pv);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		try{
						
			stmt = mysqlconn.prepareStatement("SELECT * FROM assenze");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			AbsenceType assenze = null;
			
			while(rs.next()){
				assenze = new AbsenceType();
				
				assenze.code = rs.getString("Codice");			
				assenze.certificateCode = rs.getString("Codice_att");
				assenze.description = rs.getString("Descrizione");
				assenze.validFrom = rs.getDate("DataInizio");
				assenze.validTo = rs.getDate("DataFine");
		//		assenze.g1 = rs.getByte("g1");
		//		assenze.g2 = rs.getByte("g2");
				
				assenze._save();
				
			}
			em.persist(assenze);
			
			//mysqlconn.commit();
			//mysqlconn.close();	
			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		try{
			stmt = mysqlconn.prepareStatement("SELECT * FROM Persone");
			ResultSet rs = stmt.executeQuery();
			
			EntityManager em = JPA.em();
			Location locazione = null;
			
			while(rs.next()){
				locazione = new Location();
				locazione.department = rs.getString("Dipartimento");
				locazione.headOffice = rs.getNString("Sede");
				locazione.room = rs.getString("Stanza");
				
				locazione._save();
			}
			em.persist(locazione);
			mysqlconn.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}
       
    }

}
