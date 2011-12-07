package controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import models.AbsenceType;
import models.Absences;
import models.Codes;
import models.ContactData;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonVacation;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.Stamping.WayType;
import models.VacationType;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay.DayOfWeek;
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
	
	public static void fillOtherTables() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{

			/**
			 * la query sulla tabella opzioni non viene effettuata poichè in quella tabella sul db mysql 
			 * non ci sono dati, pertanto la tabella verrà solo creata.
			 * preparo la query per recuperare i dati dalla tabella codici
			 */
			stmt = mysqlCon.prepareStatement("SELECT * FROM Codici");
			ResultSet rs = stmt.executeQuery();
			EntityManager em = JPA.em();
		
			ResultSet rs6 = stmt.executeQuery();
			Codes codes = null;
			while(rs6.next()){
				/**
				 * popolo la tabella Codes
				 */
				codes = new Codes();
				codes.code = rs6.getString("Codice");
				codes.code_att = rs6.getString("Codice_att");
				codes.description = rs6.getString("Descrizione");
				if(rs6.getByte("Inattivo")==0)
					codes.inactive = false;
				else
					codes.inactive = true;
				if(rs6.getByte("Interno")==0)
					codes.internal = false;
				else 
					codes.internal = true;
				codes.fromDate = rs6.getDate("DataInizio");
				codes.toDate = rs6.getDate("DataFine");
				codes.qualification = rs6.getString("Qualifiche");
				codes.group = rs6.getString("Gruppo");
				codes.value = rs6.getInt("Valore");
				if(rs6.getByte("MinutiEccesso")==0)
					codes.minutesOver = false;
				else
					codes.minutesOver = true;
				if(rs6.getByte("QuantMin")==0)
					codes.quantMin = false;
				else
					codes.quantMin = true;
				codes.storage = rs6.getShort("Accumulo");
				if(rs6.getByte("Recuperabile")==0)
					codes.recoverable = false;
				else
					codes.recoverable = true;
				codes.limit = rs6.getInt("Limite");
				codes.gestLim = rs6.getShort("GestLim");
				codes.codiceSost = rs6.getString("CodiceSost");
				if(rs6.getByte("IgnoraTimbr")==0)
					codes.ignoreStamping = false;
				else
					codes.ignoreStamping = true;
				if(rs6.getByte("UsoMulti")==0)
					codes.usoMulti = false;
				else
					codes.usoMulti = true;
				if(rs6.getByte("TempoBuono")==0)
					codes.tempoBuono = false;
				else
					codes.tempoBuono = true;
			}
			em.persist(codes);
						
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void fillTables() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;
		try{			
			/**
			 * preparo la query sulla tabella persone, che sarà la query principale 
			 */
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
				em.persist(person);
				
				location.person = person;
				location.department = rs.getString("Dipartimento");
				location.headOffice = rs.getString("Sede");
				location.room = rs.getString("Stanza");
				
				contactData.person = person;
				contactData.email = rs.getString("Email");
				contactData.fax = rs.getString("Fax");
				contactData.telephone = rs.getString("Telefono");
				
				em.persist(location);				
				
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
				em.persist(contactData);
				
				/**
				 * recupero id del soggetto per fare le query sulle tabelle correlate a Persone e popolare
				 * le tabelle del nuovo db in relazione con Person
				 */
				id = rs.getShort("ID");
				/**
				 * query su ferie_pers per popolare VacationType e PersonVacation
				 */
				PreparedStatement stmt2 = mysqlCon.prepareStatement("SELECT * FROM ferie WHERE pid="+id);
				ResultSet rs2 = stmt2.executeQuery();
				PersonVacation personVacation = null;
				VacationType vacationType = null;
				
				while(rs2.next()){
					vacationType = new VacationType();
					vacationType.description = rs2.getString("nome");
					personVacation = new PersonVacation();
					personVacation.person = person;
					personVacation.vacationType = vacationType;
					personVacation.beginFrom = rs2.getDate("data_inizio");
					personVacation.endTo = rs2.getDate("data_fine");
					personVacation.vacationType.id = rs2.getLong("fid");
					
					personVacation._save();
					vacationType._save();
				}
				em.persist(personVacation);			
				em.persist(vacationType);
								
				/**
				 * query su totali_anno per recuperare lo storico da mettere in YearRecap
				 */
				PreparedStatement stmt3 = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
				ResultSet rs3 = stmt3.executeQuery();
				YearRecap recap = null;
				while(rs3.next()){
					recap = new YearRecap();
					recap.person = person;
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
				
				PreparedStatement stmt8 = mysqlCon.prepareStatement("SELECT * FROM orari_di_lavoro ");

				ResultSet rs8 = stmt.executeQuery();

				WorkingTimeType wtt = null;
				WorkingTimeTypeDay wttd = null;


				while(rs.next()){

					wtt = new WorkingTimeType();

					wtt.person = person;
					wtt.description = rs.getString("nome");

					wtt.shift = rs.getBoolean("turno");
					
					wttd = new WorkingTimeTypeDay();
					wttd.workingTimeType = wtt;
					wttd.dayOfWeek = DayOfWeek.monday;
					wttd.workingTime = rs.getInt("lu_tempo_lavoro");
					wttd.holiday = rs.getBoolean("lu_festa");
					wttd.timeSlotEntranceFrom = rs.getInt("lu_fascia_ingresso");
					wttd.timeSlotEntranceTo = rs.getInt("lu_fascia_ingresso1");
					wttd.timeSlotExitFrom = rs.getInt("lu_fascia_uscita");
					wttd.timeSlotExitTo = rs.getInt("lu_fascia_uscita1");
					wttd.timeMealFrom = rs.getInt("lu_fascia_pranzo");
					wttd.timeMealTo = rs.getInt("lu_fascia_pranzo1");
					wttd.breakTicketTime = rs.getInt("lu_tempo_interv"); 
					wttd.mealTicketTime = rs.getInt("lu_tempo_buono");

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
					wttd._save();

				}
				em.persist(wtt);
				em.persist(wttd);

			}
				
				/**
				 * query su totali_mens per recueperare lo storico mensile da mettere su monthRecap
				 */
				PreparedStatement stmt4 = mysqlCon.prepareStatement("SELECT * FROM totali_mens WHERE ID="+id);
				ResultSet rs4 = stmt4.executeQuery();
				MonthRecap monthRecap = null;
				while(rs4.next()){
					monthRecap = new MonthRecap();
					monthRecap.person = person;
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
					if(rs4.getByte("nore_aggiuntive")==0)
						monthRecap.nadditionalHours = false;
					else 
						monthRecap.nadditionalHours = true;
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
				
				
				PreparedStatement stmt5 = mysqlCon.prepareStatement("SELECT " +
						"Orario.ID, Orario.Giorno, Orario.TipoGiorno, Orario.TipoTimbratura, Orario.Ora, " +
						"Orario.Ora1, orario_pers.data_inizio, orario_pers.data_fine " +
						"FROM orario_pers,Orario, Persone " +
						"WHERE Persone.id=orario_pers.pid " +
						"AND orario_pers.pid=Orario.id " +
						"AND Persone.id="+id);
				ResultSet rs5 = stmt5.executeQuery();
				Stamping stamping = null;
				while(rs5.next()){
										
					/**
					 * popolo la tabella stampings
					 */
					stamping = new Stamping();
					byte tipoTimbratura = rs5.getByte("TipoTimbratura");
					Date giorno = rs.getDate("Giorno");
					Time ora = rs.getTime("Ora");
					if((int)tipoTimbratura%2 != 0)
						stamping.way = WayType.in;					
					else
						stamping.way = WayType.out;
					Calendar calGiorno = new GregorianCalendar();
                    calGiorno.setTime(giorno);
                    Calendar calOra = new GregorianCalendar();
                    calOra.setTime(ora);
                    
                    calGiorno.set(Calendar.HOUR, calOra.get(Calendar.HOUR));
                    calGiorno.set(Calendar.MINUTE, calOra.get(Calendar.MINUTE));
                    calGiorno.set(Calendar.SECOND, calOra.get(Calendar.SECOND));
                    stamping.date = new LocalDate(calGiorno);
					
					stamping._save();
				}
				em.persist(stamping);
			
				
				PreparedStatement stmt7 = mysqlCon.prepareStatement("SELECT Persone.ID, assenze_init.id, assenze_init.anno, " +
						"assenze_init.mese, assenze_init.giorno, assenze_init.Codice, assenze_init.giorni, " +
						"assenze.matricola, assenze.mese AS 'AssenzeMese', assenze.anno AS 'AssenzeAnno', " +
						"assenze.g1, assenze.g2 " +
						"FROM assenze, assenze_init, Persone " +
						"WHERE Persone.ID=assenze.ID and assenze.ID=assenze_init.idp and Persone.ID="+id);
				ResultSet rs7 = stmt7.executeQuery();
				Absences absence = null;
				AbsenceType absenceType = null;
				while(rs7.next()){
					/**
					 * popolo la tabella absence e la tabella absenceType con i dati prelevati da assenze e 
					 * assenze_init
					 */
					absence.person = person;
					absenceType = new AbsenceType();
					absence.absenceType = absenceType;
					Calendar cal = new GregorianCalendar();
					cal.set(rs7.getInt("anno"), rs7.getInt("mese"), rs7.getInt("giorno"));
					absence.date = new LocalDate(cal);
					absenceType.code = rs7.getString("codice");
					
					absence._save();
					absenceType._save();
					
				}
				em.persist(absence);
				em.persist(absenceType);				
								
						
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}


}
