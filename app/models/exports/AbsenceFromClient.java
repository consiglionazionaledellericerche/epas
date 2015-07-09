package models.exports;

import models.BadgeReader;
import models.StampType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

public class AbsenceFromClient {

	public Long personId;
	public LocalDate date;
	public String code;
	
	public int inizio;
	public int fine;
	public int durata;
	
	public String tipog;
}
