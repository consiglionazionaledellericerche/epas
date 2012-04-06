package it.cnr.iit.epas;

import java.io.IOException;
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
import java.util.Map;

import javax.persistence.EntityManager;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;

import models.ConfParameters;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonVacation;
import models.StampModificationType;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.VacationCode;
import models.VacationPeriod;
import models.ValuableCompetence;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.YearRecap;
import net.sf.oval.constraint.Email;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;

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
	
	@SuppressWarnings("deprecation")
	public static void createParameters() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException{
		EntityManager em = JPA.em();
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtParam = mysqlCon.prepareStatement("SELECT * FROM parametri ORDER BY data_inizio DESC limit 1");
		ResultSet rsParam = stmtParam.executeQuery();
		ConfParameters parameters = null;
		while(rsParam.next()){			

			String blob = rsParam.getString("valore");
			int lunghezza = blob.length();
			System.out.println("lunghezza di blob = " +lunghezza);			
			
			int i = 0;
			while(i < lunghezza){
				String value = "";
				String desc = "";
				if(blob.charAt(i)=='$' || blob.charAt(i)=='%'){					
					int conta = i++;					
					
					while(blob.charAt(conta)!='='){
						desc = desc+blob.charAt(conta);
						conta++;
					}
					parameters = new ConfParameters();
					if(desc.charAt(0)=='$' || desc.charAt(0)=='%')
						parameters.description  = desc.substring(1, desc.length()-1);
					else
						parameters.description = desc;	
					Timestamp dataprev = rsParam.getTimestamp("data");
					parameters.date = new LocalDate(dataprev.getYear(),dataprev.getMonth(),dataprev.getDay());
					int nuovo = conta++;
					
					if(blob.charAt(nuovo)=='='){
						nuovo++;
						while(blob.charAt(nuovo)!=';'){
							value = value+blob.charAt(nuovo);
							nuovo++;
						}
						if(value.charAt(0)=='"')
							parameters.value = value.substring(1, value.length()-1);
						else
							parameters.value = value;
					}
					em.persist(parameters);
				}
							
				i++;
				
			}
			
		}		
		
	}
	
	public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		EntityManager em = JPA.em();

		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, passwordmd5, Dipartimento, Sede " +
				"FROM Persone order by ID");
		ResultSet rs = stmt.executeQuery();
		
		while(rs.next()){
			Logger.warn("Creazione delle info per la persona: "+rs.getString("Nome").toString()+" "+rs.getString("Cognome").toString());
			//rs.next(); // exactly one result so allowed 
					
			Person person = FromMysqlToPostgres.createPerson(rs, em);
			
			FromMysqlToPostgres.createContactData(rs, person, em);

			FromMysqlToPostgres.createValuableCompetence(rs.getInt("Matricola"), person, em);
			
			FromMysqlToPostgres.createContract(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacations(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacationType(rs.getLong("ID"), person, em);
	
			FromMysqlToPostgres.createAbsences(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createWorkingTimeTypes(rs.getLong("ID"), em);
			
			FromMysqlToPostgres.createStampings(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createYearRecap(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createMonthRecap(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createCompetence(rs.getLong("ID"), person, em);
			
			
		}
	}
	
	public static void importNotInOldDb(){
		EntityManager em = JPA.em();
		FromMysqlToPostgres.createStampModificationType(em);
	}
	
	@SuppressWarnings("unused")
	public static Person createPerson(ResultSet rs, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.configuredManually = true;
		Logger.info("Inizio a creare la persona: "+rs.getString("Nome").toString()+" "+rs.getString("Cognome").toString());
		
		long id = rs.getLong("ID");
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtWorkingTime = mysqlCon.prepareStatement("select * from orari_di_lavoro,orario_pers " +
				" WHERE orario_pers.oid=orari_di_lavoro.id and orario_pers.pid = " + id + " order by data_fine desc limit 1");

		ResultSet rsInterno = stmtWorkingTime.executeQuery();		
		Person person = new Person();
		person.name = rs.getString("Nome");
		person.surname = rs.getString("Cognome");
		person.username = String.format("%s.%s", person.name.toLowerCase(), person.surname.toLowerCase() );
		person.password = rs.getString("passwordmd5");
		person.bornDate = rs.getDate("DataNascita");
		person.number = rs.getInt("Matricola");
		int i = 0;
		
		WorkingTimeType wtt = null;
		if(rsInterno.next()){
//		while(rsInterno.next()){
//			i++;
//		}
		//Logger.info("l'id dell'orario di lavoro è: " +rsInterno.getInt("id"));
			int idCodiceOrarioLavoro = rsInterno.getInt("id");
			if(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro)!=null){
				wtt = WorkingTimeType.findById(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro));
				person.workingTimeType = wtt;
				em.persist(wtt);
			}
			else{
				wtt = new WorkingTimeType();
				wtt.description = rsInterno.getString("nome");
				wtt.shift = rsInterno.getBoolean("turno");
				person.workingTimeType=wtt;
				em.persist(wtt);		
				mappaCodiciWorkingTimeType.put(idCodiceOrarioLavoro,wtt.id);
			}
			
		}
		else{
			Logger.info("Sono nel ramo else della costruzione del workingtimetype");
			Logger.info("Non ho trovato un workingTimeType valido nel db mysql");
			if(mappaCodiciWorkingTimeType.get(100)==null){
				wtt = new WorkingTimeType();
				wtt.description = "normale-mod";
				wtt.shift = false;
				em.persist(wtt);
				person.workingTimeType=wtt;
				
				mappaCodiciWorkingTimeType.put(100,wtt.id);
				
			}
			else{
				wtt = WorkingTimeType.findById(mappaCodiciWorkingTimeType.get(100));
				person.workingTimeType = wtt;
				
			}					
		}
		em.persist(person);
		
		Location location = new Location();
		location.person = person;
		
		location.department = rs.getString("Dipartimento");
		location.headOffice = rs.getString("Sede");
		location.room = rs.getString("Stanza");		
		em.persist(location);
		
		return person;
	}
	
		
	
		
	public static void createContactData(ResultSet rs, Person person, EntityManager em) throws SQLException {
		Logger.info("Inizio a creare il contact data per " +person.name+ " " +person.surname);
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
		Logger.info("Inizio a creare il contratto per " +person.name+ " " +person.surname);
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
	

	public static void createStampings(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.info("Inizio a creare le timbrature per " +person.name+ " " +person.surname);
		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulle tabelle orario, per recuperare le info sulle timbrature
		 * di ciascuna persona
		 */
		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT ID,Giorno,TipoGiorno,TipoTimbratura,Ora " +
				"FROM Orario WHERE TipoTimbratura is not null and Giorno > '2009-12-31' " +
				"and TipoGiorno = 0 and ID = " + id);
//		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT ID,Giorno,TipoGiorno,TipoTimbratura,Ora " +
//				"FROM Orario WHERE TipoTimbratura = 1 and Giorno = '2004-07-26'" +
//				"and TipoGiorno = 0 and ID = " + id);

		ResultSet rs = stmtOrari.executeQuery();
				
		StampType stampType = null;
		Stamping stamping = null;
		byte tipoTimbratura;
		while(rs.next()){
			int idCodiceTimbratura = rs.getInt("TipoTimbratura");
			tipoTimbratura = rs.getByte("TipoTimbratura");
			if(mappaCodiciStampType.get(idCodiceTimbratura)== null){				
				
				stampType = new StampType();				
				
				if((tipoTimbratura % 2 == 1) && (tipoTimbratura / 2 == 0)){
					stampType.description = "Timbratura di ingresso";					
				}
				if((tipoTimbratura % 2 == 0) && (tipoTimbratura / 2 == 1) ){
					stampType.description = "Timbratura d'uscita per pranzo";
				}
				if((tipoTimbratura % 2 == 1) && (tipoTimbratura / 2 == 1 )){
					stampType.description = "Timbratura di ingresso dopo pausa pranzo";
				}
				if((tipoTimbratura % 2 == 0) && (tipoTimbratura / 2 == 2)){
					stampType.description = "Timbratura di uscita";
				}
//				if((tipoTimbratura % 2 == 1) && (tipoTimbratura / 2 == 2)){
//					stampType.description = "Altra timbratura di ingresso";
//				}
//				if((tipoTimbratura % 2 == 0) && (tipoTimbratura / 2 == 3)){
//					stampType.description = "Altra timbratura di uscita";
//				}
//				
				em.persist(stampType);	
				mappaCodiciStampType.put(idCodiceTimbratura,stampType.id);
			}
			else{
				stampType = StampType.findById(mappaCodiciStampType.get(idCodiceTimbratura));	
			}
					
			stamping = new Stamping();
			stamping.stampType = stampType;	
			stamping.person = person;
						
			if(tipoTimbratura % 2 != 0)
				stamping.way = WayType.in;					
			else
				stamping.way = WayType.out;
	
			LocalDate giornata = new LocalDate(rs.getDate("Giorno"));
			if(giornata != null){			
								
				try {
	
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
							/**
							 * aggiunti i campi anno mese e giorno per provare a risolvere il problema sulle date.
							 * inoltre aggiunte le set corrispondenti all'oggetto calendar creato
							 */
							//int year = giorno.getYear();
							int year = giornata.getYear();
							int month = giornata.getMonthOfYear();
							int day = giornata.getDayOfMonth();
	
			                stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
			                
			                stamping.isMarkedByAdmin = false;
			                stamping.isServiceExit = true;
			                em.persist(stamping);
						}
						else{
							
							int hour = Integer.parseInt(s.substring(0, 2));
							int minute = Integer.parseInt(s.substring(3, 5));
							int second = Integer.parseInt(s.substring(6, 8));	
							int year = giornata.getYear();
							int month = giornata.getMonthOfYear();
							int day = giornata.getDayOfMonth();
	
							/**
							 * aggiunti i campi anno mese e giorno per provare a risolvere il problema sulle date.
							 * inoltre aggiunte le set corrispondenti all'oggetto calendar creato
							 */
							if(hour > 33){
								hour = hour * 60;
								hour = hour + minute;
								hour = hour - 2000;
								int newHour = hour / 60;
								int min = hour % 60;
							    stamping.date = new LocalDateTime(year,month,day,newHour,min,second);
				                
				                stamping.isMarkedByAdmin = true;
				                stamping.isServiceExit = false;
				                em.persist(stamping);
							}						
							
							else{
								if(hour == 24){
									stamping.date = new LocalDateTime(year,month,day,0,minute,second).plusDays(1);
									stamping.isMarkedByAdmin = true;
					                stamping.isServiceExit = false;
									em.persist(stamping);
								}
								else{
									//ora = rs.getTimestamp("Ora");
									Logger.info("L'ora è: ", +hour);
					                stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
					                
					                stamping.isMarkedByAdmin = false;
					                stamping.isServiceExit = false;
					                em.persist(stamping);
								}
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
			
		}
		mysqlCon.close();
	}
	
	public static void createAbsences(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.info("Inizio a creare le assenze per "+person.name+ " " +person.surname);
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
				
				int idCodiceAssenza = rs.getInt("id");
				if(mappaCodiciAbsence.get(idCodiceAssenza)== null){
					
					absenceType = new AbsenceType();					
					absenceType.code = rs.getString("Codice");
					absenceType.description = rs.getString("Descrizione");
					if(rs.getByte("IgnoraTimbr")==0)
						absenceType.ignoreStamping = false;
					else 
						absenceType.ignoreStamping = true;					
					
					/**
					 * caso di assenze orarie
					 */
					if(rs.getInt("QuantGiust") != 0){
						absenceType.isHourlyAbsence = true;
						absenceType.isDailyAbsence = false;
						absenceType.justifiedWorkTime = rs.getInt("QuantGiust");
						
					}
					/**
					 * caso di assenze giornaliere
					 */
					else{
						absenceType.isDailyAbsence = true;
						absenceType.isHourlyAbsence = false;
						
					}				
					
					
					if(rs.getString("Gruppo")!=null){
						absTypeGroup = new AbsenceTypeGroup();						
						absTypeGroup.label = rs.getString("Gruppo");
						absTypeGroup.buildUp = rs.getInt("Accumulo");
						absTypeGroup.buildUpLimit = rs.getInt("Limite");
						absTypeGroup.equivalentCode = rs.getString("CodiceSost");
						if(rs.getByte("MinutiEccesso")==0)
							absTypeGroup.minutesExcess = false;
						else 
							absTypeGroup.minutesExcess = true; 
						em.persist(absTypeGroup);
						absenceType.absenceTypeGroup = absTypeGroup;
					}
					em.persist(absenceType);
					mappaCodiciAbsence.put(idCodiceAssenza,absenceType.id);	
					
					absence = new Absence();
					absence.person = person;
					absence.date = new LocalDate(rs.getDate("Giorno"));	
					absence.absenceType = absenceType;
					em.persist(absence);
					
				}
				else{
					absenceType = AbsenceType.findById(mappaCodiciAbsence.get(idCodiceAssenza));
					
					/**
					 * caso di assenze orarie
					 */

						
						absence = new Absence();
						absence.person = person;
						absence.date = new LocalDate(rs.getDate("Giorno"));	
						absence.absenceType = absenceType;	
						em.persist(absence);
					//}
				}			
					
			}
		}	
		mysqlCon.close();
	}
	
	public static void createVacations(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su Orario per popolare PersonVacation
		 */
		Logger.info("Inizio a creare le ferie per " +person.name+ " " +person.surname);
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
		Logger.info("Inizio a creare i periodi di ferie per " +person.name+ " " +person.surname);
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
	
	
	public static void createWorkingTimeTypes(long id, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su orari di lavoro in join con orario pers e Persone
		 * per popolare workin_time_type e working_time_type_days
		 */
		Logger.info("Inizio a creare l'orario di lavoro ");
		Connection mysqlCon = getMysqlConnection();		
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM orari_di_lavoro,orario_pers WHERE " +
				"orario_pers.oid=orari_di_lavoro.id and orario_pers.pid= " + id + " order by data_fine desc limit 1");

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
				//if(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro)!=null){
					wtt = WorkingTimeType.findById(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro));
					
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
		mysqlCon.close();
	}
	
	public static void createYearRecap(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Logger.info("Inizio a creare il riepilogo annuale per " +person.name+ " " +person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			YearRecap yearRecap = null;
			while(rs.next()){										
			
				yearRecap = new YearRecap(person, rs.getShort("anno"));
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
		Logger.info("Inizio a creare il riepilogo mensile per " +person.name+ " " +person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_mens WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			MonthRecap monthRecap = null;
			while(rs.next()){
				
				monthRecap = new MonthRecap(person, rs.getInt("anno"), rs.getInt("mese"));
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
		Logger.info("Inizio a creare le competenze per " +person.name+ " " +person.surname);
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
	
	/**
	 * 
	 * @param matricola
	 * @param person
	 * @param em
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * funzione che associa a ciascuna persona creata, le corrispettive competenze valide.
	 */
	public static void createValuableCompetence(int matricola, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.info("Inizio a creare le competenze valide per " +person.name+ " " +person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT codicecomp, descrizione FROM compvalide " +
				"WHERE matricola = " +matricola );
		ResultSet rs = stmt.executeQuery();
		ValuableCompetence valuableCompetence = null;
		while(rs.next()){
			valuableCompetence = new ValuableCompetence();
			valuableCompetence.person = person;
			valuableCompetence.codicecomp = rs.getString("codicecomp");
			valuableCompetence.descrizione = rs.getString("descrizione");
			em.persist(valuableCompetence);
		}
		mysqlCon.close();
	}
	
	/**
	 * funzione di popolamento provvisoria per la stampmodificationtype
	 * @param em
	 */
	public static void createStampModificationType(EntityManager em){
		Logger.info("Inizio a creare le modification type ");
		StampModificationType smt1 = new StampModificationType();
		smt1.code = "p";
		smt1.description = "Tempo calcolato togliendo dal tempo di lavoro la durata dell'intervallo pranzo";
		em.persist(smt1);
		StampModificationType smt2 = new StampModificationType();
		smt2.code = "e";
		smt2.description = "Ora di entrata calcolata perché la durata dell'intervallo pranzo è minore del minimo";
		em.persist(smt2);
		StampModificationType smt3 = new StampModificationType();
		smt3.code = "";
		smt3.description = "";
		em.persist(smt3);		
		
	}
	
}
