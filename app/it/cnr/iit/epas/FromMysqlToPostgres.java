package it.cnr.iit.epas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import models.AbsenceType;

import models.Code;

import models.Absence;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;
import models.DailyAbsenceType;
import models.HourlyAbsenceType;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonVacation;
import models.StampType;
import models.Stamping;
import models.VacationPeriod;
import models.WorkingTimeTypeDay;
import models.Stamping.WayType;
import models.VacationCode;
import models.WorkingTimeType;
import models.YearRecap;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.mvc.Controller;

public class FromMysqlToPostgres {
	
	public static Map<Integer,Long> mappaCodiciCompetence = new HashMap<Integer,Long>();
	public static Map<Integer,Long> mappaCodiciAbsence = new HashMap<Integer,Long>();
	public static Map<Integer,Long> mappaCodiciVacationType = new HashMap<Integer,Long>();
	public static Map<Integer,Long> mappaCodiciWorkingTimeType = new HashMap<Integer,Long>();
	public static Map<Integer,Long> mappaCodiciStampType = new HashMap<Integer,Long>();
	
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
		Logger.configuredManually = true;
		Logger.debug("Inizio a creare la persona: "+rs.getString("Nome").toString()+" "+rs.getString("Cognome").toString());
		
		Person person = new Person();
		person.name = rs.getString("Nome");
		person.surname = rs.getString("Cognome");
		person.bornDate = rs.getDate("DataNascita");
		person.number = rs.getInt("Matricola");
		em.persist(person);
		return person;
	}
	
	public static void createLocation(ResultSet rs, Person person, EntityManager em) throws SQLException {
		Logger.warn("Inizio a creare la locazione");
		Location location = new Location();
		location.person = person;
		
		location.department = rs.getString("Dipartimento");
		location.headOffice = rs.getString("Sede");
		location.room = rs.getString("Stanza");		
		em.persist(location);
	}
	
	public static void createContactData(ResultSet rs, Person person, EntityManager em) throws SQLException {
		Logger.warn("Inizio a creare il contact data");
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
			if(contactData.telephone.startsWith("315")){
				contactData.telephone = "+39050" + contactData.telephone;
			}	
			if(contactData.telephone.startsWith("335")){
				contactData.mobile = contactData.telephone;
				contactData.telephone = "No internal number";
			}
			if(contactData.telephone.length() == 2){
				contactData.telephone = "No internal number";
			}
			if(contactData.telephone.startsWith("503")){
				contactData.telephone = "+390" + contactData.telephone;
			}			

		}
		else{ 
			Logger.warn("Validazione numero di telefono non avvenuta. No phone number");
			contactData.telephone = "No phone number";		
		}
		em.persist(contactData);
	}	
	
	/**
	 * 
	 * @param id
	 * @param person
	 * @param em
	 * metodo che crea un contratto per la persona in questione. Se è già presente un contratto per quella persona,
	 * questo viene cancellato nel caso in cui la data di fine del contratto già salvato sia inferiore alla data inizio
	 * del nuovo contratto così da salvare nello storico il contratto precedente.
	 */
	public static void createContract(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtContratto = mysqlCon.prepareStatement("SELECT id,Datainizio,Datafine,continua " +
				"FROM Personedate WHERE id=" + id + " order by Datainizio");
		ResultSet rs = stmtContratto.executeQuery();	
		
		Contract contract = null;
		while(rs.next()){
			Date nuovaDataInizio = null;
			contract = Contract.findById(rs.getLong("id"));
			if(contract != null){
				
				if(contract.endContract == null){
					em.persist(contract);
				}
				else{
					
					if(rs.getDate("Datainizio")==null)
						nuovaDataInizio = new Date(1970-01-01);
					else
						nuovaDataInizio = rs.getDate("Datainizio");
					long endContractMillis=contract.endContract.getTime();
					long startNewContractMillis = nuovaDataInizio.getTime();
					if((contract.endContract != null) && (endContractMillis<startNewContractMillis)){
						contract.delete();
						contract = new Contract();
						contract.person = person;
						if(rs.getDate("Datainizio") != null)
							contract.beginContract = rs.getDate("Datainizio");
						else
							contract.beginContract = null;
						if(rs.getDate("Datafine") != null)
							contract.endContract = rs.getDate("Datafine");
						else
							contract.endContract = null;
						if(rs.getByte("continua")==0)
							contract.isContinued = false;
						else 
							contract.isContinued = true;
						em.persist(contract);		
					}
				}
			}
			else{
				contract = new Contract();
				contract.person = person;
				if(rs.getDate("Datainizio") != null)
					contract.beginContract = rs.getDate("Datainizio");
				else
					contract.beginContract = null;
				if(rs.getDate("Datafine") != null)
					contract.endContract = rs.getDate("Datafine");
				else
					contract.endContract = null;
				if(rs.getByte("continua")==0)
					contract.isContinued = false;
				else 
					contract.isContinued = true;
				em.persist(contract);
							
			}				
		}	
		mysqlCon.close();
	}
	

	@SuppressWarnings("deprecation")
	public static void createStampings(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.debug("Inizio a creare le timbrature");
		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulle tabelle orario, orario_pers per recuperare le info sulle timbrature
		 * di ciascuna persona
		 */
		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT * FROM Orario WHERE TipoGiorno = 0 and id=" + id);
		ResultSet rs = stmtOrari.executeQuery();		
		//Time oraInizioPranzo = new Time(11,59,59);
		//Time oraFinePranzo = new Time(14,59,59);
			while(rs.next()){
				int idCodiceTimbratura = rs.getInt("id");
				if(mappaCodiciStampType.get(idCodiceTimbratura)== null){
					
				}
				byte tipoTimbratura = rs.getByte("TipoTimbratura");
				StampType stampType = new StampType();
				if((int)tipoTimbratura % 2 == 1 && (int)tipoTimbratura / 2 == 0){
					stampType.description = "Timbratura di ingresso";					
				}
				if((int)tipoTimbratura % 2 == 0 && (int)tipoTimbratura / 2 == 1 ){
					stampType.description = "Timbratura d'uscita per pranzo";
				}
				if((int)tipoTimbratura % 2 == 1 && (int)tipoTimbratura / 2 == 1 ){
					stampType.description = "Timbratura di ingresso dopo pausa pranzo";
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
				stamping.stampType = stampType;
				
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

							Calendar calGiorno = new GregorianCalendar();
			                calGiorno.setTime(giorno);
			                
			                calGiorno.set(Calendar.HOUR, hour);
			                calGiorno.set(Calendar.MINUTE, minute);
			                calGiorno.set(Calendar.SECOND, second);
			                //stamping.date = calGiorno.getTime();
			                stamping.date = new LocalDate(calGiorno);
			                
			                stamping.isMarkedByAdmin = false;
			                stamping.isServiceExit = true;
						}
						else{
							
							int hour = Integer.parseInt(s.substring(0, 2));
							int minute = Integer.parseInt(s.substring(3, 5));
							int second = Integer.parseInt(s.substring(6, 8));						
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
				                
				                calGiorno.set(Calendar.HOUR, hour);
				                calGiorno.set(Calendar.MINUTE, min);
				                calGiorno.set(Calendar.SECOND, second);
				                
				                //stamping.date = calGiorno.getTime();
				                stamping.date = new LocalDate(calGiorno);
				                
				                stamping.isMarkedByAdmin = true;
				                stamping.isServiceExit = false;
							}						
											
							else{
								ora = rs.getTime("Ora");
								Calendar calGiorno = new GregorianCalendar();
								/**
								 * controllo sulla validità del campo giorno della tabella Orario.
								 * Le date di tipo 0000-00-00 presenti sul db mysql non sono riconosciute dal db postgres
								 * e al momento dell'inserimento viene riscontrato un errore.
								 * Con l'accorgimento del parametro "zeroDateTimeBehaviour=convertToNull alla stringa di 
								 * connessione al db mysql si risolve l'inconveniente. Però nel momento di inserire un oggetto null
								 * nel nuovo campo data della nuova tabella in postgres viene sollevata
								 * una nullPointerException. Qui la necessità di usare una data fittizia: '1900-01-01' per 
								 * questi casi
								 */
								if(giorno!=null)
									calGiorno.setTime(giorno);
								else
									calGiorno.setTime(new Date(1900-01-01));
				                Calendar calOra = new GregorianCalendar();
				                calOra.setTime(ora);
				                
				                calGiorno.set(Calendar.HOUR, hour);
				                calGiorno.set(Calendar.MINUTE, minute);
				                calGiorno.set(Calendar.SECOND, second);
				                
				                //stamping.date = calGiorno.getTime();
				                stamping.date = new LocalDate(calGiorno);
				                
				                stamping.isMarkedByAdmin = false;
				                stamping.isServiceExit = false;
							}
						}
					}
				}		
				 catch (SQLException sqle) {
					
					sqle.printStackTrace();
					System.out.println("Timbratura errata. Persona con id="+id);
					Logger.warn("Timbratura errata. Persona con id= "+id);
				}			
				Logger.debug("Termino di creare le timbrature");
				em.persist(stamping);	
				
			}
			mysqlCon.close();

	}
	
	public static void createAbsences(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio a creare le assenze");
		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulla tabelle Orario e Codici per recuperare le info sulle assenze e i motivi delle
		 * assenze di ciascuna persona. La query prende tutti i codici di assenza che vengono poi "smistati"
		 * nella tabella di postgres corrispondente attraverso l'analisi del campo QuantGiust presente soltanto 
		 * nelle righe relative a codici di natura giornaliera.
		 */
		PreparedStatement stmtAssenze = mysqlCon.prepareStatement("Select Orario.Giorno, Orario.TipoTimbratura, " +
				"Codici.Codice, Codici.Descrizione, Codici.QuantGiust, Codici.IgnoraTimbr, Codici.MinutiEccesso, Codici.Limite, " +
				"Codici.Accumulo, Codici.CodiceSost, Codici.id, Codici.Gruppo " +
				"from Codici, Orario " +
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
				absence.date = rs.getDate("Giorno");				
				
				int idCodiceAssenza = rs.getInt("id");
				if(mappaCodiciAbsence.get(idCodiceAssenza)== null){
					
					absenceType = new AbsenceType();
					absence.absenceType = absenceType;
					absenceType.code = rs.getString("Codice");
					absenceType.description = rs.getString("Descrizione");
					if(rs.getByte("IgnoraTimbr")==0)
						absenceType.ignoreStamping = false;
					else 
						absenceType.ignoreStamping = true;
									
					em.persist(absenceType);					
					em.persist(absence);
					mappaCodiciAbsence.put(idCodiceAssenza,absenceType.id);
					
					if(rs.getString("Gruppo")!=null){
						absTypeGroup = new AbsenceTypeGroup();
						absTypeGroup.absenceType = absenceType;
						absTypeGroup.label = rs.getString("Gruppo");
						absTypeGroup.buildUp = rs.getInt("Accumulo");
						absTypeGroup.buildUpLimit = rs.getInt("Limite");
						absTypeGroup.equivalentCode = rs.getString("CodiceSost");
						if(rs.getByte("MinutiEccesso")==0)
							absTypeGroup.minutesExcess = false;
						else 
							absTypeGroup.minutesExcess = true; 
						em.persist(absTypeGroup);
					}					
					
				}
				else{
					absenceType = AbsenceType.findById(mappaCodiciAbsence.get(idCodiceAssenza));
					absence.absenceType = absenceType;	
					absenceType.code = rs.getString("Codice");
					absenceType.description = rs.getString("Descrizione");
					if(rs.getByte("IgnoraTimbr")==0)
						absenceType.ignoreStamping = false;
					else 
						absenceType.ignoreStamping = true;
									
					em.persist(absenceType);					
					em.persist(absence);
					if(rs.getString("Gruppo")!=null){
					
						absTypeGroup = new AbsenceTypeGroup();
						absenceType.absenceTypeGroup = absTypeGroup;
						if(rs.getByte("MinutiEccesso")==0)
							absTypeGroup.minutesExcess = false;
						else 
							absTypeGroup.minutesExcess= true;
						absTypeGroup.equivalentCode = rs.getString("CodiceSost");
						absTypeGroup.buildUp = rs.getInt("Accumulo");
						absTypeGroup.buildUpLimit = rs.getInt("Limite");				
										
						em.persist(absTypeGroup);
					}
				}
						
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
					Logger.debug("Termino di creare le assenze");
					em.persist(dailyAbsenceType);
				}
					
			}
		}	
		mysqlCon.close();
	}
	
	public static void createVacations(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su Orario per popolare PersonVacation
		 */
		Logger.warn("Inizio a creare le ferie");
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM Orario WHERE TipoGiorno = 32 and id=" + id);
		ResultSet rs = stmt.executeQuery();
		PersonVacation personVacation = null;

		try{
			if(rs != null){				
				while(rs.next()){	

					personVacation = new PersonVacation();
					personVacation.person = person;
					personVacation.vacationDay = rs.getDate("Giorno");
					em.persist(personVacation);
				}
			}
		}
		catch(SQLException sqle) {				
				sqle.printStackTrace();
				Logger.warn("Ferie errate. Persona con id="+id);				
		}			
		mysqlCon.close();
	}
	
	public static void createVacationType(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.warn("Inizio a creare i periodi di ferie");
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * " +
				"FROM ferie f,ferie_pers fp " +
				"WHERE f.id=fp.fid AND fp.pid = " +id);
		ResultSet rs = stmt.executeQuery();
		
		VacationPeriod vacationPeriod = null;
		VacationCode vacationCode = null;
		try{
			if(rs != null){
				while(rs.next()){
					int idCodiciFerie = rs.getInt("id");
					Logger.warn("l'id del tipo ferie è questo: " + rs.getInt("id"));
					Logger.warn("Ed è relativo alla persona con id= "+ id);
					Logger.warn("Nella mappacodici l'id relativo al codiceferie "+ idCodiciFerie + " è " + mappaCodiciVacationType.get(idCodiciFerie));
					if(mappaCodiciVacationType.get(idCodiciFerie)==null){
						//Logger.warn("Nella mappacodici l'id relativo al codiceferie "+ idCodiciFerie + "è " + mappaCodici.get(idCodiciFerie));
						vacationCode = new VacationCode();
						Logger.warn("Creo un nuovo vacation code perchè nella mappa il codice ferie non era presente");

						vacationCode.description = rs.getString("nome");
						vacationCode.vacationDays = rs.getInt("giorni_ferie");
						vacationCode.permissionDays = rs.getInt("giorni_pl");
					
						em.persist(vacationCode);
						Logger.warn("Il valore del vacation code appena creato è "+ vacationCode.id);
						mappaCodiciVacationType.put(idCodiciFerie,vacationCode.id);
						
						vacationPeriod = new VacationPeriod();
						vacationPeriod.vacationCode = vacationCode;
						vacationPeriod.person = person;
						vacationPeriod.beginFrom = rs.getDate("data_inizio");
						vacationPeriod.endsTo = rs.getDate("data_fine");
						em.persist(vacationPeriod);				
						
					}
					else{
						Logger.warn("Il codice era presente, devo quindi fare una find per recuperare l'oggetto vacationCode");
						vacationCode = VacationCode.findById(mappaCodiciVacationType.get(idCodiciFerie));
									
						Logger.warn("Faccio la query sui vacationPeriod...");
						
							vacationPeriod = new VacationPeriod();
							vacationPeriod.vacationCode = vacationCode;
							vacationPeriod.person = person;
							vacationPeriod.beginFrom = rs.getDate("data_inizio");
							vacationPeriod.endsTo = rs.getDate("data_fine");
							em.persist(vacationPeriod);
					}
										
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			Logger.error("Periodi di ferie errati. Persona con id="+id);			
		}
		mysqlCon.close();
	}
	
	
	public static void createWorkingTimeTypes(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su orari di lavoro in join con orario pers e Persone
		 * per popolare workin_time_type e working_time_type_days
		 */
		Logger.debug("Inizio a creare l'orario di lavoro");
		Connection mysqlCon = getMysqlConnection();		
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM orari_di_lavoro,orario_pers WHERE " +
				"orario_pers.oid=orari_di_lavoro.id and orario_pers.pid="+id);

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
				int idCodiceOrarioLavoro = rs.getInt("id");
				if(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro)!=null){
					wtt = WorkingTimeType.findById(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro));
					
					person.workingTimeType = wtt;
				
					em.persist(wtt);
					
					wttd_mo = new WorkingTimeTypeDay();
					wttd_mo.workingTimeType = wtt;
					wttd_mo.dayOfWeek = 1;
					//wttd_mo.dayOfWeek = DateTimeConstants.MONDAY;
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
					wttd_tu.dayOfWeek = 2;
					//wttd_mo.dayOfWeek = DateTimeConstants.TUESDAY;
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
					wttd_we.dayOfWeek = 3;
					//wttd_mo.dayOfWeek = DateTimeConstants.WEDNESDAY;
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
					wttd_th.dayOfWeek = 4;
					//wttd_mo.dayOfWeek = DateTimeConstants.THURSDAY;
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
					wttd_fr.dayOfWeek = 5;
					//wttd_mo.dayOfWeek = DateTimeConstants.FRIDAY;
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
					wttd_sa.dayOfWeek = 6;
					//wttd_mo.dayOfWeek = DateTimeConstants.SATURDAY;
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
					wttd_su.dayOfWeek = 7;
					//wttd_mo.dayOfWeek = DateTimeConstants.SUNDAY;
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
				else{
					wtt = new WorkingTimeType();
					wtt.description = rs.getString("nome");
					
					wtt.shift = rs.getBoolean("turno");
					person.workingTimeType = wtt;
					em.persist(wtt);
					
					mappaCodiciWorkingTimeType.put(idCodiceOrarioLavoro,wtt.id);
					
					wttd_mo = new WorkingTimeTypeDay();
					wttd_mo.workingTimeType = wtt;
					wttd_mo.dayOfWeek = 1;
					//wttd_mo.dayOfWeek = DateTimeConstants.MONDAY;
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
					wttd_tu.dayOfWeek = 2;
					//wttd_mo.dayOfWeek = DateTimeConstants.TUESDAY;
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
					wttd_we.dayOfWeek = 3;
					//wttd_mo.dayOfWeek = DateTimeConstants.WEDNESDAY;
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
					wttd_th.dayOfWeek = 4;
					//wttd_mo.dayOfWeek = DateTimeConstants.THURSDAY;
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
					wttd_fr.dayOfWeek = 5;
					//wttd_mo.dayOfWeek = DateTimeConstants.FRIDAY;
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
					wttd_sa.dayOfWeek = 6;
					//wttd_mo.dayOfWeek = DateTimeConstants.SATURDAY;
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
					wttd_su.dayOfWeek = 7;
					//wttd_mo.dayOfWeek = DateTimeConstants.SUNDAY;
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
		mysqlCon.close();
	}
	
	public static void createYearRecap(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Logger.debug("Inizio a creare il riepilogo annuale");
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
				
				em.persist(yearRecap);			
			}
			Logger.debug("Termino di creare il riepilogo annuale");
			
		}
		mysqlCon.close();
	}
	
	public static void createMonthRecap(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su totali_mens per recueperare lo storico mensile da mettere su monthRecap
		 */
		Logger.debug("Inizio a creare il riepilogo mensile");
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

				em.persist(monthRecap);
			}
			Logger.debug("Termino di creare il riepilogo mensile");
			
		}
		mysqlCon.close();
	}
	
	public static void createCompetence(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		/**
		 * funzione che riempe la tabella competence e la tabella competence_code relativamente alle competenze
		 * di una determinata persona
		 */
		Logger.debug("Inizio a creare le competenze");
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("Select codici_comp.id, competenze.mese, " +
				"competenze.anno, competenze.codice, competenze.valore, codici_comp.descrizione, codici_comp.inattivo " +
				"from competenze, codici_comp where codici_comp.codice=competenze.codice and competenze.id= "+id);
		ResultSet rs = stmt.executeQuery();
		
		Competence competence = null;
		CompetenceCode competenceCode = null;
		
		while(rs.next()){			
			competence = new Competence();
			competence.person = person;
			competence.value = rs.getInt("valore");
			competence.code = rs.getString("codice");
			competence.month = rs.getInt("mese");
			competence.year = rs.getInt("anno");
			int idCodiciCompetenza = rs.getInt("id");	
			if(mappaCodiciCompetence.get(idCodiciCompetenza)== null){
				competenceCode = new CompetenceCode();
				competenceCode.description = rs.getString("descrizione");
				
				if(rs.getByte("inattivo")==0)
					competenceCode.inactive = false;
				else 
					competenceCode.inactive = true;
				em.persist(competenceCode);
				em.persist(competence);

				mappaCodiciCompetence.put(idCodiciCompetenza,competenceCode.id);
				
			}
			else{
			
				competenceCode = CompetenceCode.findById(mappaCodiciCompetence.get(idCodiciCompetenza));
				competence.competenceCode = competenceCode;				
				competenceCode.description = rs.getString("descrizione");
				
				if(rs.getByte("inattivo")==0)
					competenceCode.inactive = false;
				else 
					competenceCode.inactive = true;
				Logger.debug("Termino di creare le competenze");
				em.persist(competenceCode);
				em.persist(competence);
			}
			
		}	
		mysqlCon.close();
		
	}
	
}
