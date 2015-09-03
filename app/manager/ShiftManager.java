package manager;

import helpers.BadRequest;
import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.DateUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import models.Absence;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftCategories;
import models.ShiftType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.ShiftSlot;
import models.exports.AbsenceShiftPeriod;
import models.exports.ShiftPeriod;
import models.exports.ShiftPeriods;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.Logger;
import play.db.jpa.JPA;
import play.i18n.Messages;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;


/**
 * 
 * @authorArianna
 *
 */
public class ShiftManager {

	private PersonDayManager personDayManager;
	
	public static String thNoStampings = Messages.get("PDFReport.thNoStampings");  		// nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thBadStampings = Messages.get("PDFReport.thBadStampings");  	// nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
	public static String thMissingTime = Messages.get("PDFReport.thMissingTime");		// 
	public static String thIncompleteTime = Messages.get("PDFReport.thIncompleteTime");
	public static String thWarnStampings = Messages.get("PDFReport.thWarnStampings");  	// nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma 
																						// con le ore lavorate che discostano meno di 2 ore
	public static String thLackTime = Messages.get("PDFReport.thLackTime");
	public static String thMissions = Messages.get("PDFReport.thMissions");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thAbsences = Messages.get("PDFReport.thAbsences");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	
	private final static String codShift = "T1";			// codice dei turni
	

	// shift day
	public final static class SD {
		Person mattina;
		Person pomeriggio;

		public SD (Person mattina, Person pomeriggio) {
			this.mattina = mattina;
			this.pomeriggio = pomeriggio;
		}
	}

	@Inject
	private PersonShiftDayDao personShiftDayDao;
	@Inject
	private PersonDayDao personDayDao;
	@Inject
	private AbsenceDao absenceDao;
	@Inject
	private ShiftDao shiftDao;
	@Inject
	private CompetenceUtility competenceUtility;
	@Inject
	private CompetenceCodeDao competenceCodeDao;
	@Inject
	private CompetenceDao competenceDao;
	@Inject
	private PersonMonthRecapDao personMonthRecapDao;


	/*
	 * Costruisce la tabella delle inconsistenza tra i giorni di turno dati e le timbrature
	 * 
	 * @param personShiftDays			- lista di giorni di turno (PersonShiftDay)
	 * @param inconsistentAbsenceTable	- tabella di tipo Person, String, List<String> che contiene le eventuali inconsistenze rilevate
	 * 									  per ogni persona che ha effettuato almeno un turno nella lista:
	 * 										- String: è una label della tabella tra: 
	 * 										- List<String> 
	 * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence, thBadStampings], [List<'gg MMM'>, List<'gg MMM'>, 'dd MMM -> HH:mm-HH:mm']) 
	 */
	public void getShiftInconsistencyTimestampTable(List<PersonShiftDay> personShiftDays, Table<Person, String, List<String>> inconsistentAbsenceTable) {

		// lista dei giorni di assenza nel mese, mancata timbratura e timbratura inconsistente	
		List<String> noStampingDays = new ArrayList<String>();		// mancata timbratura
		List<String> badStampingDays = new ArrayList<String>();		// timbrature errate
		List<String> absenceDays = new ArrayList<String>();			// giorni di assenza
		List<String> lackOfTimes = new ArrayList<String>();			// tempo mancante


		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;

			// legge l'orario di inizio e fine turno da rispettare (mattina o pomeriggio)
			LocalTime startShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorning : personShiftDay.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorning : personShiftDay.shiftType.shiftTimeTable.endAfternoon;

			// legge l'orario di inizio e fine pausa pranzo del turno
			LocalTime startLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.startAfternoonLunchTime;
			LocalTime endLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.endAfternoonLunchTime;

			//Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

			// Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
			LocalTime roundedStartShift = startShift.plusMinutes(15);

			Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

			//check for the absence inconsistencies
			//------------------------------------------
			Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personShiftDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personShiftDay.date, person).first();
			Logger.debug("Prelevo il personDay %s per la persona %s", personShiftDay.date, person.surname);

			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent()) {	
				
				if (!personDay.get().isHoliday && personShiftDay.date.isBefore(LocalDate.now())) {
					Logger.info("Il turno di %s %s √® incompatibile con la sua mancata timbratura nel giorno %s (personDay == null)", person.name, person.surname, personShiftDay.date);

					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personShiftDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

					Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
				}
			} else {

				// check for the stampings in working days
				if (!personDay.get().isHoliday && LocalDate.now().isAfter(personShiftDay.date)) {

					// check no stampings
					//-----------------------------
					if (personDay.get().stampings.isEmpty()) {
						Logger.info("Il turno di %s %s √® incompatibile con la sue mancate timbrature nel giorno %s", person.name, person.surname, personDay.get().date);


						noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
						noStampingDays.add(personShiftDay.date.toString("dd MMM"));
						inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

						Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
					} else {
						// check consistent stampings
						//-----------------------------
						//Logger.debug("Legge le coppie di timbrature valide");
						// legge le coppie di timbrature valide 
						//FIXME injettare il PersonDayManager
						List<PairStamping> pairStampings = personDayManager.getValidPairStamping(personDay.get());

						//Logger.debug("Dimensione di pairStampings =%s", pairStampings.size());

						// se c'e' una timbratura guardo se e' entro il turno
						if ((personDay.get().stampings.size() == 1) &&
								((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date.toLocalTime().isAfter(roundedStartShift)) || 
										(personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date.toLocalTime().isBefore(roundedStartShift)) )) {


							String stamp = (personDay.get().stampings.get(0).isIn()) ? personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm").concat("- **:**") : "- **:**".concat(personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm"));

							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

							// se e' vuota => manca qualche timbratura		
						} else if (pairStampings.isEmpty()) {							

							Logger.info("Il turno di %s %s e incompatibile con la sue  timbrature disallineate nel giorno %s", person.name, person.surname, personDay.get().date);

							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

							// controlla che le coppie di timbrature coprano
							// gli intervalli di prima e dopo pranzo
						} else {

							//Logger.debug("Controlla le timbrature");
							boolean okBeforeLunch = false;  	// intervallo prima di pranzo coperto
							boolean okAfterLunch = false;		// intervallo dopo pranzo coperto

							String strStamp = "";

							// per ogni coppia di timbrature
							for (PairStamping pairStamping : pairStampings) {

								strStamp = strStamp.concat(pairStamping.in.date.toString("HH:mm")).concat(" - ").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");
								Logger.debug("Controllo la coppia %s", strStamp);

								// controlla se la coppia di timbrature interseca l'intervallo prima e dopo pranzo del turno
								// if (pairStamping.out.date.toLocalTime().isAfter(startLunchTime) && pairStamping.in.date.toLocalTime().isBefore(startShift)) {
								if (!pairStamping.out.date.toLocalTime().isBefore(startLunchTime) && !pairStamping.in.date.toLocalTime().isAfter(startShift)) {
									okBeforeLunch = true;
									//Logger.debug("okBeforeLunch=%s", okBeforeLunch);
								}
								// if (pairStamping.out.date.toLocalTime().isAfter(endShift) && pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) {
								if (!pairStamping.out.date.toLocalTime().isBefore(endShift) && !pairStamping.in.date.toLocalTime().isAfter(endLunchTime)) {
									okAfterLunch = true;
									//Logger.debug("okAfterLunch=%s", okAfterLunch);
								} 
							}

							// se non ha coperto interamente i due intervalli, controlla se il tempo mancante al
							// completamento del turno sia <= 2 ore
							if (!okBeforeLunch || !okAfterLunch) {

								int workingMinutes = 0;
								LocalTime lowLimit;
								LocalTime upLimit;
								LocalTime newLimit;

								// scostamenti delle timbrature dalle fasce del turno
								int diffStartShift = 0;
								int diffStartLunchTime = 0;
								int diffEndLunchTime = 0;
								int diffEndShift = 0;

								boolean inTolleranceLimit = true;	// ingressi  euscite nella tolleranza dei 15 min

								String stampings = "";

								Logger.info("Il turno di %s  nel giorno %s non √® stato completato o c'e' stata una uscita fuori pausa pranzo - orario %s", person, personDay.get().date, strStamp);
								Logger.debug("Esamino le coppie di timbrature");

								// per ogni coppia di timbrature
								for (PairStamping pairStamping : pairStampings) {

									//Logger.debug("pairStamping.in.date = %s  pairStamping.out.date = %s", pairStamping.in.date.toLocalTime(), pairStamping.out.date.toLocalTime());

									// l'intervallo di tempo lavorato interseca la parte del turno prima di pranzo
									if ((pairStamping.in.date.toLocalTime().isBefore(startShift) && pairStamping.out.date.toLocalTime().isAfter(startShift)) ||
											(pairStamping.in.date.toLocalTime(). isAfter(startShift) && pairStamping.in.date.toLocalTime().isBefore(startLunchTime))) {

										// conta le ore lavorate in turno prima di pranzo
										lowLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift)) ? startShift : pairStamping.in.date.toLocalTime();
										upLimit = (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) ? pairStamping.out.date.toLocalTime() : startLunchTime;
										workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										Logger.debug("N.1 - ss=%s -- slt=%s lowLimit=%s upLimit=%s workingMinutes=%s", startShift, startLunchTime, lowLimit, upLimit, workingMinutes);

										// calcola gli scostamenti dalla prima fascia del turno tenendo conto dei 15 min di comporto
										// se il turnista è entrato prima
										if (pairStamping.in.date.toLocalTime().isBefore(startShift)) {
											newLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift.minusMinutes(15))) ? startShift.minusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isBefore(startShift.minusMinutes(15))) { inTolleranceLimit = false;}
										} else {
											// è entrato dopo
											newLimit = (pairStamping.in.date.toLocalTime().isAfter(startShift.plusMinutes(15))) ? startShift.plusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isAfter(startShift.plusMinutes(15))) {inTolleranceLimit = false;}
										}
										diffStartShift = DateUtility.getDifferenceBetweenLocalTime(newLimit, startShift);
										Logger.debug("diffStartShift=%s", diffStartShift);

										// calcola gli scostamenti dell'ingresso in pausa pranzo tenendo conto dei 15 min di comporto
										// se il turnista è andato a  pranzo prima
										if (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) {
											Logger.debug("vedo uscita per pranzo prima");
											newLimit = (startLunchTime.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? startLunchTime.minusMinutes(15) : pairStamping.out.date.toLocalTime();
											diffStartLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, startLunchTime);
											if (startLunchTime.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
										} else if (pairStamping.out.date.toLocalTime().isBefore(endLunchTime)) {
											// è andato a pranzo dopo
											Logger.debug("vedo uscita per pranzo dopo");
											newLimit = (startLunchTime.plusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? pairStamping.out.date.toLocalTime() : startLunchTime.plusMinutes(15);
											if (startLunchTime.plusMinutes(15).isBefore(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
											diffStartLunchTime = DateUtility.getDifferenceBetweenLocalTime(startLunchTime, newLimit); /* ? */
										}

										Logger.debug("diffStartLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", startLunchTime, newLimit, diffStartLunchTime);
									}

									// l'intervallo di tempo lavorato interseca la parte del turno dopo pranzo
									if ((pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.out.date.toLocalTime().isAfter(endLunchTime)) ||
											(pairStamping.in.date.toLocalTime().isAfter(endLunchTime) && pairStamping.in.date.toLocalTime().isBefore(endShift)))  {

										// conta le ore lavorate in turno dopo pranzo
										lowLimit = (pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) ? endLunchTime : pairStamping.in.date.toLocalTime();
										upLimit = (pairStamping.out.date.toLocalTime().isBefore(endShift)) ? pairStamping.out.date.toLocalTime() : endShift;
										workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										Logger.debug("N.2 - elt=%s --- es=%s  slowLimit=%s upLimit=%s workingMinutes=%s", endLunchTime, endShift, lowLimit, upLimit, workingMinutes);

										// calcola gli scostamenti dalla seconda fascia del turno tenendo conto dei 15 min di comporto
										// se il turnista è rientrato prima dalla pausa pranzo
										if (pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.in.date.toLocalTime().isAfter(startLunchTime)) {
											Logger.debug("vedo rientro da pranzo prima");
											newLimit = (endLunchTime.minusMinutes(15).isAfter(pairStamping.in.date.toLocalTime())) ?  endLunchTime.minusMinutes(15) : pairStamping.in.date.toLocalTime();
											diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
											Logger.debug("diffEndLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", newLimit, endLunchTime, diffEndLunchTime);
										} else if (pairStamping.in.date.toLocalTime().isBefore(endShift) && pairStamping.in.date.toLocalTime().isAfter(endLunchTime)) {
											// è rientrato dopo
											Logger.debug("vedo rientro da pranzo dopo");
											newLimit = (pairStamping.in.date.toLocalTime().isAfter(endLunchTime.plusMinutes(15))) ? endLunchTime.plusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isAfter(endLunchTime.plusMinutes(15))) {inTolleranceLimit = false;}
											diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
											Logger.debug("diffEndLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", endLunchTime, newLimit, diffEndLunchTime);
										}


										// se il turnista è uscito prima del turno
										if (pairStamping.out.date.toLocalTime().isBefore(endShift)) {
											Logger.debug("vedo uscita prima della fine turno");
											newLimit = (endShift.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? endShift.minusMinutes(15) : pairStamping.out.date.toLocalTime();
											if (endShift.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
										} else {
											Logger.debug("vedo uscita dopo la fine turno");
											// il turnista è uscito dopo la fine del turno
											newLimit = (pairStamping.out.date.toLocalTime().isAfter(endShift.plusMinutes(15))) ? endShift.plusMinutes(15) : pairStamping.out.date.toLocalTime();
										}
										diffEndShift = DateUtility.getDifferenceBetweenLocalTime(endShift, newLimit);
										Logger.debug("diffEndShift=%s", diffEndShift);
									}		

									// write the pair stamping								
									stampings = stampings.concat(pairStamping.in.date.toString("HH:mm")).concat("-").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");									 
								}

								stampings.concat("<br />");

								// controllo eventuali compensazioni di minuti in  ingresso e uscita
								//--------------------------------------------------------------------
								int restoredMin = 0;


								// controlla pausa pranzo:
								// - se è uscito prima dell'inizio PP (è andato a pranzo prima)
								if (diffStartLunchTime < 0) {
									Logger.debug("sono entrata in pausa pranzo prima! diffStartLunchTime=%s", diffStartLunchTime);
									// controlla se è anche rientrato prima dalla PP e compensa
									if (diffEndLunchTime > 0) {
										Logger.debug("E rientrato prima dalla pusa pranzo! diffEndLunchTime=%s", diffEndLunchTime);
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffEndLunchTime)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

										diffStartLunchTime = ((diffStartLunchTime + diffEndLunchTime) > 0) ? 0 : diffStartLunchTime + diffEndLunchTime;
										diffEndLunchTime = ((diffStartLunchTime + diffEndLunchTime) > 0) ? diffStartLunchTime + diffEndLunchTime : 0;

									} 
									// se necessario e se è entrato prima, compensa con l'ingresso 
									if ((diffStartLunchTime < 0) && (diffStartShift > 0)) {
										Logger.debug("E entrato anche prima! diffStartShift=%s", diffStartShift);
										// cerca di compensare con l'ingresso
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffStartShift));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffStartShift));

										diffStartLunchTime = ((diffStartLunchTime + diffStartShift) > 0) ? 0 : diffStartLunchTime + diffStartShift;
										diffStartShift = ((diffStartLunchTime + diffStartShift) > 0) ? diffStartLunchTime + diffStartShift : 0;
									}
								}

								// - se è entrato dopo la fine della pausa pranzo
								if (diffEndLunchTime < 0) {
									Logger.debug("E entrato in ritardo dalla apusa pranzo! diffEndLunchTime=%s", diffEndLunchTime);
									// controlla che sia entrata dopo in pausa pranzo
									if (diffStartLunchTime > 0) {
										Logger.debug("e andata anche dopo in pausa pranzo! diffStartLunchTime=%s", diffStartLunchTime);
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffEndLunchTime)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

										diffEndLunchTime = ((diffEndLunchTime + diffStartLunchTime) > 0) ? 0 : diffEndLunchTime + diffStartLunchTime;
										diffStartLunchTime = ((diffEndLunchTime + diffStartLunchTime) > 0) ? diffEndLunchTime + diffStartLunchTime : 0;

									} 
									// se necessario e se è uscito dopo, compensa con l'uscita
									if ((diffEndLunchTime < 0) && (diffEndShift > 0)) {
										Logger.debug("e' uscito dopo! diffEndShift=%s", diffEndShift);
										// cerca di conpensare con l'uscita (è uscito anche dopo)
										restoredMin += Math.min(Math.abs(diffEndLunchTime), Math.abs(diffEndShift));
										Logger.debug("restoredMin=%s Math.abs(diffEndLunchTime)=%s Math.abs(diffEndShift)=%s", restoredMin, Math.abs(diffEndLunchTime), Math.abs(diffEndShift));

										diffEndLunchTime = ((diffEndLunchTime + diffEndShift) > 0) ? 0 : diffEndLunchTime + diffEndShift;
										diffEndShift = ((diffEndLunchTime + diffEndShift) > 0) ? diffEndLunchTime + diffEndShift : 0;	
									}
								}

								// controlla eventuali compensazioni di ingresso e uscita
								// controlla se è uscito dopo
								if ((diffStartShift < 0) && (diffEndShift > 0)) {
									Logger.debug("e entrato dopo ed è uscito dopo! diffStartShift=%s diffEndShift=%s", diffStartShift, diffEndShift);
									restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift)); 
									Logger.debug("restoredMin=%s Math.abs(diffEndShift)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

									diffStartShift = ((diffEndShift + diffStartShift) > 0) ? 0 : diffEndShift + diffStartShift;
									diffEndShift = ((diffEndShift + diffStartShift) > 0) ? diffEndShift + diffStartShift : 0;

								} else if ((diffEndShift < 0) && (diffStartShift > 0)) {
									Logger.debug("e uscito prima ed è entrato dopo! diffStartShift=%s diffEndShift=%s", diffStartShift, diffEndShift);
									restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift)); 
									Logger.debug("restoredMin=%s Math.abs(diffEndShift)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

									diffEndShift = ((diffEndShift + diffStartShift) > 0) ? 0 : diffEndShift + diffStartShift;
									diffStartShift = ((diffEndShift + diffStartShift) > 0) ? diffEndShift + diffStartShift : 0;

								}

								Logger.debug("Minuti recuperati: %s", restoredMin);

								// check if the difference between the worked hours in the shift periods are less than 2 hours (new rules for shift)
								int twoHoursinMinutes = 2 * 60;
								int teoreticShiftMinutes = DateUtility.getDifferenceBetweenLocalTime(startShift, startLunchTime) + DateUtility.getDifferenceBetweenLocalTime(endLunchTime, endShift);
								int lackOfMinutes = teoreticShiftMinutes - workingMinutes;

								Logger.debug("teoreticShiftMinutes = %s workingMinutes = %s lackOfMinutes = %s", teoreticShiftMinutes, workingMinutes, lackOfMinutes);
								lackOfMinutes -= restoredMin;
								workingMinutes+= restoredMin;

								Logger.debug("Minuti mancanti con recupero: %s - Minuti lavorati con recupero: %s", lackOfMinutes, workingMinutes);

								String lackOfTime = competenceUtility.calcStringShiftHoursFromMinutes(lackOfMinutes);
								String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(workingMinutes);
								String label;

								if (lackOfMinutes > twoHoursinMinutes) {

									Logger.info("Il turno di %s %s nel giorno %s non √® stato completato - timbrature: %s ", person.name, person.surname, personDay.get().date, stampings);

									badStampingDays = (inconsistentAbsenceTable.contains(person, thMissingTime)) ? inconsistentAbsenceTable.get(person, thMissingTime) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(workedTime).concat(" ore lavorate)"));
									inconsistentAbsenceTable.put(person, thMissingTime, badStampingDays);

									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thMissingTime, inconsistentAbsenceTable.get(person, thMissingTime));
								} else if (lackOfMinutes != 0) {
									label = (inTolleranceLimit) ? thIncompleteTime : thWarnStampings;

									Logger.info("Il turno di %s %s nel giorno %s non e'stato completato per meno di 2 ore (%s minuti (%s)) - CONTROLLARE PERMESSO timbrature: %s", person.name, person.surname, personDay.get().date, lackOfMinutes, lackOfTime, stampings);
									Logger.info("Timbrature nella tolleranza dei 15 min. = %s", inTolleranceLimit);

									badStampingDays = (inconsistentAbsenceTable.contains(person, label)) ? inconsistentAbsenceTable.get(person, label) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(lackOfTime).concat(" ore mancanti)"));

									lackOfTimes = (inconsistentAbsenceTable.contains(person, thLackTime)) ? inconsistentAbsenceTable.get(person, thLackTime): new ArrayList<String>();
									lackOfTimes.add(Integer.toString(lackOfMinutes));
									inconsistentAbsenceTable.put(person, label, badStampingDays);
									inconsistentAbsenceTable.put(person, thLackTime, lackOfTimes);

									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thLackTime, inconsistentAbsenceTable.get(person, thLackTime));
								}
							}
						} // fine controllo coppie timbrature
					} // fine if esistenza timbrature
				} // fine se non √® giorno festivo

				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					Logger.debug("E assente!!!! Esamino le assenze(%s)", personDay.get().absences.size());
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {

							if (absence.absenceType.code.equals("92")) {
								Logger.info("Il turno di %s %s √® coincidente con una missione il giorno %s", person.name, person.surname, personShiftDay.date);

								absenceDays = (inconsistentAbsenceTable.contains(person, thMissions)) ? inconsistentAbsenceTable.get(person, thMissions) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thMissions, absenceDays);

							} else {
								Logger.info("Il turno di %s %s √® incompatibile con la sua assenza nel giorno %s", person.name, person.surname, personShiftDay.date);


								/*absenceDays = (inconsistentAbsence.contains(personName, thAbsences)) ? inconsistentAbsence.get(personName, thAbsences) : new ArrayList<Integer>();							
								absenceDays.add(personShiftDay.date.getDayOfMonth());							
								inconsistentAbsence.put(personName, thAbsences, absenceDays);*/

								absenceDays = (inconsistentAbsenceTable.contains(person, thAbsences)) ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
							}
						}
					}
				}	
			} // fine personDay != null
		}
		//return inconsistentAbsenceTable;
	}
	
	/*
	 * @param personShiftDays lista dei giorni di turno di un certo tipo
	 * @return la lista dei periodi di turno lavorati 
	 */
	public List<ShiftPeriod> getPersonShiftPeriods (List<PersonShiftDay> personShiftDays) {

		List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
		ShiftPeriod shiftPeriod = null;

		for (PersonShiftDay psd : personShiftDays) {	

			LocalTime startShift = (psd.shiftSlot.equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.startMorning : psd.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (psd.getShiftSlot().equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.endMorning : psd.shiftType.shiftTimeTable.endAfternoon;

			if (shiftPeriod == null || !shiftPeriod.person.equals(psd.personShift.person) || !shiftPeriod.end.plusDays(1).equals(psd.date) || !shiftPeriod.startSlot.equals(startShift)){
				shiftPeriod = new ShiftPeriod(psd.personShift.person, psd.date, psd.date, psd.shiftType, false, psd.shiftSlot, startShift, endShift);
				shiftPeriods.add(shiftPeriod);
				Logger.debug("\nCreato nuovo shiftPeriod, person=%s, start=%s, end=%s, type=%s, fascia=%s, orario=%s - %s" , shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
			} else {
				shiftPeriod.end = psd.date;
				Logger.debug("Aggiornato ShiftPeriod, person=%s, start=%s, end=%s, type=%s, fascia=%s, orario=%s - %s", shiftPeriod.person, shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type, shiftPeriod.shiftSlot, startShift.toString("HH:mm"), endShift.toString("HH:mm"));
			}
		}

		return shiftPeriods;
	}


	/*
	 * @param personShiftDays lista dei giorni di turno di un certo tipo
	 * @return la lista dei periodi di turno lavorati 
	 */
	public List<ShiftPeriod> getDeletedShiftPeriods (List<ShiftCancelled> personShiftCancelled) {

		List<ShiftPeriod> shiftPeriods = new ArrayList<ShiftPeriod>();
		ShiftPeriod shiftPeriod = null;


		for (ShiftCancelled sc : personShiftCancelled) {
			if (shiftPeriod == null || !shiftPeriod.end.plusDays(1).equals(sc.date)){
				shiftPeriod = new ShiftPeriod(sc.date, sc.date, sc.type, true);
				shiftPeriods.add(shiftPeriod);
				Logger.trace("Creato nuovo shiftPeriod di cancellati, start=%s, end=%s, type=%s" , shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
			} else {
				shiftPeriod.end = sc.date;
				Logger.trace("Aggiornato ShiftPeriod di cancellati, start=%s, end=%s, type=%s\n", shiftPeriod.start, shiftPeriod.end, shiftPeriod.shiftType.type);
			}
		}

		return shiftPeriods;
	}

	/* Salva nel database i giorni di turno lavorati e cancellati contenuti nella lista di periodi 
	 * di turno passati come parametro
	 * 
	 * @param shiftType - tipo dei turni che compongono  periodi d turno passati come arametro
	 * @param year		- anno nel quale sono stati lavorati i turni
	 * @param month		- mese nel quale sono stati lavorati i turni
	 * @param shiftPeriods - lista di periodi di turno lavorati e cancellati
	 * @return 			-	
	 */
	public void savePersonShiftDaysFromShiftPeriods(ShiftType shiftType, Integer year, Integer month, ShiftPeriods shiftPeriods) {
		//Il mese e l'anno ci servono per "azzerare" eventuale giorni di turno rimasti vuoti
		LocalDate monthToManage = new LocalDate(year, month, 1);

		//Conterrà i giorni del mese che devono essere attribuiti a qualche turnista 
		Set<Integer> daysOfMonthToAssign = new HashSet<Integer>();	
		Set<Integer> daysOfMonthForCancelled = new HashSet<Integer>();	

		for (int i = 1 ; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
			daysOfMonthToAssign.add(i);
			daysOfMonthForCancelled.add(i);
		}
		Logger.trace("Lista dei giorni del mese = %s", daysOfMonthToAssign);

		LocalDate day = null;
		for (ShiftPeriod shiftPeriod : shiftPeriods.periods) {

			// start and end date validation
			if (shiftPeriod.start.isAfter(shiftPeriod.end)) {
				throw new IllegalArgumentException(
						String.format("ShiftPeriod person.id = %s has start date %s after end date %s", shiftPeriod.person.id, shiftPeriod.start, shiftPeriod.end));
			}

			day = shiftPeriod.start;
			while (day.isBefore(shiftPeriod.end.plusDays(1))) {
				// normal shift
				if (!shiftPeriod.cancelled) {
					//La persona deve essere tra i turnisti 
					//PersonShift personShift = PersonShift.find("SELECT ps FROM PersonShift ps WHERE ps.person = ?", shiftPeriod.person).first();
					Logger.debug("---Prende il personShift di %s", shiftPeriod.person);
					PersonShift personShift = personShiftDayDao.getPersonShiftByPerson(shiftPeriod.person);
					if (personShift == null) {
						throw new IllegalArgumentException(
								String.format("Person %s is not a shift person", shiftPeriod.person));
					}

					//Se la persona è assente in questo giorno non può essere in turno (almeno che non sia cancellato)
					//if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, shiftPeriod.person).fetch().size() > 0) {
					if(absenceDao.getAbsencesInPeriod(Optional.fromNullable(shiftPeriod.person), day, Optional.<LocalDate>absent(), false).size() > 0){
						String msg = String.format("Assenza incompatibile di %s %s per il giorno %s", shiftPeriod.person.name, shiftPeriod.person.surname, day);

						BadRequest.badRequest(msg);
						//throw new HttpStatusException(msg, 400, "");	
						//throw new IllegalArgumentException(msg);	
					}

					//Salvataggio del giorno di turno
					//Se c'è un turno già presente viene sostituito, altrimenti viene creato un PersonShiftDay nuovo
					Logger.debug("Cerco turno shiftType = %s AND date = %s AND shiftSlot = %s", shiftType.description, day, shiftPeriod.shiftSlot);

					PersonShiftDay personShiftDay = personShiftDayDao.getPersonShiftDayByTypeDateAndSlot(shiftType, day, shiftPeriod.shiftSlot);
					//	PersonShiftDay.find("shiftType = ? AND date = ? AND shiftSlot = ?", shiftType, day, shiftPeriod.shiftSlot).first();
					if (personShiftDay == null) {
						personShiftDay = new PersonShiftDay();
						Logger.debug("Creo un nuovo personShiftDay per person = %s, day = %s, shiftType = %s", shiftPeriod.person.name, day, shiftType.description);
					} else {
						Logger.debug("Aggiorno il personShiftDay = %s di %s", personShiftDay, personShiftDay.personShift.person.name);
					}
					personShiftDay.date = day;
					personShiftDay.shiftType = shiftType;
					personShiftDay.setShiftSlot(shiftPeriod.shiftSlot);
					personShiftDay.personShift = personShift;

					personShiftDay.save();
					Logger.info("Aggiornato PersonShiftDay = %s con %s\n", personShiftDay, personShiftDay.personShift.person);

					//Questo giorno è stato assegnato
					daysOfMonthToAssign.remove(day.getDayOfMonth());

				} else {
					// cancelled shift
					// Se non c'è già il turno cancellato lo creo
					Logger.debug("Cerco turno cancellato shiftType = %s AND date = %s", shiftType.type, day);
					ShiftCancelled shiftCancelled = shiftDao.getShiftCancelled(day, shiftType);
					//		ShiftCancelled.find("type = ? AND date = ?", shiftType, day).first();
					Logger.debug("shiftCancelled = %s", shiftCancelled);

					if (shiftCancelled == null) {
						shiftCancelled = new ShiftCancelled();
						shiftCancelled.date = day;
						shiftCancelled.type = shiftType;

						Logger.debug("Creo un nuovo ShiftCancelled=%s per day = %s, shiftType = %s", shiftCancelled, day, shiftType.description);

						shiftCancelled.save();
						Logger.debug("Creato un nuovo ShiftCancelled per day = %s, shiftType = %s", day, shiftType.description);
					}

					//Questo giorno è stato annullato
					daysOfMonthForCancelled.remove(day.getDayOfMonth());
				}

				day = day.plusDays(1);
			}
		}

		Logger.info("Turni da rimuovere = %s", daysOfMonthToAssign);

		for (int dayToRemove : daysOfMonthToAssign) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);

			int cancelled = JPA.em().createQuery("DELETE FROM PersonShiftDay WHERE shiftType = :shiftType AND date = :dateToRemove)")
					.setParameter("shiftType", shiftType)
					.setParameter("dateToRemove", dateToRemove)
					.executeUpdate();
			if (cancelled == 1) {
				Logger.info("Rimosso turno di tipo %s del giorno %s", shiftType.description, dateToRemove);
			}
		}

		Logger.info("Turni cancellati da rimuovere = %s", daysOfMonthForCancelled);

		for (int dayToRemove : daysOfMonthForCancelled) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);
			
			/*int cancelled = JPA.em().createQuery("DELETE FROM ShiftCancelled WHERE type = :shiftType AND date = :dateToRemove)")
				.setParameter("shiftType", shiftType)
				.setParameter("dateToRemove", dateToRemove)
				.executeUpdate(); */
			long cancelled = shiftDao.deleteShiftCancelled(shiftType, dateToRemove);

			if (cancelled == 1) {
				Logger.info("Rimosso turno cancellato di tipo %s del giorno %s", shiftType.description, dateToRemove);
			}
		}
	}


	/*
	 * Costruisce la lista dei periodi di assenza in turno da una lista di giorni di assenza
	 * @param absencePersonShiftDays	- lista dei giorni di assenza
	 * @param shiftType					- tipo del turno
	 * @return absenceShiftPeriods		- lista di periodi di assenza in turno
	 */
	public List<AbsenceShiftPeriod> getAbsentShiftPeriodsFromAbsentShiftDays(List<Absence> absencePersonShiftDays, ShiftType shiftType) {

		// List of absence periods
		List<AbsenceShiftPeriod> absenceShiftPeriods = new ArrayList<AbsenceShiftPeriod>();

		AbsenceShiftPeriod absenceShiftPeriod = null;
		for (Absence abs : absencePersonShiftDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (absenceShiftPeriod == null || !absenceShiftPeriod.person.equals(abs.personDay.person) || !absenceShiftPeriod.end.plusDays(1).equals(abs.personDay.date)) {
				absenceShiftPeriod = new AbsenceShiftPeriod(abs.personDay.person, abs.personDay.date, abs.personDay.date, (ShiftType) ShiftType.findById(shiftType.id));
				absenceShiftPeriods.add(absenceShiftPeriod);
				Logger.trace("Creato nuovo absenceShiftPeriod, person=%s, start=%s, end=%s", absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
			} else {
				absenceShiftPeriod.end = abs.personDay.date;
				Logger.trace("Aggiornato absenceShiftPeriod, person=%s, start=%s, end=%s", absenceShiftPeriod.person, absenceShiftPeriod.start, absenceShiftPeriod.end);
			}
		}

		return absenceShiftPeriods;
	}

	/*
	 * @author arianna
	 * Salva le ore di turno da retribuire di un certo mese nelle competenze.
	 * Per ogni persona riceve i giorni di turno effettuati nel mese e le eventuali ore non lavorate.
	 * Calcola le ore da retribuire sulla base dei giorni di turno sottraendo le eventuali ore non lavorate 
	 * e aggiungendo i minuti eventualemnte avanzati nel mese precedente. Le ore retribuite sono la parte intera
	 * delle ore calcolate. I minuti eccedenti sono memorizzati nella competenza per i mesi successivi
	 * @param  personsShiftHours	- contiene per ogni persona il numero dei giorni in turno lavorati (thDays) e
	 * 									gli eventuali minuti non lavorati (thLackTime) 
	 * @param year 					- anno di riferimento dei turni
	 * @param month					- mese di riferimento dei turni
	 * @return 						- la lista delle competenze corrispondenti ai turni lavorati 
	 */
	public List<Competence> updateDBShiftCompetences(Table<Person, String, Integer> personsShiftHours, int year, int month) {

		List<Competence> savedCompetences = new ArrayList<Competence>();
		int[] apprHoursAndExcMins; 

		String thDays = Messages.get("PDFReport.thDays");
		String thLackTime = Messages.get("PDFReport.thLackTime");

		// get the Competence code for the ordinary shift  
		CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
		//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();


		// for each person
		for (Person person: personsShiftHours.rowKeySet()) {

			Logger.debug("Registro dati di %s %s", person.surname, person.name);

			BigDecimal sessanta = new BigDecimal("60");

			Logger.debug("Calcolo le ore di turno dai giorni = %s", personsShiftHours.get(person, thDays));
			BigDecimal numOfHours = competenceUtility.calcShiftHoursFromDays(personsShiftHours.get(person, thDays));

			// compute the worked time in minutes of the present month
			int workedMins = (personsShiftHours.contains(person, thLackTime)) ? numOfHours.multiply(sessanta).subtract(new BigDecimal(personsShiftHours.get(person, thLackTime))).intValue() : numOfHours.multiply(sessanta).intValue();

			Logger.debug("Minuti lavorati = thReqHour * 60 - thLackTime = %s * 60 - %s", numOfHours, personsShiftHours.get(person, thLackTime));

			// compute the hours appproved and the exceede minutes on the basis of
			// the current worked minutes and the exceeded mins of the previous month
			apprHoursAndExcMins = calcShiftValueApproved(person, year, month, workedMins);

			// compute the value requested
			BigDecimal reqHours = competenceUtility.calcDecimalShiftHoursFromMinutes(workedMins);


			// save the FS reperibility competences in the DB
			Optional<Competence> shiftCompetence = competenceDao.getCompetence(person, year, month, competenceCode);
			//			Competence shiftCompetence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
			//					person, year, month, competenceCode).first();

			// update the requested hours
			if (shiftCompetence.isPresent()) {

				// check if the competence has been processed to be sent to Rome
				// and and this case we don't change the valueApproved
				CertificatedData certData = personMonthRecapDao.getCertificatedDataByPersonMonthAndYear(person, month, year);
				//CertificatedData certData = CertificatedData.find("SELECT cd FROM CertificatedData cd WHERE cd.person = ? AND cd.year = ? AND cd.month = ?", person, year, month).first();

				//Logger.debug("certData=%s isOK=%s competencesSent=%s", certData, certData.isOk, certData.competencesSent);

				int apprHours = (certData != null && certData.isOk && (certData.competencesSent != null)) ? shiftCompetence.get().valueApproved : apprHoursAndExcMins[0];
				int exceededMins = (certData != null && certData.isOk && (certData.competencesSent != null)) ? shiftCompetence.get().exceededMins : apprHoursAndExcMins[1];
				//Logger.debug("competenza √® inviata = %s vecchia valueApproved=%d, calcolata=%d salvata=%d", certData.isOk, shiftCompetence.valueApproved, calcApproved, calcApproved1);

				shiftCompetence.get().setValueApproved(apprHours);
				shiftCompetence.get().setValueRequested(reqHours);
				shiftCompetence.get().setExceededMin(exceededMins);
				shiftCompetence.get().save();

				Logger.debug("Aggiornata competenza di %s %s: valueRequested=%s, valueApproved=%s, exceddMins=%s", shiftCompetence.get().person.surname, shiftCompetence.get().person.name, shiftCompetence.get().valueRequested, shiftCompetence.get().valueApproved, shiftCompetence.get().exceededMins);

				savedCompetences.add(shiftCompetence.get());
			} else {
				// insert a new competence with the requested hours an reason
				Competence competence = new Competence(person, competenceCode, year, month);
				competence.setValueApproved(apprHoursAndExcMins[0]);
				competence.setExceededMin(apprHoursAndExcMins[1]);
				competence.setValueRequested(reqHours);
				competence.save();

				savedCompetences.add(competence);

				Logger.debug("Salvata competenza %s", shiftCompetence);
			}
		}

		// return the number of saved competences
		return savedCompetences;
	}


	/*
	 * @author arianna
	 * Aggiorna la tabella totalPersonShiftSumDays per contenere, per ogni persona nella lista dei turni personShiftDays,
	 * e per ogni tipo di turno trovato, il numero di giorni di turno effettuati
	 * 
	 * @param personShiftDays 			- lista di shiftDays
	 * @param totalPersonShiftSumDays	- tabella contenente il numero di giorni di turno effettuati per ogni persona
	 * 									  e tipologia di turno. Questa tabella viene aggiornata contando i giorni di
	 * 									  turno contenuti nella lista personShiftDays passata come parametro
	 */
	public void countPersonsShiftsDays(List<PersonShiftDay> personShiftDays, Table<Person, String, Integer> personShiftSumDaysForTypes) {

		// for each person and dy in the month contains worked shift (A/B)
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> shiftMonth = null;

		// for each person contains the number of days of working shift divided by shift's type 
		//Table<Person, String, Integer> shiftsSumDays = TreeBasedTable.<Person, String, Integer>create();

		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;

			// registro il turno della persona per quel giorno
			//------------------------------------------------------
			builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);				
			//Logger.debug("Registro il turno %s di %s per il giorno %d", personShiftDay.shiftType.type, person, personShiftDay.date.getDayOfMonth());			
		}
		shiftMonth = builder.build();

		// for each person and shift type counts the total shift days
		for (Person person: shiftMonth.rowKeySet()) {

			Logger.debug("conto i turni di %s", person);

			// number of competence
			int shiftNum = 0;

			for (int day: shiftMonth.columnKeySet()) {

				if (shiftMonth.contains(person, day)) {
					// get the shift type
					String shift = shiftMonth.get(person, day);
					shiftNum = (personShiftSumDaysForTypes.contains(person, shift)) ? personShiftSumDaysForTypes.get(person, shift) : 0;
					shiftNum++;
					personShiftSumDaysForTypes.put(person, shift, shiftNum);	
				}
			}

		}

		Logger.debug("la countPersonsShiftCompetences ritorna  totalPersonShiftSumDays.size() = %s", personShiftSumDaysForTypes.size());

		// return the number of saved competences
		//return shiftsSumDays;
	}
	
	
	/*
	 * Crea la tabella contenente le informazioni da stampare sul report dei turni mensile.
	 * Per ogni persona contiene:
	 * - thDays - num di giorni di turno lavorati
	 * - thLackTime - numero di ore non lavorati in turno
	 * - thReqHour - ore di turno spettanti
	 * - thAppHour - ore di turno richieste
	 * - thExceededMin - minuti in eccesso da accumulare nel mese successivo
	 * 
	 * @param totalPersonShiftWorkedTime - contiene per ogni persona, il numero di giorni lavorati e i minuti non lavorati in turno
	 * @param competenceList - lista di competenze relative ai turni di lavoro di un mese
	 * @return totalShiftInfo - tabella testuale contenete tutte le informazioni
	 */
	public Table<Person, String, String> getPersonsReportShiftInfo (Table<Person, String, Integer> totalPersonShiftWorkedTime, List<Competence> competenceList) {

		Table<Person, String, String> totalShiftInfo = TreeBasedTable.<Person, String, String>create();

		for (Competence competence: competenceList) {

			// Prende i giorni di turno lavorati e le eventuali ora mancanti 
			int numOfDays = totalPersonShiftWorkedTime.get(competence.getPerson(), Messages.get("PDFReport.thDays"));
			int lackOfMin = (totalPersonShiftWorkedTime.contains(competence.getPerson(), Messages.get("PDFReport.thLackTime"))) ? totalPersonShiftWorkedTime.get(competence.getPerson(), Messages.get("PDFReport.thLackTime")) : 0;

			// prende le ore richieste, quelle approvate e i minuti in eccesso
			// che dovranno far parte del calcolo delle ore del mese successivo
			BigDecimal reqHours = competence.getValueRequested();
			int numOfApprovedHours = competence.getValueApproved();
			int exceededMins = competence.getExceededMin();

			Logger.debug("In totalShiftInfo memorizzo (person %s) giorni=%s, ore richieste=%s, ore approvate=%s, min accumulati=%s", competence.person, numOfDays, reqHours, numOfApprovedHours, exceededMins);
			totalShiftInfo.put(competence.person, Messages.get("PDFReport.thDays"), Integer.toString(numOfDays));
			totalShiftInfo.put(competence.person, Messages.get("PDFReport.thLackTime"), competenceUtility.calcStringShiftHoursFromMinutes(lackOfMin)); 

			totalShiftInfo.put(competence.person, Messages.get("PDFReport.thReqHour"), reqHours.toString());
			totalShiftInfo.put(competence.person, Messages.get("PDFReport.thAppHour"), Integer.toString(numOfApprovedHours));

			totalShiftInfo.put(competence.person, Messages.get("PDFReport.thExceededMin"), Integer.toString(exceededMins));

		}

		return totalShiftInfo;
	}


	/*
	 * @author arianna
	 * Per ogni persona e per ogni turno, calcola il nuumerio di giorni di turno lavorati
	 * e gli eventuali minuti non lavorati che non dovranno essere retribuite
	 * 
	 * @param personsShiftsWorkedDays - 	contiene, per ogni persona e per ogni turno, il nuomero dei giorni di turno lavorati
	 * @param totalInconsistentAbsences - 	contiene, per ogni persona, le eventuali inconsistenze ed in particolare 
	 * 										la lista dei minuti non lavorati nella colonna thLackTime
	 * @return totalPersonShiftWorkedTime - contiene per ogni persona il numero totale di giorni di torno lavorati (col thDays)
	 * 										e il numero totale di minuti non lavorati che non devono essere retribuiti (col thLackTime)
	 */
	public Table<Person, String, Integer> calcShiftWorkedDaysAndLackTime(Table<Person, String, Integer> personsShiftsWorkedDays, Table<Person, String, List<String>> totalInconsistentAbsences) {

		// Contains the number of the effective hours of worked shifts 
		Table<Person, String, Integer> totalPersonShiftWorkedTime = TreeBasedTable.<Person, String, Integer>create();

		String thLackTime = Messages.get("PDFReport.thLackTime");

		// Subcract the lack of time from the Requested Hours
		for (Person person: personsShiftsWorkedDays.rowKeySet()) {
			Logger.debug("Calcolo per person=%s il thLackTime", person);

			int totalShiftDays = 0;
			int lackMin = 0;

			// Sum the shift worked days for each  shift type
			for (String shiftType: personsShiftsWorkedDays.columnKeySet()) {
				totalShiftDays += (personsShiftsWorkedDays.contains(person, shiftType)) ? personsShiftsWorkedDays.get(person, shiftType) : 0;
			}

			totalPersonShiftWorkedTime.put(person, Messages.get("PDFReport.thDays"), totalShiftDays);

			Logger.debug("Somma i minuti mancanti prendendoli da totalInconsistentAbsences(%s, %s)", person.surname, thLackTime);


			// check for lack of worked time and summarize the minutes
			if (totalInconsistentAbsences.contains(person, thLackTime)) {
				Logger.debug("non è vuoto");
				String[] timeStr;	
				for (String time: totalInconsistentAbsences.get(person, thLackTime)) {

					timeStr = time.split(".");
					Logger.debug("time = '%s' valori di timeStr = %s", time, timeStr.length);

					lackMin += Integer.parseInt(time);
				}
			}

			Logger.debug("memorizza in totalPersonsShiftsWorkedTimes(%s, thLackTime) %s", person, lackMin);
			totalPersonShiftWorkedTime.put(person, thLackTime, lackMin);
		}

		return totalPersonShiftWorkedTime;
	}


	/*
	 * Costruisce na calendario di un turno in un certo mese in una tabella del tipo (tipoTurno, giorno_del_mese) -> SD(Person, Person)
	 * dove (Person, Person) indica la persona in turno di mattina e di pomeriggio
	 * 
	 * @param firstOfMonth 	- primo giorno del mese
	 * @param shiftType		- tipo del turno
	 * @param shiftCalendar	- tabella <Turno, Giorno, SD> che viene modificata inserendo per giorno del mese la persona in turno 
	 * 						  di mattina e di pomeriggio per il turno shiftType
	 */
	public void buildMonthlyShiftCalendar(LocalDate firstOfMonth, ShiftType shiftType, Table<String, Integer, SD> shiftCalendar) {

		// legge i giorni di turno del tipo 'type' da inizio a fine mese 
		List<PersonShiftDay> personShiftDays = personShiftDayDao.getPersonShiftDayByTypeAndPeriod(firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType);

		// li inserisce nel calendario
		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;

			SD shift = null;

			int day = personShiftDay.date.getDayOfMonth();
			String currShift = personShiftDay.shiftType.type;

			if (!shiftCalendar.contains(currShift, day)) {
				shift = (personShiftDay.getShiftSlot().equals(ShiftSlot.MORNING)) ? new SD (person, null) : new SD (null, person);
				shiftCalendar.put(currShift, day, shift);
				//Logger.debug("creato shift (%s, %s) con shift.mattina=%s e shift.pomeriggio=%s", currShift, day, shift.mattina, shift.pomeriggio);
			} else {
				shift = shiftCalendar.get(currShift, day);
				if (personShiftDay.getShiftSlot().equals(ShiftSlot.MORNING)) {
					Logger.debug("Completo turno di %s con la mattina di %s", day, person);
					shift.mattina = person;
				} else {
					Logger.debug("Completo turno di %s con il pomeriggio di %s", day, person);
					shift.pomeriggio = person;
				}

				//Logger.debug("Inserito turno SD=%s di tipo %s nel giorno %s", shift, currShift, day);
				shiftCalendar.put(currShift, day, shift);
			}
		}

		//legge i turni cancellati e li registra nella tabella mensile
		Logger.debug("Cerco i turni cancellati di tipo '%s' e li inserisco nella tabella mensile", shiftType);
		List<ShiftCancelled> shiftsCancelled = shiftDao.getShiftCancelledByPeriodAndType(firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), shiftType);

		SD shift = new SD (null, null);
		for (ShiftCancelled sc: shiftsCancelled) {
			shiftCalendar.put(shiftType.type, sc.date.getDayOfMonth(), shift);
			//Logger.debug("trovato turno cancellato di tipo %s del %s", type, sc.date);
		}
	}
	
	/*
	 * @author arianna
	 * Calcola le ore di turno da approvare date quelle richieste.
	 * Poich√® le ore approvate devono essere un numero intero e quelle
	 * calcolate direttamente dai giorni di turno possono essere decimali,
	 * le ore approvate devono essere arrotondate per eccesso o per difetto a seconda dell'ultimo
	 * arrotondamento effettuato in modo che questi vengano alternati 
	 */
	public int[] calcShiftValueApproved(Person person, int year, int month, int requestedMins) {
		int hoursApproved = 0;
		int exceedMins = 0;
		int oldExceedMins = 0;


		Logger.debug("Nella calcShiftValueApproved person =%s, year=%s, month=%s, requestedMins=%s)", person, year, month, requestedMins);

		String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(requestedMins);
		int hoursOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[0]);
		int minsOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[1]);

		Logger.debug("hoursOfWorkedTime = %s minsOfWorkedTime = %s", hoursOfWorkedTime, minsOfWorkedTime);

		// get the Competence code for the ordinary shift  
		CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
		//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();

		Logger.debug("month=%s", month);
		
		/*final QCompetence com = new QCompetence("competence");
		final JPQLQuery query = getQueryFactory().query();
		final Competence myCompetence = query
				.from(com)
				.where(
						com.person.eq(person)
						.and(com.year.eq(year))
						.and(com.month.lt(month))
						.and(com.competenceCode.eq(competenceCode))		
						)
						.orderBy(com.month.desc())
						.limit(1)
						.uniqueResult(com);*/
		Competence myCompetence = competenceDao.getLastPersonCompetenceInYear(person, year, month, competenceCode);

		//Logger.debug("prendo i minuti in eccesso dal mese %s", myCompetence.getMonth());

		// get the old exceede mins in the DB
		oldExceedMins = ((myCompetence == null) || ((myCompetence != null) && myCompetence.getExceededMin() == null)) ? 0 : myCompetence.getExceededMin();

		Logger.debug("oldExceedMins in the DB=%s", oldExceedMins);


		// if there are no exceeded mins, the approved hours 
		// match with the worked hours
		if (minsOfWorkedTime == 0) {
			hoursApproved = hoursOfWorkedTime;
			exceedMins = oldExceedMins;

			//Logger.debug("minsOfWorkedTime == 0 , hoursApproved=%s exceedMins=%s", hoursApproved, exceedMins);
		} else {		
			// check if the exceeded mins of this month plus those
			// worked in the previous months make up an hour
			exceedMins = oldExceedMins + minsOfWorkedTime;
			if (exceedMins >= 60) {
				hoursApproved = hoursOfWorkedTime + 1;
				exceedMins -= 60; 
			} else {
				hoursApproved = hoursOfWorkedTime;
			}

			//Logger.debug("minsOfWorkedTime = %s , hoursApproved=%s exceedMins=%s", minsOfWorkedTime, hoursApproved, exceedMins);
		}

		Logger.debug("hoursApproved=%s exceedMins=%s", hoursApproved, exceedMins);

		int[] result = {hoursApproved, exceedMins};

		Logger.debug("La calcShiftValueApproved restituisce %s", result);

		return result;
	}
	
	/*
	 * @author arianna
	 * Crea il calendario con le reperibilita' di un determinato tipo in un dato anno completo o
	 * relativo ad una sola persona
	 * 
	 * @param year 					- anno di riferimento del calendario
	 * @param type					- tipo di turni da caricare
	 * @param personsInTheCalList	- lista vuota o contenete la persona della quae caricare i turni:
	 * 								  se è vuota carica tutto il turno
	 * @return icsCalendar			- calendario
	 */
	public Calendar createicsShiftCalendar(int year, String type,Optional<PersonShift> personShift) {
		List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
		
		Logger.debug("nella createicsReperibilityCalendar(int %s, String %s, List<PersonShift> %s)", year, type, personShift.get());
		ShiftCategories shiftCategory = shiftDao.getShiftCategoryByType(type);
		String eventLabel = "Turno ".concat(shiftCategory.description).concat(": ");

		// Create a calendar
		//---------------------------       
		Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
		icsCalendar.getProperties().add(CalScale.GREGORIAN);
		icsCalendar.getProperties().add(Version.VERSION_2_0);

		// read the person(0) shift days for the year
		//-------------------------------------------------
		LocalDate from = new LocalDate(year, 1, 1);
		LocalDate to = new LocalDate(year, 12, 31);

		ShiftType shiftType = shiftDao.getShiftTypeByType(type);
		
		// get the working shift days
		//------------------------------
		
		// if the list is empty, load the entire shift days
		if (!personShift.isPresent()) {
			personShiftDays = shiftDao.getShiftDaysByPeriodAndType(from, to, shiftType);	
			Logger.debug("Shift find called from %s to %s, type %s - found %s shift days", from, to, type, personShiftDays.size());
		}
		else {
		// load the shift days of the person in the list
			personShiftDays = shiftDao.getPersonShiftDaysByPeriodAndType(from, to, shiftType, personShift.get().person);	
			Logger.debug("Shift find called from %s to %s, type %s person %s - found %s shift days", from, to, type, personShift.get().person.surname, personShiftDays.size());
		}

		// load the shift days in the calendar
		for (PersonShiftDay psd : personShiftDays) {
	
			LocalTime startShift = (psd.shiftSlot.equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.startMorning : psd.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (psd.getShiftSlot().equals(ShiftSlot.MORNING)) ? psd.shiftType.shiftTimeTable.endMorning : psd.shiftType.shiftTimeTable.endAfternoon;
			
			Logger.debug("Turno di %s del %s dalle %s alle %s", psd.personShift.person.surname, psd.date, startShift, endShift);

			//set the start event
			java.util.Calendar start = java.util.Calendar.getInstance();
			start.set(psd.date.getYear(), psd.date.getMonthOfYear() - 1, psd.date.getDayOfMonth(), startShift.getHourOfDay(), startShift.getMinuteOfHour());
			
			//set the end event
			java.util.Calendar end = java.util.Calendar.getInstance();
			end.set(psd.date.getYear(), psd.date.getMonthOfYear() - 1, psd.date.getDayOfMonth(), endShift.getHourOfDay(), endShift.getMinuteOfHour());

			String label = eventLabel.concat(psd.personShift.person.surname);
			
			icsCalendar.getComponents().add(createDurationICalEvent(new DateTime(start.getTime()), new DateTime(end.getTime()), label));
			continue;
		}	
		
		// get the deleted shift days
		//------------------------------
		// get the deleted shifts of type shiftType
		List<ShiftCancelled> shiftsCancelled = shiftDao.getShiftCancelledByPeriodAndType(from, to, shiftType);
		Logger.debug("ShiftsCancelled find called from %s to %s, type %s - found %s shift days", from, to, shiftType.type, shiftsCancelled.size());
		
		// load the calcelled shift in the calendar
		for (ShiftCancelled shiftCancelled: shiftsCancelled) {
			Logger.debug("Trovato turno %s ANNULLATO nel giorno %s", shiftCancelled.type.type, shiftCancelled.date);
			
			// build the event day
			java.util.Calendar shift = java.util.Calendar.getInstance();
			shift.set(shiftCancelled.date.getYear(), shiftCancelled.date.getMonthOfYear() - 1, shiftCancelled.date.getDayOfMonth());
			String label = eventLabel.concat("Annullato");
			
			icsCalendar.getComponents().add(createAllDayICalEvent(new Date(shift.getTime()), label));
			continue;
		}

		return icsCalendar;
	}


	/*
	 * Create a VEvent width label 'label' that start at 'startDate' end end at 'endDate'
	 */
	private VEvent createDurationICalEvent(DateTime startDate, DateTime endDate, String eventLabel) {
		VEvent shiftDay = new VEvent(startDate, endDate, eventLabel);
		shiftDay.getProperties().add(new Uid(UUID.randomUUID().toString()));

		return shiftDay;
	}
	
	/*
	 * Creat an all day VEvent whith label 'label' for the day 'date'
	 */
	private VEvent createAllDayICalEvent(Date date, String eventLabel) {
		VEvent shiftDay = new VEvent(date, eventLabel);

		shiftDay.getProperties().add(new Uid(UUID.randomUUID().toString()));
		
		return shiftDay;
	}
	
	/*
	 * Export the shift calendar in iCal for the person with id = personId with reperibility 
	 * of type 'type' for the 'year' year
	 * If the personId=0, it exports the calendar for all persons of the shift of type 'type'
	 */
	public Optional<Calendar> createCalendar(String type, Optional<Long> personId, int year) {
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, shift type %s", year, personId, type);

		Optional<PersonShift> personShift = Optional.absent();
		if (personId.isPresent()) {
			// read the shift person 
			personShift = Optional.fromNullable(shiftDao.getPersonShiftByPersonAndType(personId.get(), type));
				return Optional.<Calendar>absent();			
		}


		Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		
		Logger.debug("chiama la createicsReperibilityCalendar(%s, %s, %s)", year, type, personShift.get());
		icsCalendar = createicsShiftCalendar(year, type, personShift); /*?*/

		Logger.debug("Find %s periodi di reperibilità.", icsCalendar.getComponents().size());
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);

		return Optional.of(icsCalendar);
	}
}
