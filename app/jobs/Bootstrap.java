package jobs;

import it.cnr.iit.epas.DateUtility;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.sun.org.apache.bcel.internal.classfile.ConstantObject;

import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import models.CompetenceCode;
import models.Contract;
import models.Person;
import models.Qualification;
import models.Role;
import models.StampModificationType;
import models.StampType;
import models.User;
import models.VacationCode;
import models.enumerate.Parameter;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;

/**
 * Carica nel database dell'applicazione i dati iniziali predefiniti nel caso questi non siano già presenti
 *
 * @author cristian
 *
 */
@OnApplicationStart
@Slf4j
public class Bootstrap extends Job<Void> {
	
	private final static String JOBS_CONF = "jobs.active";

	@Inject
	static FixUserPermission fixUserPermission;
	@Inject
	static IWrapperFactory wrapperFactory;
	@Inject
	static ConsistencyManager consistencyManager; 
	
	public static class DatasetImport implements Work {

		private DatabaseOperation operation;
		private final URL url;

		public DatasetImport(DatabaseOperation operation, URL url) {
			this.operation = operation;
			this.url = url;
		}

		@Override
		public void execute(Connection connection) {
			try {
				//org.dbunit.dataset.datatype.DefaultDataTypeFactory
				IDataSet dataSet = new FlatXmlDataSetBuilder()
				.setColumnSensing(true).build(url);
				operation.execute(new H2Connection(connection, ""), dataSet);
			} catch (DataSetException e) {
				e.printStackTrace();
			} catch (DatabaseUnitException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void doJob() throws IOException {

		if (Play.runingInTestMode()) {
			log.info("Application in test mode, default boostrap job not started");
			return;
		}
		
//		in modo da inibire l'esecuzione dei job in base alla configurazione
		if("false".equals(Play.configuration.getProperty(JOBS_CONF))){
			log.info("Bootstrap Interrotto. Disattivato dalla configurazione.");
			return;
		}

		Session session = (Session) JPA.em().getDelegate();

		if(Qualification.count() == 0 ) {
			
			//qualification absenceType absenceTypeQualification absenceTypeGroup
			session.doWork(new DatasetImport(DatabaseOperation.INSERT,Resources
					.getResource("../db/import/absence-type-and-qualification-phase1.xml")));

			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/absence-type-and-qualification-phase2.xml")));

			//workingTimeType workingTimeTypeDay
			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources
					.getResource("../db/import/working-time-types.xml")));

//			// History
//			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
//					"../db/import/history/part1_history.xml")));
//			// History
//			session.doWork(new DatasetImport(DatabaseOperation.INSERT, Resources.getResource(Bootstrap.class,
//					"../db/import/history/part2_history.xml")));
		}
		
		if(User.find("byUsername", "developer").fetch().isEmpty()) {
			Fixtures.loadModels("../db/import/developer.yml");
		}
		if(Role.count() == 0){
			Fixtures.loadModels("../db/import/rolesAndPermission.yml");
		}
		if(VacationCode.count() == 0){
			Fixtures.loadModels("../db/import/vacationCode.yml");
		}
		if(StampType.count() == 0){
			Fixtures.loadModels("../db/import/stampType.yml");
		}
		if(StampModificationType.count() == 0){
			Fixtures.loadModels("../db/import/stampModificationType.yml");
		}
		if(CompetenceCode.count() == 0){
			Fixtures.loadModels("../db/import/competenceCode.yml");
		}
		
//		Allinea tutte le sequenze del db
		
		Fixtures.executeSQL(Play.getFile("db/import/fix_sequences.sql"));
		
		fixUserPermission.doJob();

		//prendere tutte le persone che a oggi non hanno inizializzazione e crearne una vuota
		//Tutte le persone con contratto iniziato dopo alla data di inizializzazione
		// devono avere la inizializzazione al giorno prima.
		List<Person> persons = Person.findAll();
		for (Person person : persons) {

			//Contratto attuale
			Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
			if (!contract.isPresent()) {
				continue;
			}
			
			IWrapperContract wcontract = wrapperFactory.create(contract.get());
			if (wcontract.initializationMissing()) {
			
				Contract c = contract.get();
				c.sourceDateResidual = new LocalDate(wcontract.dateForInitialization());
				c.sourcePermissionUsed = 0;
				c.sourceRecoveryDayUsed = 0;
				c.sourceRemainingMealTicket = 0;
				c.sourceRemainingMinutesCurrentYear = 0;
				c.sourceRemainingMinutesLastYear = 0;
				c.sourceVacationCurrentYearUsed = 0;
				c.sourceVacationLastYearUsed = 0;
				c.sourceByAdmin = false;
				c.save();
				
				consistencyManager.updatePersonSituation(person.id, c.sourceDateResidual);
			}
		}

	}
	
	

}
