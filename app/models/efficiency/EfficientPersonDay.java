package models.efficiency;

import it.cnr.iit.epas.DateUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import models.Person;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDate;

import play.Play;

/**
 * 	Classe che modella un person day calcolato attraverso PreparedStatement che permette di 
 * aumentare l'efficienza dei controller di riepilogo
 * @author alessandro
 *
 */
public class EfficientPersonDay {
	private static Connection connection = null;
	
	public String competenceCode;		//filled by preparedStatement
	public int valueApproved;
	public LocalDate date;
	public int timeAtWork;
	public int difference;
	public boolean fixed;
	public List<String> way;
	public Timestamp lastTime;
	public String justified;
	
	public int mealTicketTime;			//filled by WorkingTimeTypeDay person object
	public boolean isHoliday;
	
	public boolean isProperSequence;	//filled by adding ways
	
		
	/**
	 * Costruisce la struttura dati lista dei EfficientPersonDay per la persona, per il calcolo efficiente del riepilogo per quella persona 
	 * nelle date incluse fra dateBegin e dateEnd (estremi compresi) 
	 * @param connection
	 * @param person
	 * @param dateBegin
	 * @param dateEnd
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException 
	 */
	public static List<EfficientPersonDay> getEfficientPersonDays(Person person, LocalDate dateBegin, LocalDate dateEnd ) throws SQLException, ClassNotFoundException
	{
		//prepared statement
		if(EfficientPersonDay.connection == null)
		{
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection(
							Play.configuration.getProperty("db.new.url"),
							Play.configuration.getProperty("db.new.user"),
							Play.configuration.getProperty("db.new.password"));
		}

		List<WorkingTimeTypeDay> personWttd = person.workingTimeType.workingTimeTypeDays;
	
		List<EfficientPersonDay> personDayRsList = new ArrayList<EfficientPersonDay>();
		EfficientPersonDay actualPersonDayRs = null;
	
		String query =    
				"SELECT p.id, compcode.code, comp.valueapproved, pd.date, pd.time_at_work, pd.difference, sp.fixedworkingtime, s.way, s.date as time, abt.justified_time_at_work "
						+ "FROM persons p LEFT OUTER JOIN competences comp ON p.id = comp.person_id AND comp.year = ? AND comp.month = ? AND p.id = ? "
						+ "LEFT OUTER JOIN competence_codes compcode ON comp.competence_code_id = compcode.id AND compcode.code = ? "
						+ "LEFT OUTER JOIN person_days pd ON p.id = pd.person_id AND pd.date BETWEEN ? AND ? "
						+ "LEFT OUTER JOIN stamp_profiles sp ON sp.person_id = p.id "
						+ "LEFT OUTER JOIN stampings s ON s.personday_id = pd.id "
						+ "LEFT OUTER JOIN absences abs ON abs.personday_id = pd.id "
						+ "LEFT OUTER JOIN absence_types abt ON abs.absence_type_id = abt.id "
						+ "WHERE  (( pd.date BETWEEN sp.start_from AND sp.end_to) OR ( pd.date >= sp.start_from AND sp.end_to IS NULL)) AND compcode.code IS NOT null "
						+ "ORDER BY p.id, pd.date, s.date;";
		
		PreparedStatement ps = connection.prepareStatement(query);
		ps.setInt(1, dateBegin.getYear());
		ps.setInt(2, dateBegin.getMonthOfYear());
		ps.setLong(3, person.id);
		ps.setString(4, "S1");
		ps.setTimestamp(5,  new Timestamp(dateBegin.toDateMidnight().getMillis()));
		ps.setTimestamp(6,  new Timestamp(dateEnd.toDateMidnight().getMillis()));
	
		ResultSet rs = ps.executeQuery();
	
		while(rs.next())
		{
			String competenceCode = rs.getString("code");
			int valueApproved = rs.getInt("valueapproved");
			LocalDate date = new LocalDate(rs.getTimestamp("date").getTime());
			int timeAtWork = rs.getInt("time_at_work");
			int difference = rs.getInt("difference");
			boolean fixing = rs.getBoolean("fixedworkingtime");
			String way = rs.getString("way");
			Timestamp time =  rs.getTimestamp("time");
			String justified = rs.getString("justified_time_at_work");
			if(actualPersonDayRs==null)
			{
				actualPersonDayRs = new EfficientPersonDay(competenceCode, valueApproved, date, timeAtWork, difference, fixing, justified, way, time, personWttd);
				continue;
			}
			if( actualPersonDayRs.date.isEqual(date))
			{
				if(way!=null)
					actualPersonDayRs.addWay(way, time);
				continue;
			}
			if(!actualPersonDayRs.date.isEqual(date))
			{
				personDayRsList.add(actualPersonDayRs);
				actualPersonDayRs = new EfficientPersonDay(competenceCode, valueApproved, date, timeAtWork, difference, fixing, justified, way, time, personWttd);
				continue;
			}
		}
		if(actualPersonDayRs!=null)
		{
			personDayRsList.add(actualPersonDayRs);
		}
		//EfficientPersonDay.connection.close();
		return personDayRsList;
	}

	/**
	 * Costruttore privato
	 * @param competenceCode
	 * @param valueApproved
	 * @param date
	 * @param timeAtWork
	 * @param fixed
	 * @param justified
	 * @param way
	 * @param time
	 * @param personWttd
	 */
	private EfficientPersonDay(
			String competenceCode, int valueApproved, 
			LocalDate date, int timeAtWork, int difference, boolean fixed, String justified, String way, Timestamp time,
			List<WorkingTimeTypeDay> personWttd)
	{
		this.competenceCode = competenceCode;
		this.valueApproved = valueApproved;
		this.date = date;
		this.timeAtWork = timeAtWork;
		this.difference = difference;
		this.fixed = fixed;
		this.justified = justified;
		if(this.justified==null)
			this.justified="";
		this.way = new ArrayList<String>();
		this.addWay(way, time);
		
		this.mealTicketTime = personWttd.get(date.getDayOfWeek()-1).mealTicketTime;
		if(DateUtility.isGeneralHoliday(this.date) || personWttd.get(this.date.getDayOfWeek()-1).holiday==true)
		{
			this.isHoliday = true;
		}
		else
		{
			this.isHoliday = false;
		}
		
	}
	
	/**
	 * Aggiunge una nuova stamping way alla sequenza
	 * @param way
	 * @param time
	 */
	private void addWay(String way, Timestamp time)
	{
		if(way!=null)
		{
			if(lastTime==null)	//prima stamping
			{
				this.way.add(way);
				lastTime = time;
				this.isProperSequence = checkProperSequence(this.way);
				return;
			}
			//stamping successiva 
			//(se ha stessa data e stessa way della precedente la scarto perchè è un duplicato causato dalla join con absences)
			if(this.way.get(this.way.size()-1).equals(way) && time.compareTo(lastTime)==0)
			{
				return;
			}
			this.way.add(way);
			lastTime = time;
			this.isProperSequence = checkProperSequence(this.way);
			return;

		}
	}
	
	/**
	 * Controlla che la sequenza di stamping sia corretta
	 * @return
	 */
	private static boolean checkProperSequence(List<String> ways)
	{
		if(ways.size()==0)		//zero timbrature
			return false;
		if(ways.size()%2==1)	//timbrature dispari
			return false;
		String lastWay = "out";
		for(String way : ways)
		{
			if(way.equals(lastWay))	//timbrature uguali consecutive
				return false;
			lastWay = way;
		}
		return true;
	}
}

