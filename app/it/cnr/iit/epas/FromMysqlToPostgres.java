package it.cnr.iit.epas;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;

import models.Configuration;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonVacation;
import models.Qualification;
import models.StampModificationType;
import models.StampModificationTypeValue;
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

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
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
	public static Map<Integer,Long> mappaCodiciQualification = new HashMap<Integer,Long>();
	
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
	
	/**
	 * metodo per il popolamento delle qualifiche
	 */
	public static void createQualification(){
		int i = 1;
		while(i < 10){
			Qualification qual = new Qualification();
			qual.qualification = i;
			qual.save();
			i++;
		}
		
	}	
	
	/**
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * metodo per il popolamento delle absenceType
	 * 
	 */
	public static void createAbsenceType() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtParam = mysqlCon.prepareStatement("Select Codici.Qualifiche, " +
				"Codici.Codice, Codici.Interno, Codici.Descrizione, Codici.QuantGiust, Codici.IgnoraTimbr, Codici.MinutiEccesso, " +
				"Codici.Limite, Codici.Accumulo, Codici.CodiceSost, Codici.id, Codici.Gruppo, Codici.DataInizio, Codici.DataFine " +
				"from Codici " +
				"where Codici.id != 0");
		ResultSet rsParam = stmtParam.executeQuery();
		
		
		while(rsParam.next()){
			AbsenceType absenceType = new AbsenceType();
				
				absenceType.code = rsParam.getString("Codice");
				absenceType.description = rsParam.getString("Descrizione");
				absenceType.validFrom = rsParam.getDate("DataInizio");
				absenceType.validTo = rsParam.getDate("DataFine");
				if(rsParam.getByte("Interno")==0)
					absenceType.internalUse = false;
				else
					absenceType.internalUse = true;
				if(rsParam.getByte("IgnoraTimbr")==0)
					absenceType.ignoreStamping = false;
				else 
					absenceType.ignoreStamping = true;	
				
				
				/**
				 * caso di assenze orarie
				 */
				if(rsParam.getInt("QuantGiust") != 0){
					absenceType.isHourlyAbsence = true;
					absenceType.isDailyAbsence = false;
					absenceType.justifiedWorkTime = rsParam.getInt("QuantGiust");
					
				}
				/**
				 * caso di assenze giornaliere
				 */
				else{
					absenceType.isDailyAbsence = true;
					absenceType.isHourlyAbsence = false;
					
				}				
				
				if(rsParam.getString("Gruppo")!=null){
					AbsenceTypeGroup absTypeGroup = new AbsenceTypeGroup();
										
					absTypeGroup.label = rsParam.getString("Gruppo");
					absTypeGroup.buildUp = rsParam.getInt("Accumulo");
					absTypeGroup.buildUpLimit = rsParam.getInt("Limite");
					absTypeGroup.equivalentCode = rsParam.getString("CodiceSost");
					if(rsParam.getByte("MinutiEccesso")==0)
						absTypeGroup.minutesExcess = false;
					else 
						absTypeGroup.minutesExcess = true; 
					absTypeGroup.save();
					absenceType.absenceTypeGroup = absTypeGroup;
				}
							
				absenceType.save();				
		
			}
	}
	
	/**
	 * metodo per la giunzione tra absenceType e Qualifications
	 */
	public static void joinTables(){
		List<AbsenceType> listaAssenze = AbsenceType.findAll();
		Logger.info("ListaAssenze è lunga: "+listaAssenze.size());
		for(AbsenceType absenceType : listaAssenze){
			
			if(absenceType.code.equals("OA1") || absenceType.code.equals("OA2") || absenceType.code.equals("OA3") 
					|| absenceType.code.equals("OA4") || absenceType.code.equals("OA5") || absenceType.code.equals("OA6")
					|| absenceType.code.equals("OA7")){
				long id1 = 1;
				long id2 = 2;
				long id3 = 3;
				
				Qualification qual1 = Qualification.findById(id1);
				Logger.info("La qualifica 1: ", qual1.qualification);
				Qualification qual2 = Qualification.findById(id2);
				Qualification qual3 = Qualification.findById(id3);
				if(absenceType.qualifications == null){
					absenceType.qualifications = new ArrayList<Qualification>();
					absenceType.qualifications.add(qual1);

					absenceType.qualifications.add(qual2);
					absenceType.qualifications.add(qual3);
					
				}
				
				absenceType.save();
			}
			else{
				
				List<Qualification> listaQual = Qualification.findAll();
				
				for(Qualification qual : listaQual){
					if(absenceType.qualifications == null){
						absenceType.qualifications = new ArrayList<Qualification>();
						absenceType.qualifications.add(qual);
					}					

					absenceType.save();
				}
				
			}
		}		
		
	}
	
	public static void importAll() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		EntityManager em = JPA.em();

		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, passwordmd5, Qualifica, Dipartimento, Sede " +
				"FROM Persone p order by ID");
		ResultSet rs = stmt.executeQuery();
		
		while(rs.next()){
			Logger.info("Creazione delle info per la persona: %s %s", rs.getString("Nome"), rs.getString("Cognome"));
			//rs.next(); // exactly one result so allowed 
					
			Person person = FromMysqlToPostgres.createPerson(rs, em);
			
			FromMysqlToPostgres.createContactData(rs, person, em);

			FromMysqlToPostgres.createValuableCompetence(rs.getInt("Matricola"), person, em);
			
			FromMysqlToPostgres.createContract(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacations(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createVacationType(rs.getLong("ID"), person, em);
	
//			FromMysqlToPostgres.createAbsences(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createStampings(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createYearRecap(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createMonthRecap(rs.getLong("ID"), person, em);
			
			FromMysqlToPostgres.createCompetence(rs.getLong("ID"), person, em);		
			
		}
		//PopulatePersonDay.fillWorkingTimeTypeDays();
		//PopulatePersonDay.fillPersonDay();
	}
	
	public static void importNotInOldDb(){
		EntityManager em = JPA.em();
		FromMysqlToPostgres.createStampModificationType(em);
	}
	
	@SuppressWarnings("unused")
	public static Person createPerson(ResultSet rs, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.info("Inizio a creare la persona %s %s", rs.getString("Nome"), rs.getString("Cognome"));
		
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
		WorkingTimeTypeDay wttd_mo = null;
		WorkingTimeTypeDay wttd_tu = null;
		WorkingTimeTypeDay wttd_we = null;
		WorkingTimeTypeDay wttd_th = null;
		WorkingTimeTypeDay wttd_fr = null;
		WorkingTimeTypeDay wttd_sa = null;
		WorkingTimeTypeDay wttd_su = null;
		if(rsInterno.next()){

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
				
				wttd_mo = new WorkingTimeTypeDay();
				wttd_mo.workingTimeType = wtt;
				wttd_mo.dayOfWeek = 1;
				wttd_mo.workingTime = rsInterno.getInt("lu_tempo_lavoro");
				wttd_mo.holiday = rsInterno.getBoolean("lu_festa");
				wttd_mo.timeSlotEntranceFrom = rsInterno.getInt("lu_fascia_ingresso");
				wttd_mo.timeSlotEntranceTo = rsInterno.getInt("lu_fascia_ingresso1");
				wttd_mo.timeSlotExitFrom = rsInterno.getInt("lu_fascia_uscita");
				wttd_mo.timeSlotExitTo = rsInterno.getInt("lu_fascia_uscita1");
				wttd_mo.timeMealFrom = rsInterno.getInt("lu_fascia_pranzo");
				wttd_mo.timeMealTo = rsInterno.getInt("lu_fascia_pranzo1");
				wttd_mo.breakTicketTime = rsInterno.getInt("lu_tempo_interv"); 
				wttd_mo.mealTicketTime = rsInterno.getInt("lu_tempo_buono");
				em.persist(wttd_mo);

				wttd_tu = new WorkingTimeTypeDay();
				wttd_tu.workingTimeType = wtt;
				wttd_tu.dayOfWeek = 2;
				wttd_tu.workingTime = rsInterno.getInt("ma_tempo_lavoro");
				wttd_tu.holiday = rsInterno.getBoolean("ma_festa");
				wttd_tu.timeSlotEntranceFrom = rsInterno.getInt("ma_fascia_ingresso");
				wttd_tu.timeSlotEntranceTo = rsInterno.getInt("ma_fascia_ingresso1");
				wttd_tu.timeSlotExitFrom = rsInterno.getInt("ma_fascia_uscita");
				wttd_tu.timeSlotExitTo = rsInterno.getInt("ma_fascia_uscita1");
				wttd_tu.timeMealFrom = rsInterno.getInt("ma_fascia_pranzo");
				wttd_tu.timeMealTo = rsInterno.getInt("ma_fascia_pranzo1");
				wttd_tu.breakTicketTime = rsInterno.getInt("ma_tempo_interv"); 
				wttd_tu.mealTicketTime = rsInterno.getInt("ma_tempo_buono"); 
				em.persist(wttd_tu);

				wttd_we = new WorkingTimeTypeDay();
				wttd_we.workingTimeType = wtt;
				wttd_we.dayOfWeek = 3;
				wttd_we.workingTime = rsInterno.getInt("me_tempo_lavoro");
				wttd_we.holiday = rsInterno.getBoolean("me_festa");
				wttd_we.timeSlotEntranceFrom = rsInterno.getInt("me_fascia_ingresso");
				wttd_we.timeSlotEntranceTo = rsInterno.getInt("me_fascia_ingresso1");
				wttd_we.timeSlotExitFrom = rsInterno.getInt("me_fascia_uscita");
				wttd_we.timeSlotExitTo = rsInterno.getInt("me_fascia_uscita1");
				wttd_we.timeMealFrom = rsInterno.getInt("me_fascia_pranzo");
				wttd_we.timeMealTo = rsInterno.getInt("me_fascia_pranzo1");
				wttd_we.breakTicketTime = rsInterno.getInt("me_tempo_interv"); 
				wttd_we.mealTicketTime = rsInterno.getInt("me_tempo_buono"); 
				em.persist(wttd_we);

				wttd_th = new WorkingTimeTypeDay();
				wttd_th.workingTimeType = wtt;
				wttd_th.dayOfWeek = 4;
				wttd_th.workingTime = rsInterno.getInt("gi_tempo_lavoro");
				wttd_th.holiday = rsInterno.getBoolean("gi_festa");
				wttd_th.timeSlotEntranceFrom = rsInterno.getInt("gi_fascia_ingresso");
				wttd_th.timeSlotEntranceTo = rsInterno.getInt("gi_fascia_ingresso1");
				wttd_th.timeSlotExitFrom = rsInterno.getInt("gi_fascia_uscita");
				wttd_th.timeSlotExitTo = rsInterno.getInt("gi_fascia_uscita1");
				wttd_th.timeMealFrom = rsInterno.getInt("gi_fascia_pranzo");
				wttd_th.timeMealTo = rsInterno.getInt("gi_fascia_pranzo1");
				wttd_th.breakTicketTime = rsInterno.getInt("me_tempo_interv"); 
				wttd_th.mealTicketTime = rsInterno.getInt("me_tempo_buono"); 
				em.persist(wttd_th);

				wttd_fr = new WorkingTimeTypeDay();
				wttd_fr.workingTimeType = wtt;
				wttd_fr.dayOfWeek = 5;
				wttd_fr.workingTime = rsInterno.getInt("ve_tempo_lavoro");
				wttd_fr.holiday = rsInterno.getBoolean("ve_festa");
				wttd_fr.timeSlotEntranceFrom = rsInterno.getInt("ve_fascia_ingresso");
				wttd_fr.timeSlotEntranceTo = rsInterno.getInt("ve_fascia_ingresso1");
				wttd_fr.timeSlotExitFrom = rsInterno.getInt("ve_fascia_uscita");
				wttd_fr.timeSlotExitTo = rsInterno.getInt("ve_fascia_uscita1");
				wttd_fr.timeMealFrom = rsInterno.getInt("ve_fascia_pranzo");
				wttd_fr.timeMealTo = rsInterno.getInt("ve_fascia_pranzo1");
				wttd_fr.breakTicketTime = rsInterno.getInt("me_tempo_interv"); 
				wttd_fr.mealTicketTime = rsInterno.getInt("me_tempo_buono"); 
				em.persist(wttd_fr);

				wttd_sa = new WorkingTimeTypeDay();
				wttd_sa.workingTimeType = wtt;
				wttd_sa.dayOfWeek = 6;
				wttd_sa.workingTime = rsInterno.getInt("sa_tempo_lavoro");
				wttd_sa.holiday = rsInterno.getBoolean("sa_festa");
				wttd_sa.timeSlotEntranceFrom = rsInterno.getInt("sa_fascia_ingresso");
				wttd_sa.timeSlotEntranceTo = rsInterno.getInt("sa_fascia_ingresso1");
				wttd_sa.timeSlotExitFrom = rsInterno.getInt("sa_fascia_uscita");
				wttd_sa.timeSlotExitTo = rsInterno.getInt("sa_fascia_uscita1");
				wttd_sa.timeMealFrom = rsInterno.getInt("sa_fascia_pranzo");
				wttd_sa.timeMealTo = rsInterno.getInt("sa_fascia_pranzo1");
				wttd_sa.breakTicketTime = rsInterno.getInt("me_tempo_interv");
				wttd_sa.mealTicketTime = rsInterno.getInt("me_tempo_buono"); 
				em.persist(wttd_sa);

				wttd_su = new WorkingTimeTypeDay();		
				wttd_su.workingTimeType = wtt;
				wttd_su.dayOfWeek = 7;
				wttd_su.workingTime = rsInterno.getInt("do_tempo_lavoro");
				wttd_su.holiday = rsInterno.getBoolean("do_festa");
				wttd_su.timeSlotEntranceFrom = rsInterno.getInt("do_fascia_ingresso");
				wttd_su.timeSlotEntranceTo = rsInterno.getInt("do_fascia_ingresso1");
				wttd_su.timeSlotExitFrom = rsInterno.getInt("do_fascia_uscita");
				wttd_su.timeSlotExitTo = rsInterno.getInt("do_fascia_uscita1");
				wttd_su.timeMealFrom = rsInterno.getInt("do_fascia_pranzo");
				wttd_su.timeMealTo = rsInterno.getInt("do_fascia_pranzo1");
				wttd_su.breakTicketTime = rsInterno.getInt("me_tempo_interv");
				wttd_su.mealTicketTime = rsInterno.getInt("me_tempo_buono");
				em.persist(wttd_su);
			}
			
		}
		else{
			Logger.trace("Non ho trovato un workingTimeType valido nel db mysql");
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
		Logger.info("Inizio a creare il contact data per %s %s", person.name, person.surname);
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
	
        Logger.info("Inizio a creare il contratto per %s %s", person.name, person.surname);	
        Connection mysqlCon = getMysqlConnection();	
        PreparedStatement stmtContratto = mysqlCon.prepareStatement("SELECT id,DataInizio,DataFine,continua " +	
                        "FROM Personedate WHERE id=" + id + " order by DataFine");	
        ResultSet rs = stmtContratto.executeQuery();       	
        Contract contract = null;	
        while(rs.next()){
        	
        	if (contract != null) {
				em.remove(contract);
				em.flush();
			}        	
        	contract = new Contract();       	
        	/**
    		 * non esistono contratti per quella persona nel db, questo è il primo
    		 */
    		contract = new Contract();
    		contract.person = person;
    		Date begin = rs.getDate("DataInizio");
    		Date end = rs.getDate("DataFine");    				
    		
    		if(begin == null && end == null){
    			/**
    			 * le date non sono valorizzate, si costruisce un contratto con date fittizie
    			 */
    			contract.beginContract = new LocalDate(1971,12,31);
    			contract.endContract = new LocalDate(2099,1,1);
    		}
    			
    		if(begin != null && end == null){
    			/**
    			 * è il caso dei contratti a tempo indeterminato che non hanno data di fine valorizzata. posso lasciarla anche
    			 * io a null
    			 */
    			contract.beginContract = new LocalDate(begin);
    			contract.endContract = null;
    		}
    		if(begin == null && end != null){
    			contract.beginContract = new LocalDate(1971,12,31);
    			contract.endContract = new LocalDate(end);
    		}
    		
    		if(begin != null && end != null){
    			/**
    			 * entrambi gli estremi valorizzati, contratto a tempo determinato, si inseriscono entrambe
    			 */
    			contract.beginContract = new LocalDate(begin);
    			contract.endContract = new LocalDate(end);
    		}
    		
    		if(rs.getByte("continua")==0)
    			contract.isContinued = false;
    		else
    			contract.isContinued = true;
    		contract.save();
    		    	     
             
        }
        mysqlCon.close();
	
    }
	

	public static void createStampings(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.info("Inizio a creare le timbrature per %s %s", person.name, person.surname);
		Connection mysqlCon = getMysqlConnection();
		
		/**
		 * query sulle tabelle orario, per recuperare le info sulle timbrature
		 * di ciascuna persona
		 */
		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT Orario.ID,Orario.Giorno,Orario.TipoGiorno,Orario.TipoTimbratura," +
				"Orario.Ora, Codici.id, Codici.Codice, Codici.Qualifiche " +
				"FROM Orario, Codici " +
				"WHERE Orario.TipoGiorno=Codici.id and Orario.Giorno > '2009-12-31' " +
				"and Orario.ID = " + id + "ORDER BY Orario.Giorno");

		ResultSet rs = stmtOrari.executeQuery();
				
		StampType stampType = null;
		PersonDay pd = null;
		Absence absence = null;
		AbsenceType absenceType = null;
		Stamping stamping = null;
		LocalDate data = null;
		LocalDate newData = null;
		byte tipoTimbratura;
		while(rs.next()){
			
			/**
			 * mi serve la data per fare i controlli sul calcolo delle info del personDay...non appena la data cambia, faccio i calcoli sul
			 * personday del giorno precedente
			 */
			newData = new LocalDate(rs.getDate("Giorno"));
			if(data != null){
				if(newData.isAfter(data)){				
					
					/**
					 * TODO: in questo caso la data del "giro successivo" è maggiore della data alla fine del giro precedente. Quindi bisogna fare 
					 * i calcoli del personDay relativi al giorno precedente (quello con date = data) e proseguire nell'elaborazione
					 */
					PersonDay pdYesterday = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.data = ?", person, data).first();
					pdYesterday.timeAtWork = pdYesterday.timeAtWork();
					pdYesterday.difference = pdYesterday.getDifference();
					pdYesterday.progressive = pdYesterday.getProgressive();
					pdYesterday.setTicketAvailable();
					pdYesterday.save();
				}
			}
			if(data == null || newData.isEqual(data)){
				/**
				 * si tratta di timbratura
				 */
				if(rs.getInt("TipoGiorno")==0){
					pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person,data).first();
					if(pd == null)
						pd = new PersonDay(person,data);
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

						em.persist(stampType);	
						mappaCodiciStampType.put(idCodiceTimbratura,stampType.id);
					}
					else{
						stampType = StampType.findById(mappaCodiciStampType.get(idCodiceTimbratura));	
					}
							
					stamping = new Stamping();
					stamping.stampType = stampType;	
								
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
								stamping.markedByAdmin = false;
								stamping.serviceExit = false;
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
									
									int year = giornata.getYear();
									int month = giornata.getMonthOfYear();
									int day = giornata.getDayOfMonth();
			
					                stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
					                
					                stamping.markedByAdmin = false;
					                stamping.serviceExit = true;
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
						                
						                stamping.markedByAdmin = true;
						                stamping.serviceExit = false;
						                em.persist(stamping);
									}						
									
									else{
										if(hour == 24){
											stamping.date = new LocalDateTime(year,month,day,0,minute,second).plusDays(1);
											stamping.markedByAdmin = true;
							                stamping.serviceExit = false;
											em.persist(stamping);
										}
										else{
											
											Logger.trace("L'ora è: ", +hour);
							                stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
							                
							                stamping.markedByAdmin = false;
							                stamping.serviceExit = false;
							                em.persist(stamping);
										}
									}
								}
							}
						} catch (SQLException sqle) {					
							sqle.printStackTrace();
							Logger.warn("Timbratura errata. Persona con id = %s", id);
						}			
						
						stamping.personDay = pd;
						em.persist(stamping);	
						
					}
					
				}
				/**
				 * si tratta di assenza
				 */
				else{
					pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, data).first();
					if(pd == null)
						pd = new PersonDay(person,data);
					String codice = rs.getString("Codice");
					absenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", codice).first();
					if(absenceType.isDailyAbsence==true){
						pd.difference += 0;
						pd.progressive += 0;
						pd.timeAtWork += 0;
					}
					else{
						int justified = absenceType.justifiedWorkTime;
						pd.timeAtWork = pd.timeAtWork-justified;
						pd.difference = pd.difference-justified;
						pd.progressive = pd.progressive-justified;
					}
					pd.save();
					absence = new Absence();
					absence.personDay = pd;
					absence.date = data;	
					absence.absenceType = absenceType;
					em.persist(absence);
					//em.persist(pd);
				}	
				data = new LocalDate(rs.getDate("Giorno"));
			}
						
		}
		Logger.debug("Termino di creare le timbrature. Person con id = %s", id);

		mysqlCon.close();
	}
	
	/**
	 * TODO: questo metodo va riscritto dal momento che le absenceType sono già state definite prima della procedura di importazione.
	 * Quindi occorrerà recuperare solo le assenze e associarle a absenceType già esistenti che si possono recuperare sul db grazie
	 * a semplici find
	 * 
	 * @param id
	 * @param person
	 * @param em
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
//	public static void createAbsences(long id, Person person, EntityManager em) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
//		Logger.info("Inizio a creare le assenze per %s %s",  person.name, person.surname);
//		Connection mysqlCon = getMysqlConnection();
//		
//		/**
//		 * query sulla tabelle Orario e Codici per recuperare le info sulle assenze e i motivi delle
//		 * assenze di ciascuna persona. La query prende tutti i codici di assenza che vengono poi "smistati"
//		 * nella tabella di postgres corrispondente attraverso l'analisi del campo QuantGiust presente soltanto 
//		 * nelle righe relative a codici di natura giornaliera.
//		 */
//		PreparedStatement stmtAssenze = mysqlCon.prepareStatement("Select Orario.Giorno, Orario.TipoTimbratura, Codici.Qualifiche, " +
//				"Codici.Codice, Codici.Interno, Codici.Descrizione, Codici.QuantGiust, Codici.IgnoraTimbr, Codici.MinutiEccesso, " +
//				"Codici.Limite, Codici.Accumulo, Codici.CodiceSost, Codici.id, Codici.Gruppo, Codici.DataInizio, Codici.DataFine " +
//				"from Codici, Orario " +
//				"where Orario.TipoGiorno=Codici.id " +
//				"and TipoGiorno !=0 and Orario.id = "+id);
//		ResultSet rs = stmtAssenze.executeQuery();		
//		
//		if(rs != null){
//			Absence absence = null;
//			AbsenceType absenceType = null;
//
//			while(rs.next()){			
//							
//				String codice = rs.getString("Codice");
//				absenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", codice).first();
//								
//				absence = new Absence();
//				absence.person = person;
//				absence.date = new LocalDate(rs.getDate("Giorno"));	
//				absence.absenceType = absenceType;
//				em.persist(absence);
//				
//			}
//		}	
//		mysqlCon.close();
//	}
	
	public static void createVacations(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		/**
		 * query su Orario per popolare PersonVacation
		 */
		Logger.info("Inizio a creare le ferie per %s %s", person.name, person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM Orario WHERE TipoGiorno = 32 and TipoGiorno = 31 and id=" + id);
		ResultSet rs = stmt.executeQuery();
		PersonVacation personVacation = null;

		try{
			if(rs != null){				
				while(rs.next()){
					personVacation = new PersonVacation(person, rs.getDate("Giorno"));
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
		Logger.info("Inizio a creare i periodi di ferie per %s %s", person.name ,person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * " +
				"FROM ferie f,ferie_pers fp " +
				"WHERE f.id=fp.fid AND fp.pid = " +id + 
				" ORDER BY data_fine");
		ResultSet rs = stmt.executeQuery();
		
		VacationPeriod vacationPeriod = null;
		VacationCode vacationCode = null;
		
		try{
			if(rs != null){
				while(rs.next()){
									
					int idCodiciFerie = rs.getInt("id");
					Logger.trace("l'id del tipo ferie è %s ed è relativo alla persona con id=%d", rs.getInt("id"), id);
					Logger.debug("Nella mappacodici l'id relativo al codiceferie "+ idCodiciFerie + " è " + mappaCodiciVacationType.get(idCodiciFerie));
					
					if (vacationPeriod != null) {
						em.remove(vacationPeriod);
						em.flush();
					}
					
					vacationPeriod = new VacationPeriod();

					if(mappaCodiciVacationType.get(idCodiciFerie)==null){
						Logger.trace("Creo un nuovo vacation code perchè nella mappa il codice ferie non era presente");

						vacationCode = new VacationCode();
						vacationCode.description = rs.getString("nome");
						vacationCode.vacationDays = rs.getInt("giorni_ferie");
						vacationCode.permissionDays = rs.getInt("giorni_pl");
					
						em.persist(vacationCode);
						Logger.debug("Creato un nuovo vacation code con id = , description = %s", vacationCode.id, vacationCode.description);
						mappaCodiciVacationType.put(idCodiciFerie,vacationCode.id);
						
					}
					else {
						Logger.trace("Il codice era presente, devo quindi fare una find per recuperare l'oggetto vacationCode");
						vacationCode = VacationCode.findById(mappaCodiciVacationType.get(idCodiciFerie));

					}
					
					vacationPeriod.vacationCode = vacationCode;
					vacationPeriod.person = person;
					vacationPeriod.beginFrom = rs.getDate("data_inizio");
					vacationPeriod.endTo = rs.getDate("data_fine");					
					em.persist(vacationPeriod);		
					
					Logger.info("Creato vacationPeriod id=%s per Person = %s con vacationCode = %s", vacationPeriod, person, vacationCode);
										
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			Logger.error("Periodi di ferie errati. Persona con id="+id);			
		}
		mysqlCon.close();
	}
	
	public static void createYearRecap(long id, Person person, EntityManager em) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Logger.info("Inizio a creare il riepilogo annuale per %s %S", person.name, person.surname);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID="+id);
		ResultSet rs = stmt.executeQuery();
		
		if(rs != null){
			YearRecap yearRecap = null;
			while(rs.next()){										
			
				short year = rs.getShort("anno");
				yearRecap = new YearRecap(person, year);
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
				
				Logger.debug("Terminato di creare il riepilogo annuale per l'anno %d per %s", year, person);
			}
			Logger.info("Terminati di creare i riepiloghi annuali per %s", person);
			
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
		Logger.info("Inizio a creare le competenze per %s %s", person.name, person.surname);
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
			/**
			 * per popolare la personReperibility controllo che quando inserisco un nuovo codice, quella persona abbia anche già
			 * una entry nella tabella PersonReperibility: in tal caso non la inserisco, altrimenti devo aggiungerla.
			 */
			String codice = rs.getString("codice");
			if(codice.equals("207") || codice.equals("208")){
				PersonReperibility personRep = PersonReperibility.find("Select pr from PersonReperibility pr where " +
						"pr.person = ?", person).first();
				if(personRep==null){
					personRep = new PersonReperibility();
					personRep.person = person;
					personRep.startDate = null;
					personRep.endDate = null;
					em.persist(personRep);
					em.flush();
				}
				
			}
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
				
			} else {
				competenceCode = CompetenceCode.findById(mappaCodiciCompetence.get(idCodiciCompetenza));
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
		Logger.debug("Terminato di creare le competenze per %s", person);
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
		Logger.info("Inizio a creare le competenze valide per %s %s", person.name, person.surname);
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
		
