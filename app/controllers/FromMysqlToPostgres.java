package controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import models.ContactData;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonVacation;
import models.VacationType;
import models.WorkingTimeType;
import models.YearRecap;

import play.db.jpa.JPA;
import play.mvc.Controller;

public class FromMysqlToPostgres extends Controller{
	
	private static Logger log;
	
	public static String mySqldriver = "com.mysql.jdbc.Driver";	

	private static Connection mysqlCon = null;
	
	private static Connection getMysqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (mysqlCon != null ) {
			return mysqlCon;
		}
		Class.forName(mySqldriver).newInstance();

		return DriverManager.getConnection("jdbc:mysql://localhost:3306/IIT?zeroDateTimeBehavior=convertToNull","root", "orologio");
	}
	
	public static void fillTables() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{			

			stmt = mysqlCon.prepareStatement("SELECT * FROM Persone ");

			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			
			short id = 0;
			
			Person person = null;
			Location location = null;
			ContactData contactData = null;
			while(rs.next()){
				/**
				 * costruzione istanze delle tabelle Person, ContactData e Location 
				 * con conseguente riempimento dei campi presi dalla tabella Persone
				 * sul db mysql
				 */
				person = new Person();
				location = new Location();
				contactData = new ContactData();
				
				person.name = rs.getString("Nome");
				person.surname = rs.getString("Cognome");
				person.bornDate = rs.getDate("DataNascita");
				//person.contractLevel = rs.getInteger("Qualifica");
				person.number = rs.getInt("Matricola");
				
				location.department = rs.getString("Dipartimento");
				location.headOffice = rs.getString("Sede");
				location.room = rs.getString("Stanza");
				location.person.id = rs.getLong("ID");
				
				contactData.email = rs.getString("Email");
				contactData.fax = rs.getString("Fax");
				contactData.telephone = rs.getString("Telefono");
				/**
				 * controllo sui valori del campo Telefono e conseguente modifica sul nuovo db
				 */
				if(contactData.telephone != null){
					if(contactData.telephone.length() == 4){
						contactData.telephone = "+39050315" + contactData.telephone;
					}
					if((contactData.telephone.startsWith("3"))&&(contactData.telephone.length() > 4)){
						contactData.mobile = contactData.telephone;
						contactData.telephone = null;
					}
					if(contactData.telephone.startsWith("50")){
						contactData.telephone = "+390" + contactData.telephone;
					}					

				}
				else 
					log.warn("Validazione numero di telefono non avvenuta. Il campo verra' settato a null");
				contactData.telephone = null;
				
				/**
				 * recupero id del soggetto per fare le query sulle tabelle correlate a Persone e popolare
				 * le tabelle del nuovo db in relazione con Person
				 */
				id = rs.getShort("ID");
				/**
				 * query su ferie_pers per popolare VacationType
				 */
				PreparedStatement stmt2 = mysqlCon.prepareStatement("SELECT * FROM ferie WHERE pid="+id);
				ResultSet rs2 = stmt2.executeQuery();
				PersonVacation personVacation = null;
				
				while(rs2.next()){
					personVacation = new PersonVacation();
					personVacation.beginFrom = rs2.getDate("data_inizio");
					personVacation.endTo = rs2.getDate("data_fine");
					personVacation.vacationType.id = rs2.getLong("fid");
					
					personVacation._save();
				}
				em.persist(personVacation);				
								
				/**
				 * query su totali_anno per recuperare lo storico da mettere in YearRecap
				 */
				PreparedStatement stmt3 = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
				ResultSet rs3 = stmt3.executeQuery();
				YearRecap recap = null;
				while(rs3.next()){
					recap = new YearRecap();
					recap.id = rs3.getLong("ID");
					recap = new YearRecap();
					recap.year = rs3.getShort("anno");
					recap.remaining = rs3.getInt("residuo");
					recap.remainingAp = rs3.getInt("residuoap");
					recap.recg = rs3.getInt("recg");
					recap.recgap = rs3.getInt("recgap");
					recap.overtime = rs3.getInt("straord");
					recap.overtimeAp = rs3.getInt("straordap");
					recap.recguap = rs3.getInt("recguap");
					recap.recm = rs3.getInt("recm");
					recap.lastModified = rs3.getTimestamp("data_ultimamod");
					
					recap._save();
				}
				em.persist(recap);
				
				/**
				 * query su totali_mens per recueperare lo storico mensile da mettere su monthRecap
				 */
				PreparedStatement stmt4 = mysqlCon.prepareStatement("SELECT * FROM totali_mens WHERE ID="+id);
				ResultSet rs4 = stmt4.executeQuery();
				MonthRecap monthRecap = null;
				while(rs.next()){
					monthRecap = new MonthRecap();
					monthRecap.id = rs4.getLong("ID");
					monthRecap.month = rs4.getShort("mese");
					monthRecap.year = rs4.getShort("anno");
					monthRecap.workingDays = rs4.getShort("giorni_lavorativi");
					monthRecap.daysWorked = rs4.getShort("giorni_lavorati");
					monthRecap.giorniLavorativiLav = rs4.getShort("giorni_lavorativi");
					monthRecap.workTime = rs4.getInt("tempo_lavorato");
					monthRecap.remaining = rs4.getInt("residuo");
					monthRecap.justifiedAbsence = rs4.getShort("assenze_giust");
					monthRecap.vacationAp = rs4.getShort("ferie_ap");
					monthRecap.vacationAc = rs4.getShort("ferie_ac");
					monthRecap.holidaySop = rs4.getShort("festiv_sop");
					monthRecap.recoveries = rs4.getInt("recuperi");
					monthRecap.recoveriesAp = rs4.getShort("recuperiap");
					monthRecap.recoveriesG = rs4.getShort("recuperig");
					monthRecap.recoveriesGap = rs4.getShort("recuperigap");
					monthRecap.overtime = rs4.getInt("ore_str");
					monthRecap.lastModified = rs4.getTimestamp("data_ultimamod");
					monthRecap.residualApUsed = rs4.getInt("residuoap_usato");
					monthRecap.extraTimeAdmin = rs4.getInt("tempo_eccesso_ammin");
					monthRecap.additionalHours = rs4.getInt("ore_aggiuntive");
					monthRecap.nadditionalHours = rs4.getByte("nore_aggiuntive");
					monthRecap.residualFine = rs4.getInt("residuo_fine");
					monthRecap.beginWork = rs4.getByte("inizio_lavoro");
					monthRecap.endWork = rs4.getByte("fine_lavoro");
					monthRecap.timeHourVisit = rs4.getInt("tempo_visite_orarie");
					monthRecap.endRecoveries = rs4.getShort("recuperi_fine");
					monthRecap.negative = rs4.getInt("negativo");
					monthRecap.endNegative = rs4.getInt("negativo_fine");
					monthRecap.progressive = rs4.getString("progressivo");				

					monthRecap._save();
				}
				em.persist(monthRecap);

				
				
				
				
				
				
				PreparedStatement stmt6 = mysqlCon.prepareStatement("SELECT * FROM ferie_pers WHERE pid="+id);
				PreparedStatement stmt7 = mysqlCon.prepareStatement("SELECT * FROM ferie_pers WHERE pid="+id);
				PreparedStatement stmt8 = mysqlCon.prepareStatement("SELECT * FROM ferie_pers WHERE pid="+id);
				PreparedStatement stmt9 = mysqlCon.prepareStatement("SELECT * FROM ferie_pers WHERE pid="+id);
				
						
				person._save();
				contactData._save();
				location._save();
			}
			em.persist(person);
			em.persist(contactData);
			em.persist(location);

			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}


}
