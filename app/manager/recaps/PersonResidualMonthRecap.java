package manager.recaps;

import models.Contract;
import models.Person;

public class PersonResidualMonthRecap {

	public Person person;
	public Contract contract;
	
	public String contractDescription;
	
	public int qualifica;
	public boolean possibileUtilizzareResiduoAnnoPrecedente = true;
	public PersonResidualMonthRecap mesePrecedente;
	public int anno;
	public int mese;
	
	public int initMonteOreAnnoPassato;
	public int initMonteOreAnnoCorrente;
	
	public int initResiduoAnnoCorrenteNelMese = 0;	//per il template (se sourceContract è del mese)
	
	public int progressivoFinaleMese		 = 0;	//person day
	public int progressivoFinalePositivoMese = 0;	//person day
	public int progressivoFinaleNegativoMese = 0;	//person day
	
	public int progressivoFinalePositivoMesePrint = 0;	//per il template
	
	public int straordinariMinuti 			 = 0;	//competences
	public int straordinariMinutiS1Print	 = 0;	//per il template
	public int straordinariMinutiS2Print	 = 0;	//per il template
	public int straordinariMinutiS3Print	 = 0;	//per il template
	
	public int riposiCompensativiMinuti 	 = 0;	//absences 
	public int riposiCompensativiMinutiPrint = 0;	//per il template
	
	
	public int progressivoFinaleNegativoMeseImputatoAnnoPassato;
	public int progressivoFinaleNegativoMeseImputatoAnnoCorrente;
	public int progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese;
	
	public int riposiCompensativiMinutiImputatoAnnoPassato;
	public int riposiCompensativiMinutiImputatoAnnoCorrente;
	public int riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese;
	
	public int monteOreAnnoPassato;
	public int monteOreAnnoCorrente;
	public int numeroRiposiCompensativi;
	
	public int oreLavorate = 0;
	
	public PersonResidualMonthRecap() {}
	
	
}


