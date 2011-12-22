package it.cnr.iit.epas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import models.AbsenceType;

import models.Code;

import models.Absence;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.DailyAbsenceType;
import models.HourlyAbsenceType;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonVacation;
import models.StampType;
import models.Stamping;
import models.WorkingTimeTypeDay;
import models.Stamping.WayType;
import models.VacationType;
import models.WorkingTimeType;
import models.YearRecap;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Controller;

public class FromMysqlToPostgres {
	
	public static String mySqldriver = Play.configuration.getProperty("db.old.driver");//"com.mysql.jdbc.Driver";	

	private static Connection mysqlCon = null;
	
	public static Connection getMysqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (mysqlCon != null ) {
			return mysqlCon;
		}
		Class.forName(mySqldriver).newInstance();

		return DriverManager.getConnection(
				Play.configuration.getProperty("db.old.url"),
				Play.configuration.getProperty("db.old.user"),
				Play.configuration.getProperty("db.old.password"));
	}
	
	public static Person createPerson(ResultSet rs, EntityManager em) throws SQLException {
		Person person = new Person();
		person.name = rs.getString("Nome");
		person.surname = rs.getString("Cognome");
		person.bornDate = rs.getDate("DataNascita");
		person.number = rs.getInt("Matricola");
		em.persist(person);
		return person;
	}
	
	public static void createLocation(ResultSet rs, Person person, EntityManager em) throws SQLException {
		Location location = new Location();
		location.person = person;
		
		location.department = rs.getString("Dipartimento");
		location.headOffice = rs.getString("Sede");
		location.room = rs.getString("Stanza");		
		em.persist(location);
	}
	
	public static void createContactData(ResultSet rs, Person person, EntityManager em) throws SQLException {
		ContactData contactData = new ContactData();
		contactData.person = person;
		
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
			Logger.warn("Validazione numero di telefono non avvenuta. Il campo verra' settato a null");
		contactData.telephone = null;		
		em.persist(contactData);
	}	
	

	public static void createStampings(short id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulle tabelle orario, orario_pers per recuperare le info sulle timbrature
		 * di ciascuna persona
		 */
		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT * FROM Orario WHERE TipoGiorno = 0 and id=" + id);
		ResultSet rs = stmtOrari.executeQuery();		
			
			while(rs.next()){
				
				byte tipoTimbratura = rs.getByte("TipoTimbratura");
				StampType stampType = new StampType();
				if((int)tipoTimbratura % 2 == 1 && (int)tipoTimbratura / 2 == 0){
					stampType.description = "Prima timbratura di ingresso";					
				}
				if((int)tipoTimbratura % 2 == 0 && (int)tipoTimbratura / 2 == 1){
					stampType.description = "Prima timbratura di uscita";
				}
				if((int)tipoTimbratura % 2 == 1 && (int)tipoTimbratura / 2 == 1){
					stampType.description = "Timbratura di ingresso";
				}
				if((int)tipoTimbratura % 2 == 0 && (int)tipoTimbratura / 2 == 2){
					stampType.description = "Timbratura di uscita";
				}
				em.persist(stampType);
				Stamping stamping = new Stamping();
				/**
				 * popolo la tabella stampings
				 */
				stamping.person = person;				
				
				if((int)tipoTimbratura % 2 != 0)
					stamping.way = WayType.in;					
				else
					stamping.way = WayType.out;
				Date giorno = rs.getDate("Giorno");
				Time ora = null;
				try {
					/**
					 * per adesso svolgo l'intero processo del controllo sull'ora all'interno del metodo.
					 * In seguito verrà usata una funzione da chiamare che svolgerà questo compito.
					 */
					
					//il campo Ora contiene dei valori piuttosto "bizzarri", tipo 52:30:12, piuttosto che -11:40:00
					//quindi il campo viene prelevato "row" come byte[] a poi convertito in stringa in modo da evitare
					//controlli di consistenza da parte del driver JDBC che tenta di validare il campo come time.
					byte[] bs = rs.getBytes("Ora");
					String s = bs != null ? new String(bs) : null;
					
					if(s == null){
						stamping.date = null;
						stamping.isMarkedByAdmin = false;
						stamping.isServiceExit = false;
					}
					else{
						if(s.startsWith("-")){
							int hour = Integer.parseInt(s.substring(1, 3));
							int minute = Integer.parseInt(s.substring(4, 6));
							int second = Integer.parseInt(s.substring(7, 9));
							@SuppressWarnings("deprecation")
							Time newOra = new Time(hour,minute,second);
							Calendar calGiorno = new GregorianCalendar();
			                calGiorno.setTime(giorno);
			                Calendar calOra = new GregorianCalendar();
			                calOra.setTime(newOra);
			                
			                calGiorno.set(Calendar.HOUR, calOra.get(Calendar.HOUR));
			                calGiorno.set(Calendar.MINUTE, calOra.get(Calendar.MINUTE));
			                calGiorno.set(Calendar.SECOND, calOra.get(Calendar.SECOND));
			                stamping.date = new LocalDate(calGiorno);
			                stamping.isMarkedByAdmin = false;
			                stamping.isServiceExit = true;
						}
						else{
							
							int hour = Integer.parseInt(s.substring(0, 2));
							int minute = Integer.parseInt(s.substring(3, 5));
													
							if(hour > 33){
								hour = hour * 60;
								hour = hour + minute;
								hour = hour - 2000;
								hour = hour / 60;
								int min = hour % 60;
								
								@SuppressWarnings("deprecation")
								Time newOra = new Time(hour,min,0);
								Calendar calGiorno = new GregorianCalendar();
				                calGiorno.setTime(giorno);
				                Calendar calOra = new GregorianCalendar();
				                calOra.setTime(newOra);
				                
				                calGiorno.set(Calendar.HOUR, calOra.get(Calendar.HOUR));
				                calGiorno.set(Calendar.MINUTE, calOra.get(Calendar.MINUTE));
				                calGiorno.set(Calendar.SECOND, calOra.get(Calendar.SECOND));
				                stamping.date = new LocalDate(calGiorno);
				                stamping.isMarkedByAdmin = true;
				                stamping.isServiceExit = false;
							}						
											
							else{
								ora = rs.getTime("Ora");
								Calendar calGiorno = new GregorianCalendar();
				                calGiorno.setTime(giorno);
				                Calendar calOra = new GregorianCalendar();
				                calOra.setTime(ora);
				                
				                calGiorno.set(Calendar.HOUR, calOra.get(Calendar.HOUR));
				                calGiorno.set(Calendar.MINUTE, calOra.get(Calendar.MINUTE));
				                calGiorno.set(Calendar.SECOND, calOra.get(Calendar.SECOND));
				                stamping.date = new LocalDate(calGiorno);
				                stamping.isMarkedByAdmin = false;
				                stamping.isServiceExit = false;
							}
						}
					}
				}				
				 catch (SQLException sqle) {
					//L'ora va "corretta"
					sqle.printStackTrace();
					System.out.println("Timbratura errata. Persona con id="+id);
					Logger.warn("Timbratura errata. Persona con id= "+id);
				}			
				
				em.persist(stamping);	
				
			}		

	}
	
	public static void createAbsences(short id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulla tabelle Orario e Codici per recuperare le info sulle assenze e i motivi delle
		 * assenze di ciascuna persona. La query prende tutti i codici di assenza che vengono poi "smistati"
		 * nella tabella di postgres corrispondente attraverso l'analisi del campo QuantGiust presente soltanto 
		 * nelle righe relative a codici di natura giornaliera.
		 */
		PreparedStatement stmtAssenze = mysqlCon.prepareStatement("Select Orario.Giorno, Orario.TipoTimbratura, " +
				"Codici.Descrizione, Codici.QuantGiust, Codici.IgnoraTimbr, Codici.MinutiEccesso, Codici.Limite, " +
				"Codici.Accumulo, Codici.CodiceSost " +
				"from Persone, Codici, Orario " +
				"where Orario.TipoGiorno=Codici.id " +
				"and TipoGiorno !=0 and Orario.id = "+id);
		ResultSet rs = stmtAssenze.executeQuery();
		if(rs != null){
			Absence absence = null;
			AbsenceType absenceType = null;
			AbsenceTypeGroup absTypeGroup = null;
			while(rs.next()){			
				/**
				 * popolo la tabella absence, la tabella absenceType, la tabella absenceTypeGroup
				 * e le tabelle HourlyAbsenceType e DailyAbsenceType
				 * con i dati prelevati da Orario e Codici
				 */
				absence = new Absence();
				absence.person = person;
				absence.date = new LocalDate(rs.getDate("Giorno"));				
				em.persist(absence);
				
				absenceType = new AbsenceType();
				absence.absenceType = absenceType;
				absenceType.code = rs.getString("codice");
				absenceType.description = rs.getString("Descrizione");
				if(rs.getByte("IgnoraTimbr")==0)
					absenceType.ignoreStamping = false;
				else 
					absenceType.ignoreStamping = true;
				em.persist(absenceType);
				
				absTypeGroup = new AbsenceTypeGroup();
				absenceType.absenceTypeGroup = absTypeGroup;
				if(rs.getByte("IgnoraTimbr")==0)
					absTypeGroup.minutesExcess = false;
				else 
					absTypeGroup.minutesExcess= true;
				absTypeGroup.equivalentCode = rs.getString("CodiceSost");
				absTypeGroup.buildUp = rs.getInt("Accumulo");
				absTypeGroup.buildUpLimit = rs.getInt("Limite");				
								
				em.persist(absTypeGroup);
				
				/**
				 * caso di assenze orarie
				 */
				if(rs.getInt("QuantGiust") != 0){
					HourlyAbsenceType hourlyAbsenceType = new HourlyAbsenceType();
					hourlyAbsenceType.absenceType = absenceType;					
					hourlyAbsenceType.justifiedWorkTime = rs.getInt("QuantGiust");
					em.persist(hourlyAbsenceType);
				}
				/**
				 * caso di assenze giornaliere
				 */
				else{
					DailyAbsenceType dailyAbsenceType = new DailyAbsenceType();
					dailyAbsenceType.absenceType = absenceType;
					
					em.persist(dailyAbsenceType);
				}
					
			}
		}		
	}
	
	public static void createVacations(short id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su ferie_pers per popolare VacationType e PersonVacation
		 */
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT ferie.nome,ferie_pers.data_inizio," +
				"ferie_pers.data_fine FROM ferie,ferie_pers " +
				"WHERE ferie_pers.fid=ferie.id AND ferie_pers.pid="+id);
		ResultSet rs = stmt.executeQuery();
		PersonVacation personVacation = null;
		VacationType vacationType = null;
		try{
			if(rs != null){
				
				while(rs.next()){										
				
					vacationType = new VacationType();
					vacationType.description = rs.getString("nome");
					personVacation = new PersonVacation();
					personVacation.person = person;
					personVacation.vacationType = vacationType;
					personVacation.beginFrom = rs.getDate("data_inizio");
					personVacation.endTo = rs.getDate("data_fine");					
				
				}
			}
		}
		catch(SQLException sqle) {				
				sqle.printStackTrace();
				System.out.println("Ferie errate. Persona con id="+id);				
			}			
			em.persist(personVacation);			
			em.persist(vacationType);
		}
	
	
	public static void createWorkingTimeTypes(short id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su orari di lavoro in join con orario pers e Persone
		 * per popolare workin_time_type e working_time_type_days
		 */
		Connection mysqlCon = getMysqlConnection();		
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM orari_di_lavoro,Persone,orario_pers WHERE " +
				"Persone.ID=orario_pers.pid AND orario_pers.oid=orari_di_lavoro.id and Persone.id="+id);

		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			WorkingTimeType wtt = null;
			WorkingTimeTypeDay wttd_mo = null;
			WorkingTimeTypeDay wttd_tu = null;
			WorkingTimeTypeDay wttd_we = null;
			WorkingTimeTypeDay wttd_th = null;
			WorkingTimeTypeDay wttd_fr = null;
			WorkingTimeTypeDay wttd_sa = null;
			WorkingTimeTypeDay wttd_su = null;
			while(rs.next()){
				
				wtt = new WorkingTimeType();

				person.workingTimeType = wtt;
				wtt.description = rs.getString("nome");

				wtt.shift = rs.getBoolean("turno");
				
				em.persist(wtt);
				
				wttd_mo = new WorkingTimeTypeDay();
				wttd_mo.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.MONDAY;
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
				em.persist(wttd_mo);

				wttd_tu = new WorkingTimeTypeDay();
				wttd_tu.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.TUESDAY;
				wttd_tu.workingTime = rs.getInt("ma_tempo_lavoro");
				wttd_tu.holiday = rs.getBoolean("ma_festa");
				wttd_tu.timeSlotEntranceFrom = rs.getInt("ma_fascia_ingresso");
				wttd_tu.timeSlotEntranceTo = rs.getInt("ma_fascia_ingresso1");
				wttd_tu.timeSlotExitFrom = rs.getInt("ma_fascia_uscita");
				wttd_tu.timeSlotExitTo = rs.getInt("ma_fascia_uscita1");
				wttd_tu.timeMealFrom = rs.getInt("ma_fascia_pranzo");
				wttd_tu.timeMealTo = rs.getInt("ma_fascia_pranzo1");
				wttd_tu.breakTicketTime = rs.getInt("ma_tempo_interv"); 
				wttd_tu.mealTicketTime = rs.getInt("ma_tempo_buono"); 
				em.persist(wttd_tu);

				wttd_we = new WorkingTimeTypeDay();
				wttd_we.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.WEDNESDAY;
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
				em.persist(wttd_we);

				wttd_th = new WorkingTimeTypeDay();
				wttd_th.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.THURSDAY;
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
				em.persist(wttd_th);

				wttd_fr = new WorkingTimeTypeDay();
				wttd_fr.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.FRIDAY;
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
				em.persist(wttd_fr);

				wttd_sa = new WorkingTimeTypeDay();
				wttd_sa.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.SATURDAY;
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
				em.persist(wttd_sa);

				wttd_su = new WorkingTimeTypeDay();		
				wttd_su.workingTimeType = wtt;
				wttd_mo.dayOfWeek = DateTimeConstants.SUNDAY;
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
				em.persist(wttd_su);
			
			}
		}
	}
	
	public static void createYearRecap(short id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			YearRecap yearRecap = null;
			while(rs.next()){										
			
				yearRecap = new YearRecap();
				yearRecap.person = person;
				yearRecap.year = rs.getShort("anno");
				yearRecap.remaining = rs.getInt("residuo");
				yearRecap.remainingAp = rs.getInt("residuoap");
				yearRecap.recg = rs.getInt("recg");
				yearRecap.recgap = rs.getInt("recgap");
				yearRecap.overtime = rs.getInt("straord");
				yearRecap.overtimeAp = rs.getInt("straordap");
				yearRecap.recguap = rs.getInt("recguap");
				yearRecap.recm = rs.getInt("recm");
				yearRecap.lastModified = rs.getTimestamp("data_ultimamod");
									
			}
			em.persist(yearRecap);
		}
	}
	public static void createMonthRecap(short id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su totali_mens per recueperare lo storico mensile da mettere su monthRecap
		 */
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_mens WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			MonthRecap monthRecap = null;
			while(rs.next()){
				
				monthRecap = new MonthRecap();
				monthRecap.person = person;
				monthRecap.month = rs.getShort("mese");
				monthRecap.year = rs.getShort("anno");
				monthRecap.workingDays = rs.getShort("giorni_lavorativi");
				monthRecap.daysWorked = rs.getShort("giorni_lavorati");
				monthRecap.giorniLavorativiLav = rs.getShort("giorni_lavorativi");
				monthRecap.workTime = rs.getInt("tempo_lavorato");
				monthRecap.remaining = rs.getInt("residuo");
				monthRecap.justifiedAbsence = rs.getShort("assenze_giust");
				monthRecap.vacationAp = rs.getShort("ferie_ap");
				monthRecap.vacationAc = rs.getShort("ferie_ac");
				monthRecap.holidaySop = rs.getShort("festiv_sop");
				monthRecap.recoveries = rs.getInt("recuperi");
				monthRecap.recoveriesAp = rs.getShort("recuperiap");
				monthRecap.recoveriesG = rs.getShort("recuperig");
				monthRecap.recoveriesGap = rs.getShort("recuperigap");
				monthRecap.overtime = rs.getInt("ore_str");
				monthRecap.lastModified = rs.getTimestamp("data_ultimamod");
				monthRecap.residualApUsed = rs.getInt("residuoap_usato");
				monthRecap.extraTimeAdmin = rs.getInt("tempo_eccesso_ammin");
				monthRecap.additionalHours = rs.getInt("ore_aggiuntive");
				if(rs.getByte("nore_aggiuntive")==0)
					monthRecap.nadditionalHours = false;
				else 
					monthRecap.nadditionalHours = true;
				monthRecap.residualFine = rs.getInt("residuo_fine");
				monthRecap.beginWork = rs.getByte("inizio_lavoro");
				monthRecap.endWork = rs.getByte("fine_lavoro");
				monthRecap.timeHourVisit = rs.getInt("tempo_visite_orarie");
				monthRecap.endRecoveries = rs.getShort("recuperi_fine");
				monthRecap.negative = rs.getInt("negativo");
				monthRecap.endNegative = rs.getInt("negativo_fine");
				monthRecap.progressive = rs.getString("progressivo");				

			}
			em.persist(monthRecap);
		}
	}
	
	public static void createCompetence(short id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		/**
		 * funzione che riempe la tabella competence e la tabella competence_code relativamente alle competenze
		 * di una determinata persona
		 */
		
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("Select codici_comp.id, competenze.mese, " +
				"competenze.anno, competenze.codice, competenze.valore, codici_comp.descrizione, codici_comp.inattivo " +
				"from competenze, codici_comp where codici_comp.codice=competenze.codice and competenze.id= "+id);
		ResultSet rs = stmt.executeQuery();
		
		Competence competence = null;
		CompetenceCode competenceCode = null;
		Map<Integer,Integer> mappaCodici = new HashMap<Integer,Integer>();
		while(rs.next()){			
			competence = new Competence();
			competence.person = person;
			competence.value = rs.getInt("valore");
			competence.code = rs.getString("codice");
			competence.month = rs.getInt("mese");
			competence.year = rs.getInt("anno");
			int idCodiciCompetenza = rs.getInt("id");	
			if(mappaCodici.get(idCodiciCompetenza)== null){
				competenceCode = new CompetenceCode();
				competenceCode.description = rs.getString("descrizione");
				
				if(rs.getByte("inattivo")==0)
					competenceCode.inactive = false;
				else 
					competenceCode.inactive = true;
				long c = competenceCode.id;
				int codiceCompetenza = (int)c;
				Integer codiceCompetenzaNuovo = new Integer(codiceCompetenza);
				mappaCodici.put(idCodiciCompetenza,codiceCompetenzaNuovo);
				em.persist(competenceCode);
				em.persist(competence);
			}
			else{
				competenceCode = CompetenceCode.findById(idCodiciCompetenza);
				competence.competenceCode = competenceCode;				
				competenceCode.description = rs.getString("descrizione");
				
				if(rs.getByte("inattivo")==0)
					competenceCode.inactive = false;
				else 
					competenceCode.inactive = true;
				em.persist(competenceCode);
				em.persist(competence);
			}
			
		}		
		
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
			
			Code codes = null;
			while(rs.next()){
				/**
				 * popolo la tabella Codes
				 */
				codes = new Code();
				codes.code = rs.getString("Codice");
				codes.code_att = rs.getString("Codice_att");
				codes.description = rs.getString("Descrizione");
				if(rs.getByte("Inattivo")==0)
					codes.inactive = false;
				else
					codes.inactive = true;
				if(rs.getByte("Interno")==0)
					codes.internal = false;
				else 
					codes.internal = true;
				codes.fromDate = rs.getDate("DataInizio");
				codes.toDate = rs.getDate("DataFine");
				codes.qualification = rs.getString("Qualifiche");
				codes.groupOf = rs.getString("Gruppo");
				codes.value = rs.getInt("Valore");
				if(rs.getByte("MinutiEccesso")==0)
					codes.minutesOver = false;
				else
					codes.minutesOver = true;
				if(rs.getByte("QuantMin")==0)
					codes.quantMin = false;
				else
					codes.quantMin = true;
				codes.storage = rs.getShort("Accumulo");
				if(rs.getByte("Recuperabile")==0)
					codes.recoverable = false;
				else
					codes.recoverable = true;
				codes.limitOf = rs.getInt("Limite");
				codes.gestLim = rs.getShort("GestLim");
				codes.codiceSost = rs.getString("CodiceSost");
				if(rs.getByte("IgnoraTimbr")==0)
					codes.ignoreStamping = false;
				else
					codes.ignoreStamping = true;
				if(rs.getByte("UsoMulti")==0)
					codes.usoMulti = false;
				else
					codes.usoMulti = true;
				if(rs.getByte("TempoBuono")==0)
					codes.tempoBuono = false;
				else
					codes.tempoBuono = true;
			}
			em.persist(codes);
						
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	public static void fillTables() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt;		
		
		try{			
			/**
			 * preparo la query sulla tabella persone, che sarà la query principale 
			 */
			stmt = mysqlCon.prepareStatement("SELECT * FROM Persone");

			ResultSet rs = stmt.executeQuery();

			EntityManager em = JPA.em();
			em.getTransaction().begin();
			
			short id;
			
			Person person = null;
			while(rs.next()){
				
				/**
				 * recupero id del soggetto per fare le query sulle tabelle correlate a Persone e popolare
				 * le tabelle del nuovo db in relazione con Person
				 */
				id = rs.getShort("ID");	
				
				/**
				 * costruzione istanze delle tabelle Person, ContactData e Location 
				 * con conseguente riempimento dei campi presi dalla tabella Persone
				 * sul db mysql
				 */
				person = createPerson(rs, em);							
				createLocation(rs, person, em);
				createContactData(rs, person, em);
				
				/**
				 * popolo le tabelle stamping, absences, absence_type, vacation_type, person_vacation,
				 * working_time_type, working_time_type_day, year_recap e month_recap invocando i rispettivi
				 * metodi 
				 */
				createStampings(id, person, em);
				createAbsences(id, person, em);
				createVacations(id, person, em);
				createWorkingTimeTypes(id, person, em);
				createYearRecap(id, person, em);
				createMonthRecap(id, person, em);
				
								
			} //qui finisce il while principale di Person
			
			em.getTransaction().commit();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
