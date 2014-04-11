package procedure.evolutions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import models.Contract;
import models.Person;
import models.PersonWorkingTimeType;
import models.VacationCode;
import models.VacationPeriod;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import play.Logger;
import play.Play;

public class Evolutions {

	public static void updateVacationPeriodRelation() {

		List<Person> personList = Person.getActivePersons(new LocalDate());
		for(Person p : personList){
			Logger.debug("Cerco i contratti per %s %s", p.name, p.surname);
			List<Contract> contractList = Contract.find("Select c from Contract c where c.person = ?", p).fetch();
			for(Contract con : contractList){
				Logger.debug("Sto analizzando il contratto %s", con.toString());
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
		}

	}

	public static void updateWorkingTimeTypeRelation() throws ClassNotFoundException, SQLException{
		List<Person> personList = Person.getActivePersons(new LocalDate());
		for(Person p : personList){
			Contract c = p.getCurrentContract();
			Connection connection = null;
			String sql = "Select working_time_type_id from persons where id = ".concat(p.id+";");
			if(connection == null)
			{
				Class.forName("org.postgresql.Driver");
				connection = DriverManager.getConnection(
						Play.configuration.getProperty("db.new.url"),
						Play.configuration.getProperty("db.new.user"),
						Play.configuration.getProperty("db.new.password"));
			}
			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			try{
				while(rs.next()){
					Long wttId = rs.getLong("working_time_type_id");
					WorkingTimeType wtt = WorkingTimeType.findById(wttId);
					PersonWorkingTimeType pwtt = new PersonWorkingTimeType();
					pwtt.person = p;
					pwtt.workingTimeType = wtt;
					pwtt.beginDate = c.beginContract;
					pwtt.endDate = null;
					pwtt.save();
					Logger.debug("Inserito person_working_time_type per %s %s con date %s %s", p.name, p.surname, pwtt.beginDate, pwtt.endDate);
				}
			}
			catch(Exception e){
				Logger.debug("Bene ma non benissimo per %s %s", p.name, p.surname);
			}
		}
	}

}
