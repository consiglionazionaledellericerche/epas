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
import models.WorkingTimeTypeDay.DayOfWeek;

/**
 * 
 * @author dario
 *
 */
public class FillTable extends Controller{

	/**
	 * @param args
	 */
	public static String mySqldriver = "com.mysql.jdbc.Driver";	

	private static Connection mysqlCon = null;


	/**
	 * @return un singleton per la connessione Mysql
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private static Connection getMysqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (mysqlCon != null ) {
			return mysqlCon;
		}
		Class.forName(mySqldriver).newInstance();

		return DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT?zeroDateTimeBehavior=convertToNull","root", "orologio");
	}


	public static void fillWorkingTime() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{			

			stmt = mysqlCon.prepareStatement("SELECT * FROM orari_di_lavoro ");

			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			WorkingTimeType wtt = null;


			while(rs.next()){

				wtt = new WorkingTimeType();

				wtt.description = rs.getString("nome");

				wtt.shift = rs.getBoolean("turno");

				WorkingTimeTypeDay wttd_mo = new WorkingTimeTypeDay();
				wttd_mo.workingTime = rs.getInt("lu_tempo_lavoro");
				wttd_mo.holiday = rs.getBoolean("lu_festa");

				wttd_mo.timeSlotEntranceFrom = rs.getInt("lu_fascia_ingresso");
				wttd_mo.timeSlotEntranceTo = rs.getInt("lu_fascia_ingresso1");
				wttd_mo.timeSlotExitFrom = rs.getInt("lu_fascia_uscita");
				wttd_mo.timeSlotExitTo = rs.getInt("lu_fascia_uscita1");
				wttd_mo.timeMealFrom = rs.getInt("lu_fascia_pranzo");
				wttd_mo.timeMealTo = rs.getInt("lu_fascia_pranzo1");
				wttd_mo.breakTicketTime = rs.getInt("lu_tempo_interv"); 
				wttd_mo.mealTicketTime = rs.getInt("lu_tempo_buono");

				WorkingTimeTypeDay wttd_tu = new WorkingTimeTypeDay();
				wttd_tu.workingTime = rs.getInt("ma_tempo_lavoro");
				wttd_tu.holiday = rs.getBoolean("ma_festa");
				wttd_tu.timeSlotEntranceFrom = rs.getInt("ma_fascia_ingresso");
				wttd_tu.timeSlotEntranceTo = rs.getInt("ma_fascia_ingresso1");
				wttd_tu.timeSlotExitFrom = rs.getInt("ma_fascia_uscita");
				wttd_tu.timeSlotExitTo = rs.getInt("ma_fascia_uscita1");
				wttd_tu.timeMealFrom = rs.getInt("ma_fascia_pranzo");
				wttd_tu.timeMealTo = rs.getInt("ma_fascia_pranzo1");
				wttd_tu.breakTicketTime = rs.getInt("ma_tempo_interv"); 
				wttd_tu.mealTicketTime = rs.getByte("ma_tempo_buono"); 

				WorkingTimeTypeDay wttd_we = new WorkingTimeTypeDay();
				wttd_we.workingTime = rs.getInt("me_tempo_lavoro");
				wttd_we.holiday = rs.getBoolean("me_festa");
				wttd_we.timeSlotEntranceFrom = rs.getInt("me_fascia_ingresso");
				wttd_we.timeSlotEntranceTo = rs.getInt("me_fascia_ingresso1");
				wttd_we.timeSlotExitFrom = rs.getInt("me_fascia_uscita");
				wttd_we.timeSlotExitTo = rs.getInt("me_fascia_uscita1");
				wttd_we.timeMealFrom = rs.getInt("me_fascia_pranzo");
				wttd_we.timeMealTo = rs.getInt("me_fascia_pranzo1");
				wttd_we.breakTicketTime = rs.getInt("me_tempo_interv"); 
				wttd_we.mealTicketTime = rs.getInt("me_tempo_buono"); 

				WorkingTimeTypeDay wttd_th = new WorkingTimeTypeDay();
				wttd_th.workingTime = rs.getInt("gi_tempo_lavoro");
				wttd_th.holiday = rs.getBoolean("gi_festa");
				wttd_th.timeSlotEntranceFrom = rs.getInt("gi_fascia_ingresso");
				wttd_th.timeSlotEntranceTo = rs.getInt("gi_fascia_ingresso1");
				wttd_th.timeSlotExitFrom = rs.getInt("gi_fascia_uscita");
				wttd_th.timeSlotExitTo = rs.getInt("gi_fascia_uscita1");
				wttd_th.timeMealFrom = rs.getInt("gi_fascia_pranzo");
				wttd_th.timeMealTo = rs.getInt("gi_fascia_pranzo1");
				wttd_th.breakTicketTime = rs.getInt("me_tempo_interv"); 
				wttd_th.mealTicketTime = rs.getInt("me_tempo_buono"); 

				WorkingTimeTypeDay wttd_fr = new WorkingTimeTypeDay();
				wttd_fr.workingTime = rs.getInt("ve_tempo_lavoro");
				wttd_fr.holiday = rs.getBoolean("ve_festa");
				wttd_fr.timeSlotEntranceFrom = rs.getInt("ve_fascia_ingresso");
				wttd_fr.timeSlotEntranceTo = rs.getInt("ve_fascia_ingresso1");
				wttd_fr.timeSlotExitFrom = rs.getInt("ve_fascia_uscita");
				wttd_fr.timeSlotExitTo = rs.getInt("ve_fascia_uscita1");
				wttd_fr.timeMealFrom = rs.getInt("ve_fascia_pranzo");
				wttd_fr.timeMealTo = rs.getInt("ve_fascia_pranzo1");
				wttd_fr.breakTicketTime = rs.getInt("me_tempo_interv"); 
				wttd_fr.mealTicketTime = rs.getInt("me_tempo_buono"); 

				WorkingTimeTypeDay wttd_sa = new WorkingTimeTypeDay();
				wttd_sa.workingTime = rs.getInt("sa_tempo_lavoro");
				wttd_sa.holiday = rs.getBoolean("sa_festa");
				wttd_sa.timeSlotEntranceFrom = rs.getInt("sa_fascia_ingresso");
				wttd_sa.timeSlotEntranceTo = rs.getInt("sa_fascia_ingresso1");
				wttd_sa.timeSlotExitFrom = rs.getInt("sa_fascia_uscita");
				wttd_sa.timeSlotExitTo = rs.getInt("sa_fascia_uscita1");
				wttd_sa.timeMealFrom = rs.getInt("sa_fascia_pranzo");
				wttd_sa.timeMealTo = rs.getInt("sa_fascia_pranzo1");
				wttd_sa.breakTicketTime = rs.getInt("me_tempo_interv");
				wttd_sa.mealTicketTime = rs.getInt("me_tempo_buono"); 

				WorkingTimeTypeDay wttd_su = new WorkingTimeTypeDay();				
				wttd_su.workingTime = rs.getInt("do_tempo_lavoro");
				wttd_su.holiday = rs.getBoolean("do_festa");
				wttd_su.timeSlotEntranceFrom = rs.getInt("do_fascia_ingresso");
				wttd_su.timeSlotEntranceTo = rs.getInt("do_fascia_ingresso1");
				wttd_su.timeSlotExitFrom = rs.getInt("do_fascia_uscita");
				wttd_su.timeSlotExitTo = rs.getInt("do_fascia_uscita1");
				wttd_su.timeMealFrom = rs.getInt("do_fascia_pranzo");
				wttd_su.timeMealTo = rs.getInt("do_fascia_pranzo1");
				wttd_su.breakTicketTime = rs.getInt("me_tempo_interv");
				wttd_su.mealTicketTime = rs.getInt("me_tempo_buono");

				wtt._save();	

			}
			em.persist(wtt);

		}
		catch(Exception e){
			e.printStackTrace();
		}
	} 

	public static void fillVacationType() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT * FROM ferie");
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
	}
	public static void fillPerson() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT Nome, Cognome, DataNascita, Matricola, Qualifica FROM Persone");
			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			Person person = null;
			while(rs.next()){
				person = new Person();
				person.name = rs.getString("Nome");
				person.surname = rs.getString("Cognome");
				person.bornDate = rs.getDate("DataNascita");
				//person.contractLevel = rs.getInteger("Qualifica");
				person.number = rs.getInt("Matricola");
				person._save();
			}
			em.persist(person);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void fillPersonVacation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT * FROM ferie_pers");
			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			PersonVacation pv = null;
			while(rs.next()){
				pv = new PersonVacation();
				pv.beginFrom = rs.getDate("data_inizio");
				pv.endTo = rs.getDate("data_fine");
				pv._save();
			}
			em.persist(pv);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void fillYearRecap() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT * FROM totali_anno");
			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			YearRecap recap = null;

			while(rs.next()){
				recap = new YearRecap();
				recap.year = rs.getShort("anno");
				recap.remaining = rs.getInt("residuo");
				recap.remainingAp = rs.getInt("residuoap");
				recap.recg = rs.getInt("recg");
				recap.recgap = rs.getInt("recgap");
				recap.overtime = rs.getInt("straord");
				recap.overtimeAp = rs.getInt("straordap");
				recap.recguap = rs.getInt("recguap");
				recap.recm = rs.getInt("recm");
				recap.lastModified = rs.getTimestamp("data_ultimamod");

				recap._save();
			}
			em.persist(recap);


		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void fillMonthRecap() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT * FROM totali_mens limit 200");
			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			MonthRecap recap = null;

			while(rs.next()){
				recap = new MonthRecap();
				recap.month = rs.getShort("mese");
				recap.year = rs.getShort("anno");
				recap.workingDays = rs.getShort("giorni_lavorativi");
				recap.daysWorked = rs.getShort("giorni_lavorati");
				recap.giorniLavorativiLav = rs.getShort("giorni_lavorativi");
				recap.workTime = rs.getInt("tempo_lavorato");
				recap.remaining = rs.getInt("residuo");
				recap.justifiedAbsence = rs.getShort("assenze_giust");
				recap.vacationAp = rs.getShort("ferie_ap");
				recap.vacationAc = rs.getShort("ferie_ac");
				recap.holidaySop = rs.getShort("festiv_sop");
				recap.recoveries = rs.getInt("recuperi");
				recap.recoveriesAp = rs.getShort("recuperiap");
				recap.recoveriesG = rs.getShort("recuperig");
				recap.recoveriesGap = rs.getShort("recuperigap");
				recap.overtime = rs.getInt("ore_str");
				recap.lastModified = rs.getTimestamp("data_ultimamod");
				recap.residualApUsed = rs.getInt("residuoap_usato");
				recap.extraTimeAdmin = rs.getInt("tempo_eccesso_ammin");
				recap.additionalHours = rs.getInt("ore_aggiuntive");
				recap.nadditionalHours = rs.getByte("nore_aggiuntive");
				recap.residualFine = rs.getInt("residuo_fine");
				recap.beginWork = rs.getByte("inizio_lavoro");
				recap.endWork = rs.getByte("fine_lavoro");
				recap.timeHourVisit = rs.getInt("tempo_visite_orarie");
				recap.endRecoveries = rs.getShort("recuperi_fine");
				recap.negative = rs.getInt("negativo");
				recap.endNegative = rs.getInt("negativo_fine");
				recap.progressive = rs.getString("progressivo");				

				recap._save();
			}
			em.persist(recap);


		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void fillContactAndLocation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();

		PreparedStatement stmt;
		try{
			stmt = mysqlCon.prepareStatement("SELECT Dipartimento, Sede, Stanza, Email, Fax, Telefono FROM Persone");
			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			Location locazione = null;
			ContactData cd = null;

			while(rs.next()){
				locazione = new Location();
				cd = new ContactData();
				locazione.department = rs.getString("Dipartimento");
				locazione.headOffice = rs.getString("Sede");
				locazione.room = rs.getString("Stanza");
				cd.email = rs.getString("Email");
				cd.fax = rs.getString("Fax");
				// TODO: Provare la valdazione del nuovo campo phone come 
				// "+39.0" +  rs.getString("Telefono"); se va a buon fine impostare questa
				// altrimenti null e log con warning
				// Fare qualche euristica per estrarre i telefoni correnti
				cd.telephone = rs.getString("Telefono");
				if(cd.telephone != null){
					if(cd.telephone.length() == 4){
						cd.telephone = "+39050315" + cd.telephone;
					}
					if((cd.telephone.startsWith("3"))&&(cd.telephone.length() > 4)){
						cd.mobile = cd.telephone;
						cd.telephone = null;
					}
					if(cd.telephone.startsWith("50")){
						cd.telephone = "+390" + cd.telephone;
					}					

				}
				else 
					System.out.println("Validazione numero di telefono non avvenuta. Il campo verra' settato a null");
				cd.telephone = null;


				locazione._save();
				cd._save();
			}
			em.persist(locazione);
			em.persist(cd);


		} catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			mysqlCon.close();
		}


	}
	
	public static void riempiTabelle() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{

		fillWorkingTime();
		fillVacationType();
		fillPerson();
		fillPersonVacation();
		fillYearRecap();
		fillMonthRecap();
		fillContactAndLocation();

	}

}
