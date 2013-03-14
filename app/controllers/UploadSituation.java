package controllers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import models.Absence;
import models.Competence;
import models.Configuration;
import models.Person;
import models.PersonMonth;

import play.Logger;
import play.mvc.Controller;

public class UploadSituation extends Controller{
	
	
	public static void show(Integer month, Integer year){
		render();
	}

	@Check(Security.UPLOAD_SITUATION)
	public static void uploadSituation(Integer year, Integer month) throws IOException{
		if(month == null || year == null){
			flash.error("Il valore dei parametri su cui fare il caricamento dei dati non può essere nullo");
			Application.indexAdmin();
		}
		Logger.debug("Anno: %s", year);
		Logger.debug("Mese: %s", month);
		Configuration config = Configuration.getCurrentConfiguration();
		List<Person> personList = Person.find("Select p from Person p where p.number is not null order by p.number").fetch();
		List<Absence> absenceList = null;
		List<Competence> competenceList = null;
		File uploadSituation = new File("/home/dario/git/epas/caricaSituazioneDipendenti"+year.toString()+month.toString()+".txt");
		Logger.debug("Creato nuovo file per caricare informazioni mensili sul personale in %s", uploadSituation.getAbsolutePath());
		FileWriter writer = new FileWriter(uploadSituation, true);
		try {
			BufferedWriter out = new BufferedWriter(writer);
			out.write(config.seatCode.toString());
			out.write(' ');
			out.write(new String(month.toString()+year.toString()));
			out.newLine();
			for(Person p : personList){
				out.append(p.number.toString());
				out.append(' ');
				PersonMonth pm = new PersonMonth(p, year, month);
				absenceList = pm.getAbsenceInMonthForUploadSituation();
				if(absenceList != null){
					for(Absence abs : absenceList){
						out.append('A');
						out.append(' ');
						out.append(abs.absenceType.code);
						out.append(' ');
						out.append(new Integer(abs.personDay.date.getDayOfMonth()).toString());
						out.append(' ');
						out.append(new Integer(abs.personDay.date.getDayOfMonth()).toString());
						out.append(' ');
						out.append('0');
						out.newLine();
					}
				}
				competenceList = pm.getCompetenceInMonthForUploadSituation();
				if(competenceList != null){
					for(Competence comp : competenceList){
						out.append(p.number.toString());
						out.append(' ');
						out.append('C');
						out.append(' ');
						out.append(comp.competenceCode.code);
						out.append(' ');
						out.append(new Integer(comp.valueApproved).toString());
						out.append(' ');
						out.append('0');
						out.append(' ');
						out.append('0');
						out.newLine();
					}
				}
			}
			
			out.close();
			flash.success("Il file contenente le informazioni da caricare su attestati di presenza è stato creato correttamente e si trova in: %s", 
					uploadSituation.getAbsolutePath());
			Application.indexAdmin();
		} catch (IOException e) {
			
			e.printStackTrace();
			flash.error("Il file non è stato creato correttamente, accedere al file di log.");
			Application.indexAdmin();
		}
	}
}
