package it.cnr.iit.epas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Location;
import models.Permission;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonReperibility;
import models.PersonYear;
import models.Qualification;
import models.StampModificationType;
import models.StampModificationTypeValue;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.TotalOvertime;
import models.VacationCode;
import models.VacationPeriod;
import models.ValuableCompetence;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.StampTypeValues;
import models.enumerate.WorkingTimeTypeValues;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;

import com.google.common.collect.ImmutableMap;


public class FromMysqlToPostgres {

	public static Map<Integer,CompetenceCode> mappaCodiciCompetence = new HashMap<Integer,CompetenceCode>();
	public static Map<String,AbsenceTypeGroup> mappaCodiciAbsenceTypeGroup = new HashMap<String,AbsenceTypeGroup>();
	public static Map<Integer,VacationCode> mappaCodiciVacationType = new HashMap<Integer,VacationCode>();
	public static Map<Integer,WorkingTimeType> mappaCodiciWorkingTimeType = new HashMap<Integer,WorkingTimeType>();

	private static Map<Integer, JustifiedTimeAtWork> mappaQuantGiust = 
			ImmutableMap.<Integer, JustifiedTimeAtWork>builder()
			.put(0, JustifiedTimeAtWork.AllDay)
			.put(-1, JustifiedTimeAtWork.HalfDay)
			.put(1, JustifiedTimeAtWork.OneHour)
			.put(2, JustifiedTimeAtWork.TwoHours)
			.put(3, JustifiedTimeAtWork.ThreeHours)
			.put(4, JustifiedTimeAtWork.FourHours)
			.put(5, JustifiedTimeAtWork.FiveHours)
			.put(6, JustifiedTimeAtWork.SixHours)
			.put(7, JustifiedTimeAtWork.SevenHours)
			.put(8, JustifiedTimeAtWork.EightHours)
			.put(20, JustifiedTimeAtWork.Nothing)
			.put(21, JustifiedTimeAtWork.TimeToComplete)
			.put(23, JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours)
			.build();

	public static String mySqldriver = Play.configuration.getProperty("db.old.driver");//"com.mysql.jdbc.Driver";	

	private static Connection mysqlCon = null;

	public static Connection getMysqlConnection() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		if (mysqlCon != null ) {
			return mysqlCon;
		}
		Class.forName(mySqldriver).newInstance();

		mysqlCon = DriverManager.getConnection(
				Play.configuration.getProperty("db.old.url"),
				Play.configuration.getProperty("db.old.user"),
				Play.configuration.getProperty("db.old.password"));
		return mysqlCon;
	}


	/**
	 * Importa le informazioni del personale dal database Mysql dell'applicazione Orologio.
	 * I dati importati sono:
	 *  - ContactData
	 *  - Location
	 *  - WorkingTimeType
	 *  - ValuableCompetence
	 *  - Contract
	 *  - Vacations
	 *  - YearRecap
	 *  - MonthRecap
	 *  - Competence
	 *  - Stampings
	 * 
	 * @param limit se questo parametro è maggiore di 0 allora limita il numero di persone da importare in 
	 * 	funzione di questo parametro 
	 * @throws SQLException 
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLExceptiongetMonthResidualcreateContract
	 */

	public static void importVacationDaysPastYear() throws SQLException{
		//Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		String sql = "SELECT * FROM Orario WHERE Giorno >= '2012-01-01' AND Giorno <= '2012-12-31' AND TipoGiorno = 32 order by ID";
		PreparedStatement stmt = mysqlCon.prepareStatement(sql);
		ResultSet rs = stmt.executeQuery();
		Logger.info("Inizio popolamento dei personDay dell'anno precedente con i giorni di assenza per ciascuna persona");
		while(rs.next()){
			long oldId = rs.getLong("ID");
			Person person = Person.find("Select p from Person p where p.oldId = ?", oldId).first();
			if(person == null)
				throw new IllegalArgumentException();
			PersonDay pd = new PersonDay(person, new LocalDate(rs.getDate("Giorno")));
			pd.save();
			Absence abs = new Absence();
			Integer integer = rs.getInt("TipoGiorno");
			String code = integer.toString();
			abs.absenceType = AbsenceType.find("byCode", code).first();
			abs.personDay = pd;
			abs.save();


		}
		Logger.info("Fine popolamento della tabella dei personDay dell'anno precedente con le assenze dovute a ferie anno corrente");
		//	mysqlCon.close();

	}

	public static void importOreStraordinario() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();
		String sql = "SELECT * FROM monteorestr";

		PreparedStatement stmt = mysqlCon.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();
		Logger.info("Inizio popolamento tabella degli straordinari");
		while(rs.next()){

			TotalOvertime total = new TotalOvertime();
			total.date = new LocalDate(rs.getDate("data"));
			total.year = rs.getInt("anno");
			total.numberOfHours = rs.getInt("ore");
			total.save();
		}
		Logger.info("Fine popolamento tabella straordinari");
		mysqlCon.close();

	}

	public static void importAll(int limit, int anno) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();

		String sqlVacationCode = "Select * from ferie";
		
		PreparedStatement stmtCode = mysqlCon.prepareStatement(sqlVacationCode);
		ResultSet rsCode = stmtCode.executeQuery();
		JPAPlugin.startTx(false);
		while(rsCode.next()){
			VacationCode code = new VacationCode();
			code.description = rsCode.getString("nome");
			code.permissionDays = rsCode.getInt("giorni_pl");
			code.vacationDays = rsCode.getInt("giorni_ferie");
			code.save();
		}
			
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);
		String sql = "SELECT ID, Nome, Cognome, DataNascita, Telefono," +
				"Fax, Email, Stanza, Matricola, Matricolabadge, passwordmd5, Qualifica, Dipartimento, Sede " +
				"FROM Persone order by ID";
		if (limit > 0) {
			sql += " LIMIT " + limit;
		}


		PreparedStatement stmt = mysqlCon.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();

		JPAPlugin.closeTx(false);

		Date start = new Date();

		while(rs.next()){
			JPAPlugin.startTx(false);

			Date personStart = new Date();
			Logger.info("Creazione delle info per la persona: %s %s", rs.getString("Nome"), rs.getString("Cognome"));

			int oldIDPersona = rs.getInt("ID");

			Person person = FromMysqlToPostgres.createPerson(rs);

			FromMysqlToPostgres.createContactData(rs, person);

			FromMysqlToPostgres.createLocation(rs, person);

			FromMysqlToPostgres.setWorkingTimeType(oldIDPersona, person);


			FromMysqlToPostgres.createContract(oldIDPersona, person);

			JPAPlugin.closeTx(false);

//			FromMysqlToPostgres.createVacationType(oldIDPersona, person);	

			FromMysqlToPostgres.createInitializationTime(oldIDPersona, person, anno);		

			FromMysqlToPostgres.createPersonYear(oldIDPersona, person);

			JPAPlugin.closeTx(false);

			FromMysqlToPostgres.createStampings(oldIDPersona, person, anno);

			FromMysqlToPostgres.createPersonMonthAndYear(person);


			FromMysqlToPostgres.createValuableCompetence(rs.getInt("Matricola"), person);

			FromMysqlToPostgres.createCompetence(oldIDPersona, person, anno);

			Logger.info("Terminata la creazione delle info della persona %s %s", rs.getString("Nome"), rs.getString("Cognome"));

			JPAPlugin.closeTx(false);

			Logger.info("In %s secondi, terminata la creazione delle info della persona %s %s",
					((new Date()).getTime() - personStart.getTime()) / 1000,
					rs.getString("Nome"), rs.getString("Cognome"));
		}
		JPAPlugin.startTx(false);
		Logger.info("Terminata l'importazione dei dati di tutte le persone in %d secondi", ((new Date()).getTime() - start.getTime()) / 1000);

		Logger.info("Adesso aggiorno le date di inizio dei contratti, i vacation period, creo le competenze, il monte ore ed aggiusto i permessi");

//		FromMysqlToPostgres.updateContract();
		//		FromMysqlToPostgres.updateVacationPeriod();
		FromMysqlToPostgres.updateCompetence();

		FromMysqlToPostgres.updateCompetenceCode();
		FromMysqlToPostgres.importVacationDaysPastYear();
		FromMysqlToPostgres.personToCompetence();
		FromMysqlToPostgres.importOreStraordinario();

		FromMysqlToPostgres.addPermissiontoAll();
		
		FromMysqlToPostgres.checkFixedWorkingTime();
		JPAPlugin.closeTx(false);
		Logger.info("Importazione terminata");

		mysqlCon.close();


	}

	/**
	 * controlla tutte le persone che hanno timbrature fisse 
	 */
	public static void checkFixedWorkingTime() {
		Logger.debug("Controllo delle persone con timbratura fissa");
		List<Person> activePerson = Person.getActivePersons(new LocalDate(2013,1,1));
		
		for(Person p : activePerson){
			Logger.debug("Analizzo %s %s", p.name, p.surname);
			LocalDate date = new LocalDate().monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue();
			if(StampProfile.getCurrentStampProfile(p,date).fixedWorkingTime){
				
				while(date.isBefore(new LocalDate())){
					if(!DateUtility.isHoliday(p, date)){
						
						PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", p, date).first();
						if(pd == null){
							pd = new PersonDay(p, date);
							pd.create();
							Logger.debug("Creato person day per %s %s in data %s", p.name, p.surname, date);
							pd.populatePersonDay();
							pd.save();
							Logger.debug("Persistito il tempo di lavoro = %d per %s %s in data %s", pd.timeAtWork, p.name, p.surname, date);
						}
						
					}
					
					date = date.plusDays(1);
				}
				
			}
		}
	}


	
	/**
	 * metodo che assegna un inizio di contratto di default a quelle persone che dall'importazione non hanno ricevuto un beginContract
	 */
	public static void updateContract(){
		List<Person> personList = Person.findAll();
		for(Person p : personList){
			Contract con = Contract.find("Select con from Contract con where con.person = ?", p).first();
			if(con != null && con.beginContract == null){
				con.beginContract = new LocalDate(1970,1,1);
				con.save();
			}

		}
	}


	/**
	 * Importa le informazioni del personale dal database Mysql dell'applicazione Orologio.
	 * I dati importati sono:
	 *  - ContactData
	 *  - Location
	 *  - WorkingTimeType
	 *  - ValuableCompetence
	 *  - Contract
	 *  - Vacations
	 *  - YearRecap
	 *  - MonthRecap
	 *  - Competence
	 *  - Stampings
	 *   
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void importAll(int anno) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		importAll(0, anno);
	}


	public static Person createPerson(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.trace("Inizio a creare la persona %s %s", rs.getString("Nome"), rs.getString("Cognome"));

		Person person = Person.find("Select p from Person p where p.name = ? and p.surname = ?", rs.getString("Nome"), rs.getString("Cognome")).first();
		if(person == null){
			person = new Person();
			person.name = rs.getString("Nome");
			person.surname = rs.getString("Cognome");
			person.username = String.format("%s.%s", person.name.toLowerCase(), person.surname.toLowerCase() );
			person.password = rs.getString("passwordmd5");
			person.bornDate = rs.getDate("DataNascita");
			person.number = rs.getInt("Matricola");
			person.badgeNumber = rs.getString("Matricolabadge");
			person.oldId = rs.getLong("ID");
			Long qualifica = rs.getLong("Qualifica");
			person.qualification = qualifica != null && qualifica != 0 ? JPA.em().getReference(Qualification.class, qualifica) : null;
			person.save();
			Logger.info("Creata %s", person);
		} else {
			Logger.info("La persona %s %s era già presente nel db, non ne è stata creata una nuova", rs.getString("Nome"), rs.getString("Cognome"));
		}

		return person;
	}	

	public static void createContactData(ResultSet rs, Person person) throws SQLException {
		Logger.trace("Inizio a creare il contact data per %s", person);
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
			Logger.info("Validazione numero di telefono non avvenuta. No phone number");
			contactData.telephone = null;		
		}
		contactData.save();
		Logger.info("Creato %s", person);
	}


	public static void createLocation(ResultSet rs, Person person) throws SQLException{
		Logger.debug("Inizio a creare la location per %s", person);
		Location location = new Location();
		location.person = person;

		location.department = rs.getString("Dipartimento");
		location.headOffice = rs.getString("Sede");
		location.room = rs.getString("Stanza");		
		location.save();
		Logger.info("Creata %s", location);
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
	public static int importAbsenceTypes() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio ad importare gli AbsenceType");

		JPAPlugin.startTx(false);

		long absenceTypeCount = AbsenceType.count();
		if (absenceTypeCount > 0) {
			Logger.info("Sono già presenti %s AbsenceType nell'applicazione, non verranno importati nuovi AbsenceType dalla vecchia applicazione");
			return 0;
		}
		int importedAbsenceTypes = 0;

		Connection mysqlCon = getMysqlConnection();
		// select ordinata perchè così vengono prima creati i codici che hanno il campo codiceSost = null perchè ci sono codici che possono essere
		//utilizzati in sostituzione di altri codici
		PreparedStatement stmtCodici = mysqlCon.prepareStatement("Select * from Codici where Codici.id != 0 order by CodiceSost");
		ResultSet rsCodici = stmtCodici.executeQuery();

		AbsenceTypeGroup absTypeGroup = null;
		while(rsCodici.next()){
			AbsenceType absenceType = new AbsenceType();

			absenceType.code = rsCodici.getString("Codice");

			//I codici della serie 91 sono riposi compensativi
			if (absenceType.code.startsWith("91")) {
				absenceType.compensatoryRest = true;
			}

			if(absenceType.code.equals("21") || absenceType.code.equals("38") || absenceType.code.startsWith("11")){
				absenceType.consideredWeekEnd = true;
			}

			absenceType.description = rsCodici.getString("Descrizione");
			absenceType.validFrom = new LocalDate(rsCodici.getDate("DataInizio"));
			absenceType.validTo = new LocalDate(rsCodici.getDate("DataFine"));

			absenceType.internalUse = rsCodici.getByte("Interno") != 0; 
			absenceType.ignoreStamping =  rsCodici.getByte("IgnoraTimbr") != 0;


			if (rsCodici.getInt("QuantGiust") == 22) {
				Logger.info("Il tipo di assenza %s non e' stato importato perche' le assenze con \"Assegna tempo del'orario di lavoro\" non sono " +
						"più gestite come assenze ma come casi particolari dei PersonDay (gestiti con appositi boolean isTimeAtWorkAutoCertificated e isWorkingInAnotherPlace)",
						absenceType.description);
				continue;
			}

			absenceType.justifiedTimeAtWork = mappaQuantGiust.get(rsCodici.getInt("QuantGiust"));

			if(rsCodici.getString("Gruppo")!=null){
				String gruppo = rsCodici.getString("Gruppo");
				if(mappaCodiciAbsenceTypeGroup.get(gruppo) == null){
					absTypeGroup = new AbsenceTypeGroup();

					absTypeGroup.label = gruppo;
					absTypeGroup.limitInMinute = rsCodici.getInt("Limite");

					String codSost = rsCodici.getString("CodiceSost");
					if(codSost != null && !codSost.trim().equals("")){
						AbsenceType abt = JPA.em().createQuery("Select abt from AbsenceType abt where abt.code = :codSost", AbsenceType.class)
								.setParameter("codSost", codSost).getSingleResult();

						absTypeGroup.replacingAbsenceType = abt;
					}

					int gestioneLimite = rsCodici.getInt("GestLim");
					switch (gestioneLimite){
					case 0:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.nothing;
						break;
					case 1:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.replaceCodeAndDecreaseAccumulation;
						break;
					case 2:
						absTypeGroup.accumulationBehaviour = AccumulationBehaviour.noMoreAbsencesAccepted;
						break;
					default:
						throw new IllegalStateException(
								String.format("Valore del parametro GestLim = %s del gruppo di codici %s non supportato dalla nuova applicazione, " +
										"non esiste un corrispettivo AccumulationBehaviour", 
										gestioneLimite, absTypeGroup.label));
					}

					int accumulo = rsCodici.getInt("Accumulo");
					switch (accumulo){
					case 0:
						absTypeGroup.accumulationType = AccumulationType.no;
						break;
					case 1:
						absTypeGroup.accumulationType = AccumulationType.monthly;
						break;
					case 2:
						absTypeGroup.accumulationType = AccumulationType.yearly;
						break;
					case 3:
						absTypeGroup.accumulationType = AccumulationType.always;
						break;
					default:
						throw new IllegalStateException(
								String.format("Valore del parametro accumulo = \"%s\" non supportato dalla nuova applicazione, " +
										"non esiste un corrispettivo AccumulationType", accumulo));
					}

					absTypeGroup.minutesExcess = rsCodici.getByte("MinutiEccesso") != 0;

					absTypeGroup.save();
					Logger.info("Creato absenceTypeGroup %s", absTypeGroup.label);
					absenceType.absenceTypeGroup = absTypeGroup;
					mappaCodiciAbsenceTypeGroup.put(gruppo, absTypeGroup);
				}
				else{
					absTypeGroup = mappaCodiciAbsenceTypeGroup.get(gruppo);
					absenceType.absenceTypeGroup = absTypeGroup;
				}

			}

			absenceType.save();		
			JPA.em().flush();
			Logger.info("Creato absenceType %s - %s", absenceType.code, absenceType.description);

			importedAbsenceTypes++;
		}

		JPAPlugin.closeTx(false);

		return importedAbsenceTypes;
	}

	/**
	 * metodo per la giunzione tra absenceType e Qualifications
	 */
	public static void createAbsenceTypeToQualificationRelations(){
		Logger.debug("Aggiungo le qualfiche possibili agli AbsenceType");

		JPAPlugin.startTx(false);

		List<AbsenceType> listaAssenze = AbsenceType.findAll();
		for(AbsenceType absenceType : listaAssenze){

			if(absenceType.code.equals("OA1") || absenceType.code.equals("OA2") || absenceType.code.equals("OA3") 
					|| absenceType.code.equals("OA4") || absenceType.code.equals("OA5") || absenceType.code.equals("OA6")
					|| absenceType.code.equals("OA7")){
				long id1 = 1;
				long id2 = 2;
				long id3 = 3;

				Qualification qual1 = Qualification.findById(id1);
				Qualification qual2 = Qualification.findById(id2);
				Qualification qual3 = Qualification.findById(id3);

				if(absenceType.qualifications.isEmpty()){
					absenceType.qualifications.add(qual1);
					absenceType.qualifications.add(qual2);
					absenceType.qualifications.add(qual3);
					Logger.debug("Aggiunte le qualifiche 1-2-3 all'AbsenceCode %s", absenceType.code);
				}

			}
			else{
				List<Qualification> listaQual = Qualification.findAll();
				absenceType.qualifications.addAll(listaQual);
				Logger.debug("Aggiunte tutte le qualifiche all'AbsenceCode %s", absenceType.code);
			}
		}		
		JPAPlugin.closeTx(false);
	}

	public static int importWorkingTimeTypes() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Logger.debug("Comincio l'importazione dei WorkingTimeType");

		JPAPlugin.startTx(false);

		Long workingTimeTypeCount = WorkingTimeType.count();
		if (workingTimeTypeCount > 1) {
			Logger.info("Ci sono %s WorkingTimeType presenti nel database, i workingTimeType NON verranno importati dal database MySQL", workingTimeTypeCount);
			return 0;
		}

		int importedWorkingTimeTypes = 0;

		Connection mysqlCon = FromMysqlToPostgres.getMysqlConnection();

		WorkingTimeType wtt = null;

		PreparedStatement selectOrariDiLavoro = mysqlCon.prepareStatement("select * from orari_di_lavoro");
		ResultSet orarioDiLavoro = selectOrariDiLavoro.executeQuery();

		while(orarioDiLavoro.next()){

			Integer idCodiceOrarioLavoro = orarioDiLavoro.getInt("id");

			if(mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro)!=null){
				wtt = mappaCodiciWorkingTimeType.get(idCodiceOrarioLavoro);
			}
			else{					
				wtt = new WorkingTimeType();				

				wtt.description = orarioDiLavoro.getString("nome");
				wtt.shift = orarioDiLavoro.getBoolean("turno");
				//				wtt.defaultWorkingTimeType = orarioDiLavoro.getBoolean("comune");
				wtt.save();
				Logger.info("Creato %s", wtt);

				mappaCodiciWorkingTimeType.put(idCodiceOrarioLavoro,wtt);

				WorkingTimeTypeDay wttd = null;
				ImmutableMap<Integer, String> weekDays = 
						ImmutableMap.<Integer, String>builder()
						.put(1, "lu").put(2, "ma").put(3, "me").put(4, "gi").put(5, "ve").put(6, "sa").put(7, "do")
						.build();

				for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
					wttd = new WorkingTimeTypeDay();
					wttd.workingTimeType = wtt;
					wttd.dayOfWeek = dayOfWeek;
					String dayOfWeekStart = weekDays.get(dayOfWeek);
					wttd.workingTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_lavoro");
					wttd.holiday = orarioDiLavoro.getBoolean(dayOfWeekStart + "_festa");
					wttd.timeSlotEntranceFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_ingresso");
					wttd.timeSlotEntranceTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_ingresso1");
					wttd.timeSlotExitFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_uscita");
					wttd.timeSlotExitTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_uscita1");
					wttd.timeMealFrom = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_pranzo");
					wttd.timeMealTo = orarioDiLavoro.getInt(dayOfWeekStart + "_fascia_pranzo1");
					wttd.breakTicketTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_interv"); 
					wttd.mealTicketTime = orarioDiLavoro.getInt(dayOfWeekStart + "_tempo_buono");
					wttd.save();
					wtt.workingTimeTypeDays.add(wttd);
					Logger.info("Creato %s", wttd);
				}
			}

			//Per ricaricare la lista dei WorkingTimeTypeDay associati al WorkingTimeType
			//wtt.refresh();

			importedWorkingTimeTypes++;
		}
		Logger.info("Creati %d workingTimeTimes", importedWorkingTimeTypes);

		JPAPlugin.closeTx(false);
		return importedWorkingTimeTypes;
	}



	private static void setWorkingTimeType(int oldIDPersona, Person person) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		if (person == null) {
			throw new IllegalArgumentException("Person should not be null at this point");
		}

		Logger.debug("Cerco il workingTimeType per %s", person);

		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("select odl.id, odl.nome, op.data_inizio, op.data_fine FROM orari_di_lavoro as odl JOIN orario_pers op " +
				" ON op.oid = odl.id " +
				" WHERE op.pid = " + oldIDPersona + " AND op.data_inizio < curdate() and op.data_fine > curdate() order by op.data_fine desc limit 1");
		ResultSet rs = stmt.executeQuery();

		WorkingTimeType wtt = null;
		if(rs.next()){

			wtt = mappaCodiciWorkingTimeType.get(rs.getInt("id"));
			Logger.trace("Esiste il working time. Assegno a %s %s il tempo di lavoro %s", person.name, person.surname, mappaCodiciWorkingTimeType.get(rs.getInt("id")));

		} else {
			//Non c'è nessun orario di lavoro impostato per la Persona quindi impostiamo l'orario predefinito che è il normale-mod
			wtt = WorkingTimeType.em().getReference(WorkingTimeType.class, WorkingTimeTypeValues.NORMALE_MOD.getId());
		}
		person.workingTimeType = wtt;
		person.save();
		Logger.info("Assegnato %s a %s. Il tipo di orario ha questi giorni impostati %s", wtt, person, wtt.workingTimeTypeDays);
	}



	/**
	 * metodo che crea un contratto per la persona in questione. Se è già presente un contratto per quella persona,
	 * questo viene cancellato nel caso in cui la data di fine del contratto già salvato sia inferiore alla data inizio
	 * del nuovo contratto così da salvare nello storico il contratto precedente.
	 *  
	 * @param id
	 * @param person
	 * @param em
	 */
	public static void createContract(long id, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Logger.debug("Inizio a creare il contratto per %s", person);	

		Connection mysqlCon = getMysqlConnection();	
		PreparedStatement stmtContratto = mysqlCon.prepareStatement("SELECT id,DataInizio,DataFine,continua,firma,Presenzadefault " +	
				"FROM Personedate WHERE id=" + id + " order by DataInizio");	
		ResultSet rs = stmtContratto.executeQuery();       	

		Contract previousContract = null;
		StampProfile stampProfile = null;
		int mysqlPresenceDefault = 0;
		while(rs.next()){
			//dati contratto mysql
			if(person.id == 40L){
				int i = 0;
			}
			LocalDate mysqlStartContract = null;
			Date mysqlBegin = rs.getDate("DataInizio");
			Date mysqlEnd = rs.getDate("DataFine");
			Byte mysqlContinua = rs.getByte("continua"); //==1			//continua
			Byte mysqlFirma = rs.getByte("firma");						//firma
			mysqlPresenceDefault = rs.getInt("Presenzadefault");		//presenzadefault
			if(mysqlBegin != null)
				mysqlStartContract = new LocalDate(mysqlBegin);			//start contract
			else
				mysqlStartContract = new LocalDate(1970,1,1);
			
			LocalDate mysqlEndContract;									//end contract
			if (mysqlEnd != null) 
			{
				mysqlEndContract = new LocalDate(mysqlEnd);
			}
			else{
				mysqlEndContract = null;
			}
			

			//postgres
			if(previousContract==null)
			{
				//prima iterazione
				Contract contract = new Contract();
				contract.onCertificate = mysqlFirma == 0 ? true : false;
				contract.person = person;
				contract.beginContract = mysqlStartContract;
				contract.expireContract = mysqlEndContract;
				contract.endContract = null;
				previousContract = contract;
			}
			else
			{
				//continua dal precedente
				if(mysqlContinua==1)
				{
					//aggiorno i campi
					previousContract.expireContract = mysqlEndContract;
				}

				//storicizzo il previous contract e creo il nuovo contratto
				if(mysqlContinua==0)
				{

					previousContract.save();

					stampProfile = new StampProfile();
					stampProfile.person = person;
					stampProfile.fixedWorkingTime = mysqlPresenceDefault == 0 ? false : true;
					stampProfile.startFrom = previousContract.beginContract;
					stampProfile.endTo = previousContract.expireContract;
					stampProfile.create();
					person.stampProfiles.add(stampProfile);
					person.save();

					Contract contract = new Contract();
					contract.onCertificate = mysqlFirma == 0 ? true : false;
					contract.person = person;
					contract.beginContract = mysqlStartContract;
					contract.expireContract = mysqlEndContract;
					contract.endContract = null;
					previousContract = contract;
				}
			}
		}

		//storicizzo l'ultimo contratto
		if(previousContract!=null)
		{
			previousContract.save();
			stampProfile = new StampProfile();
			stampProfile.person = person;
			stampProfile.fixedWorkingTime = mysqlPresenceDefault == 0 ? false : true;
			stampProfile.startFrom = previousContract.beginContract;
			stampProfile.endTo = previousContract.expireContract;
			stampProfile.create();
			person.stampProfiles.add(stampProfile);
			person.save();
			JPAPlugin.closeTx(false);
			FromMysqlToPostgres.createVacationType(previousContract);
		}
		
		
	}



	public static void createStampings(long id, Person p, int anno) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		JPAPlugin.startTx(false);

		Person person = Person.findById(p.id);

		Logger.debug("Inizio a creare le timbrature per %s", person);
		Connection mysqlCon = getMysqlConnection();

		/**
		 * query sulle tabelle orario, per recuperare le info sulle timbrature e sulle assenze
		 * di ciascuna persona per generare i personday
		 */


		PreparedStatement stmtOrari = mysqlCon.prepareStatement("SELECT Orario.ID,Orario.Giorno,Orario.TipoGiorno,Orario.TipoTimbratura," + 	
				"Orario.Ora, Codici.id, Codici.Codice, Codici.Qualifiche " +
				"FROM Orario " +
				"LEFT JOIN Codici ON Orario.TipoGiorno=Codici.id " +
				"WHERE Orario.Giorno >= '2013-01-01' " +
				"and Orario.ID = " + id + " ORDER BY Orario.Giorno");
		ResultSet rs = stmtOrari.executeQuery();

		PersonDay pd = null;
		LocalDate data = null;
		LocalDate newData = null;

		int importedStamping = 0;
		boolean countAdmin = false;
		while(rs.next()){
			Logger.debug("Il tipoGiorno è: %s ", rs.getInt("TipoGiorno"));
			if(rs.getInt("TipoGiorno")==-1){
				//Logger.debug("Rilevata timbratura modificata dall'amministratore il %s per %s %s", pd.date, pd.person.name, pd.person.surname);
				countAdmin = true;
				continue;

			}
			/**
			 * controllo che la data prelevata sia diversa da null, poichè nel vecchio db esiste la possibilità di avere date del tipo 0000-00-00
			 * in tal caso devo scartarle e continuare l'elaborazione
			 */
			if(rs.getDate("Giorno") == null){
				Logger.warn("Impossibile importare la timbratura in quanto la data è nulla." +
						"Timbratura e PersonDay non creati per %s", person.toString());
				continue;
			}
			newData = new LocalDate(rs.getDate("Giorno"));

			if(data != null) {

				if(newData.isAfter(data)){		

					Logger.debug("Nuovo giorno %s per %s, prima si fanno i calcoli sul personday poi si crea quello nuovo", newData, person.toString());
					Logger.debug("Il progressivo del personday del giorno appena trascorso da cui partire per fare i calcoli è: %s", pd.progressive);
					PersonDay pdOld = PersonDay.findById(pd.id);

					pdOld.populatePersonDay();	
					pdOld.merge();

					Logger.debug("Il progressivo del personday del giorno appena trascorso assegnato a un nuovo personDay è: %s", pdOld.progressive);

					pd = new PersonDay(person, newData);
					pd.create();
					
					Logger.debug("Creato %s ", pd.toString());
					//il caso di timbratura modificata/inserita dall'amministratore

					if(rs.getInt("TipoGiorno")==0){

						createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora"), countAdmin);
						countAdmin = false;
					}
					else{
						createAbsence(pd, rs.getString("Codice"));
					}

				}
				if(newData.isEqual(data)){					
					/**
					 * si tratta di timbratura
					 */		

					if(rs.getInt("TipoGiorno")==0){					
						Logger.debug("Altra timbratura per %s nel giorno %s", person.toString(), newData);
						createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora"), countAdmin);
						countAdmin = false;
					}
					/**
					 * si tratta di assenza
					 */
					else{
						Logger.debug("Assenza verosimilmente oraria per %s nel giorno %s", person.toString(), newData);					
						createAbsence(pd, rs.getString("codice"));
					}
				}
			}
			else {

				Logger.debug("Prima timbratura o assenza per %s", person.toString());

				if(pd == null){
					pd = new PersonDay(person,newData);
					pd.create();
					Logger.debug("Creato %s", pd.toString());
				}

				if(rs.getInt("TipoGiorno")==0){				
					createStamping(pd, rs.getLong("TipoTimbratura"), rs.getBytes("Ora"), countAdmin); 
					countAdmin = false;
				}
				/**
				 * si tratta di assenza
				 */
				else{										
					createAbsence(pd, rs.getString("codice"));
				}	
			}

			if(rs.isLast()){
				Logger.info("Creazione dell'ultimo person day per %s", person.toString());
				/**
				 * in questo caso la data del "giro successivo" è nulla poichè siamo all'ultima riga del ciclo. Quindi bisogna fare 
				 * i calcoli del personDay relativi a questo ultimo giorno (quello con date = data).
				 */
				pd.merge();
				pd.populatePersonDay();

				Logger.debug("Il progressivo al termine del resultset è: %s e il differenziale è: %s", pd.progressive, pd.difference);
				Logger.info("Creato %s", pd);
			}

			data = newData;		
			countAdmin = false;

			if (importedStamping % 100 == 0) {
				JPAPlugin.closeTx(false);
				JPAPlugin.startTx(false);
				Logger.debug("Importate %d timbrature per %s. Chiusa e riaperta la transazione", importedStamping, person);
				person = Person.findById(person.id);
				pd = pd.merge();
			}

			importedStamping++;

		}

		JPAPlugin.closeTx(false);

		Logger.debug("Terminato di creare le timbrature per %s", person);

	}

	public static void createVacationType(Contract con) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		JPAPlugin.startTx(false);
		Logger.debug("Inizio a creare i periodi di ferie per %s", con.person);
		if(con.expireContract == null){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = con.beginContract;
			first.endTo = con.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = con;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = con.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =con;
			second.save();
		}
		else{
			if(con.expireContract.isAfter(con.beginContract.plusYears(3).minusDays(1))){
				VacationPeriod first = new VacationPeriod();
				first.beginFrom = con.beginContract;
				first.endTo = con.beginContract.plusYears(3).minusDays(1);
				first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
				first.contract = con;
				first.save();
				VacationPeriod second = new VacationPeriod();
				second.beginFrom = con.beginContract.plusYears(3);
				second.endTo = con.expireContract;
				second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
				second.contract =con;
				second.save();
			}
			else{
				VacationPeriod first = new VacationPeriod();
				first.beginFrom = con.beginContract;
				first.endTo = con.expireContract;
				first.contract = con;
				first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
				first.save();
			}
		}

	}

	public static void createInitializationTime(long id, Person person, int anno) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{

		JPAPlugin.startTx(false);
		/**
		 * query su totali_anno per recuperare lo storico da mettere in YearRecap
		 */
		Logger.debug("Inizio a creare i riepiloghi annuali per %s", person);
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtResidualAndRecovery = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID = ? and anno = ? limit 1");
		stmtResidualAndRecovery.setLong(1, id);
		stmtResidualAndRecovery.setInt(2, 2012);
		ResultSet rs = stmtResidualAndRecovery.executeQuery();

		if(rs != null){
			while(rs.next()){
				InitializationTime initTime = new InitializationTime();
				InitializationAbsence initAbsence = new InitializationAbsence();
				initTime.date = new LocalDate(2013,1,1);
				initTime.person = person;
				initTime.residualMinutesPastYear = rs.getInt("residuo");
				initTime.save();
				initAbsence.person = person;
				initAbsence.date = new LocalDate(2013,1,1);
				initAbsence.recoveryDays = rs.getInt("recg");
				initAbsence.absenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", "91").first();
				initAbsence.save();
			}
		}
		Logger.info("Terminati i riepiloghi annuali per %s", person);

	}

	public static void createPersonYear(long id, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmtResidualAndRecovery = mysqlCon.prepareStatement("SELECT * FROM totali_anno WHERE ID = ? and anno = 2011");
		stmtResidualAndRecovery.setLong(1, id);

		ResultSet rs = stmtResidualAndRecovery.executeQuery();
		while(rs.next()){
			PersonYear py = new PersonYear(person,2011);
			py.remainingMinutes = rs.getInt("residuo");
			py.save();
		}
	}


	public static void createCompetence(long id, Person person, int anno) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		/**
		 * funzione che riempe la tabella competence e la tabella competence_code relativamente alle competenze
		 * di una determinata persona
		 */
		Logger.debug("Inizio a creare le competenze per %s", person);

		Connection mysqlCon = getMysqlConnection();
		PreparedStatement stmt = mysqlCon.prepareStatement("Select codici_comp.id, competenze.mese, codici_comp.codice, codici_comp.codice_att, " +
				"competenze.anno, competenze.codice, competenze.valore, codici_comp.descrizione, codici_comp.inattivo " +
				"from competenze, codici_comp where codici_comp.codice=competenze.codice and competenze.id= ? and competenze.anno >= ?");
		stmt.setLong(1, id);
		stmt.setInt(2, anno);
		ResultSet rs = stmt.executeQuery();

		Competence competence = null;
		CompetenceCode competenceCode = null;

		while(rs.next()){			

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
					personRep.save();
					Logger.info("Creato %s", personRep.toString());
				}

			}

			int idCodiciCompetenza = rs.getInt("id");	
			if(mappaCodiciCompetence.get(idCodiciCompetenza)== null){
				Logger.debug("Non esiste ancora il codice competenza %s, lo creo.", rs.getString("codice"));
				competenceCode = new CompetenceCode();
				competenceCode.code = rs.getString("codice");
				competenceCode.codeToPresence = rs.getString("codice_att");
				competenceCode.description = rs.getString("descrizione");
				competenceCode.inactive = rs.getByte("inattivo") != 0;

				competenceCode.save();
				mappaCodiciCompetence.put(idCodiciCompetenza,competenceCode);
				Logger.debug("Creato il codice competenza %s.", competenceCode.code);
				Logger.info("Creato %s", competenceCode.toString());			

			} 
			else {
				competenceCode = mappaCodiciCompetence.get(idCodiciCompetenza);
			}
			competence = new Competence();
			competence.month = rs.getInt("mese");
			competence.year = rs.getInt("anno");
			competence.person = person;
			competence.valueApproved = rs.getInt("valore");
			competence.competenceCode = competenceCode;
			competence.save();
			Logger.debug("Creato %s", competence.toString());


		}	
		Logger.debug("Terminato di creare le competenze per %s", person);

	}

	/**
	 * 
	 * @param matricola
	 * @param person
	 * @param em
	 * 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * funzione che associa a ciascuna persona creata, le corrispettive competenze valide.
	 */
	public static void createValuableCompetence(int matricola, Person person) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{

		JPAPlugin.startTx(false);
		Logger.debug("Inizio a creare le competenze valide per %s %s", person.name, person.surname);
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
			valuableCompetence.save();
			Logger.info("Creato %s", valuableCompetence);
		}
		if (valuableCompetence == null) {
			Logger.info("Non ci sono competenza valide da importare per %s", person);
		}

	}


	public static void createPersonMonthAndYear(Person person){

		JPAPlugin.startTx(false);
		Logger.debug("Inizio a creare i personMonth e personYear per %s %s", person.name, person.surname);

		List<String> result = JPA.em().
				createNativeQuery("select cast(extract(year from date) as text) || extract(month from date) " +
						"FROM person_days WHERE person_id = :personId group by extract(year from date), extract(month from date) " +
						"order by extract(year from date), extract(month from date)").setParameter("personId", person.id).getResultList();

		for(String s : result){
			Integer year = new Integer(s.substring(0, 4));
			Logger.debug("L'anno di riferimento per il personMonth di %s %s è %s", person.name, person.surname, year);
			Integer month  = new Integer(s.substring(4, s.length()));
			Logger.debug("Il mese di riferimento per il personMonth di %s %s è %s", person.name, person.surname, month);
			PersonMonth pm = PersonMonth.build(person, year, month);
			pm.save();
			if(month.equals(12)){
				PersonYear py = PersonYear.build(person, year);
				py.save();
			}

		}
		JPAPlugin.closeTx(false);
		Logger.debug("Terminata la creazione dei personMonth e personYear per %s %s", person.name, person.surname);
	}

	private static void setDateTimeToStamping(Stamping stamping, LocalDate date, String time){

		if(time.startsWith("-")){
			int hour = Integer.parseInt(time.substring(1, 3));
			int minute = Integer.parseInt(time.substring(4, 6));
			int second = Integer.parseInt(time.substring(7, 9));
			/**
			 * aggiunti i campi anno mese e giorno per provare a risolvere il problema sulle date.
			 * inoltre aggiunte le set corrispondenti all'oggetto calendar creato
			 */

			int year = date.getYear();
			int month = date.getMonthOfYear();
			int day = date.getDayOfMonth();

			stamping.date = new LocalDateTime(year,month,day,hour,minute,second);

			stamping.markedByAdmin = false;
			stamping.stampType = StampType.find("Select st from StampType st where st.code = ?", "motiviDiServizio").first();
			//stamping.stampType = StampType.em().getReference(StampType.class, StampTypeValues.MOTIVI_DI_SERVIZIO.getId());
		}
		else{

			int hour = Integer.parseInt(time.substring(0, 2));
			int minute = Integer.parseInt(time.substring(3, 5));
			int second = Integer.parseInt(time.substring(6, 8));	
			int year = date.getYear();
			int month = date.getMonthOfYear();
			int day = date.getDayOfMonth();

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
				stamping.note = "Timbratura inserita dall'amministratore";
				stamping.markedByAdmin = true;
			}						

			else{
				if(hour == 24){
					stamping.date = new LocalDateTime(year,month,day,0,minute,second).plusDays(1);
					stamping.markedByAdmin = true;
				}
				else{
					stamping.date = new LocalDateTime(year,month,day,hour,minute,second);
					stamping.markedByAdmin = false;

				}
			}
		}
	}

	//	private static void createModifiedStamping(PersonDay pd, long tipoTimbratura, ResultSet rs ) throws SQLException{
	//		Stamping stamping = new Stamping();
	//		while(rs.next()){
	//			if(rs.getLong("TipoTimbratura")==tipoTimbratura && rs.getInt("TipoGiorno") != -1){
	//				if(tipoTimbratura % 2 != 0)
	//					stamping.way = WayType.in;	
	//				else
	//					stamping.way = WayType.out;
	//				String s = rs.getString("Ora");
	//				setDateTimeToStamping(stamping, pd.date, s);
	//				stamping.personDay = pd;
	//				stamping.markedByAdmin = true;
	//				stamping.note = "timbratura modificata dall'amministratore";
	//				stamping.stampModificationType = StampModificationType.findById(3L);
	//				stamping.save();
	//				pd.stampings.add(stamping);
	//				pd.merge();
	//				
	//				Logger.debug("Creata timbratura modificata dall'amministratore %s", stamping.toString());
	//			}
	//		}
	//	}

	private static void createStamping(PersonDay pd, long tipoTimbratura, byte[] oldTime, boolean countAdmin){
		Stamping stamping = new Stamping();

		if(tipoTimbratura % 2 != 0)
			stamping.way = WayType.in;	
		else
			stamping.way = WayType.out;

		if(oldTime == null){
			Logger.info("L'ora è nulla nella timbratura del %s per la persona %s e non verrà inserita", pd.date, pd.person.toString());
			return; 
		}
		String s = oldTime != null ? new String(oldTime) : null;
		setDateTimeToStamping(stamping, pd.date, s);
		stamping.markedByAdmin = countAdmin;
		//		stamping.considerForCounting = true;
		if(countAdmin == true)
			stamping.stampModificationType = StampModificationTypeValue.MARKED_BY_ADMIN.getStampModificationType();
		stamping.personDay = pd;
		stamping.save();
		pd.stampings.add(stamping);
		pd.merge();

		Logger.debug("Creata %s", stamping.toString());	

	}

	private static void createAbsence(PersonDay pd, String codice){

		AbsenceType absenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", codice).first();

		Absence absence = new Absence();
		absence.personDay = pd;
		absence.absenceType = absenceType;
		absence.save();
		pd.absences.add(absence);
		pd.merge();
		Logger.debug("Creata %s", absence);
		if(absenceType.code.equals("91")){
			Logger.debug("Trovato riposo compensativo per %s %s nel giorno %s. Proseguo con i calcoli...", 
					pd.person.name, pd.person.surname, pd.date);
			PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
					pd.person, pd.date.getYear(), pd.date.getMonthOfYear()).first();
			if(pm == null){
				Logger.debug("Il person month era nullo, lo devo creare");
				pm = new PersonMonth(pd.person, pd.date.getYear(), pd.date.getMonthOfYear());

			}
			pm.prendiRiposoCompensativo(pd.date);
			pm.save();
			Logger.debug("Assegnato riposo compensativo e salvato person month per %s %s", pd.person.name, pd.person.surname);
			Logger.debug("%s %s: il valore dei riposi compensativi è: %s per anno corrente e %s per anno passato", 
					pd.person.name, pd.person.surname, pm.riposiCompensativiDaAnnoCorrente, pm.riposiCompensativiDaAnnoPrecedente);

		}

		//createAbsence(pd, absenceType);
	}

	/**
	 * metodo che consente permessi di "amministrazione" a un utente specificato
	 */
	public static void upgradePerson(){
		Logger.debug("Chiamata la funzione upgrade person");
		Person person = Person.find("bySurnameAndName", "Lucchesi", "Cristian").first();
		Logger.debug("Scelta persona: %s %s", person.name, person.surname);
		if(person.permissions.size() > 0){
			List<Permission> oldPermissions = person.permissions;
			person.permissions.removeAll(oldPermissions);
			List<Permission> permissionList = Permission.findAll();
			person.permissions.addAll(permissionList);
		}
		else{
			List<Permission> permissionList = Permission.findAll();
			person.permissions.addAll(permissionList);
		}		

		person.save();

	}

	/**
	 * metodo che dà a ciascun utente presente in anagrafica la possibilità di avere il permesso di visualizzazione per la propria
	 * situazione mensile
	 */
	public static void addPermissiontoAll(){
		Logger.debug("Chiamata la funzione addPermissiontoAll");
		List<Person> personList = Person.findAll();
		Permission per = Permission.find("Select per from Permission per where per.description = ?", "viewPersonalSituation").first();
		Logger.debug("Caricato il permesso: %s", per.description);
		for(Person p : personList){
			p.permissions.add(per);
			p.save();
		}

	}
	/**
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void updateCompetence() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException{
		//Logger.debug("Chiamata la funzione update competence per %s %s per impostare gli straordinari", person.name, person.surname);

		List<Person> personList = Person.findAll();
		CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?","S1").first();
		Logger.debug("Il codice competenze è: %s", code);
		if(code == null){
			Logger.debug("Il codice era nullo, quindi va creato...");
			code = new CompetenceCode();
			code.description = "Straordinario diurno nei giorni lavorativi";
			code.code = "S1";
			code.codeToPresence = "S1";
			code.inactive = false;
			code.save();
			Logger.debug("Il codice creato: %s", code.code);
		}

		for(Person person : personList){
			Logger.debug("Prendo il vecchio id di %s %s: %s", person.name, person.surname, person.oldId);
			if(person.oldId != null){
				//Connection mysqlCon = getMysqlConnection();
				PreparedStatement stmt = mysqlCon.prepareStatement("Select ore_str, mese, anno " +
						"from totali_mens, Persone where Persone.ID = totali_mens.ID and totali_mens.anno > ? and Persone.ID = ?");
				stmt.setLong(1, 2011);
				stmt.setLong(2, person.oldId);

				ResultSet rs = stmt.executeQuery();
				Logger.debug("Risultato della interrogazione sul vecchio db per le competenze: %s", rs.toString());
				while(rs.next()){

					Competence comp = new Competence();
					comp.competenceCode = code;
					comp.person = person;
					comp.year = rs.getInt("anno");
					comp.month = rs.getInt("mese");

					comp.valueApproved = rs.getInt("ore_str");
					comp.save();
					Logger.debug("Creata competenza per il mese di %s e anno %s con valore: %s", comp.month, comp.year, comp.valueApproved);

					/**
					 * aggiorno i personMonth con il valore degli straordinari solo relativamente a questo anno
					 */
					LocalDate date = new LocalDate();
					if(rs.getInt("anno") == date.getYear()){
						PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
								person, rs.getInt("anno"), rs.getInt("mese")).first();
						if(pm == null)
							pm = new PersonMonth(person, rs.getInt("anno"), rs.getInt("mese"));
						pm.assegnaStraordinari(comp.valueApproved);
						pm.save();
					}

				}
			}
			Logger.debug("Terminata la update competence per %s %s.", person.name, person.surname);
		}

	}

	/**
	 * metodo di riempimento da lanciare al termine della procedura di importazione per completare tutti i codici di competenza presenti
	 * @throws SQLException
	 */
	public static void updateCompetenceCode() throws SQLException{
		Logger.debug("Lancio la updateCompetenceCode per riempire i codici di competenza con quelli meno usati ma presenti nel vecchio db");

		PreparedStatement stmt = mysqlCon.prepareStatement("Select * from codici_comp");		

		ResultSet rs = stmt.executeQuery();
		while(rs.next()){

			CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", 
					rs.getString("codice")).first();
			if(code == null){
				Logger.debug("Non ho trovato il codice di competenza %s nel db, devo crearlo", rs.getString("codice"));
				code = new CompetenceCode();
				code.create();
				code.code = rs.getString("codice");
				code.codeToPresence = rs.getString("codice_att");
				code.description = rs.getString("descrizione");
				code.inactive = rs.getBoolean("inattivo");
				code.save();
			}					

		}
		Logger.debug("Terminata la update competenceCode.");
	}

	public static void personToCompetence() throws SQLException{
		Logger.debug("PersonToCompetence per dare a ogni dipendente le proprie competenze attive");

		PreparedStatement stmt = mysqlCon.prepareStatement("Select * from compvalide");
		ResultSet rs = stmt.executeQuery();

		while(rs.next()){
			CompetenceCode code = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", rs.getString("codicecomp")).first();
			Person person = Person.find("Select p from Person p where p.number = ?", rs.getInt("matricola")).first();
			if(person != null && code != null){
				person.competenceCode.add(code);
				person.save();
			}
		}
		Logger.debug("Terminazione di PersonToCompetence per dare a ogni dipendente le proprie competenze attive");
	}
}

