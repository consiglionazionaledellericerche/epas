/**
 * 
 */
package it.cnr.iit.epas;

import injection.StaticInject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;

import models.BadgeReader;
import models.Person;
import models.StampType;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.BadgeReaderDao;
import dao.PersonDao;
import dao.StampingDao;


/**
 * @author cristian
 *
 */
@Global
@StaticInject
public class JsonAbsenceBinder implements TypeBinder<AbsenceFromClient> {

	@Inject
	private static PersonDao personDao;

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], 
	 * java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	
			Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding AbsenceFromClient: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			
			Person person = null;
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
			
			Logger.debug("jsonObject = %s", jsonObject);

			AbsenceFromClient absence = new AbsenceFromClient();
			
			/**
			 * Cercare la persona in funzione del tipo di matricolaFirma.
			 * Nel campo matricolaFirma decido di riportare il valore dell'id 
			 * con cui viene salvata la persona sul db invece che la matricola
			 */
			JsonArray tipoMatricola = jsonObject.getAsJsonArray("tipoMatricolaFirma");
			String matricolaFirma = jsonObject.get("matricolaFirma").getAsString();
			
			Logger.trace("L'array json di tipoMatricola: %s", tipoMatricola);

			for (int i = 0; i < tipoMatricola.size() ; i++) {
				String tipo = tipoMatricola.get(i).getAsString();				
				Logger.trace("Il tipo di matricolaFirma che sto controllando "
						+ "per la matricola %s e': %s", matricolaFirma, tipo);

				/**
				 * l'ordine con cui faccio le ricerche sul db dipende dall'array tipoMatricola che mi ha passato il client, 
				 * quindi vado sul db a fare la ricerca partendo dal primo campo dell'array passato. 
				 * Se lo trovo ok ed esco, altrimenti proseguo nel for a cercare con il tipo successivo
				 */				
				
				if(tipo.equals("matricolaCNR")){
			
					if (matricolaFirma.indexOf("INT") > 0) {
						continue;
					}
					try {
						int firma = Integer.parseInt(matricolaFirma);
						person = personDao.getPersonByNumber(firma);
						//person = Person.find("Select p from Person p where p.number = ?", firma).first();
					} catch (NumberFormatException nfe) {
						Logger.debug("Impossibile cercare una persona tramite la matricola se la matricola non e' numerica. Matricola = %s", matricolaFirma);
						continue;
					}
					
					if(person != null){
						absence.personId = person.id;
						break;
					}
					continue;

				}
				
				if (tipo.equals("idTabellaINT")) {
					Logger.debug("Controllo l'idtabellaINT");
					//Matricola firma derivante dal contatore interno
					String intMatricolaFirma = matricolaFirma;
					
					if (matricolaFirma.indexOf("INT") > 0) {
						intMatricolaFirma = matricolaFirma.substring(matricolaFirma.indexOf("INT") + 3).trim();
					} else {
						continue;
					}
					Logger.debug("La matricola firma è: %s", intMatricolaFirma);
					
					long intMatricolaFirmaAsLong = Long.parseLong(intMatricolaFirma);
					
										
					//Controlla sul campo person oldId
					person = personDao.getPersonByOldID(intMatricolaFirmaAsLong);
					//person = Person.find("Select p from Person p where p.oldId = ?", intMatricolaFirmaAsLong).first();
					if(person != null){
						absence.personId = person.id;
						break;
					}	
					
					//Nell'inserimento delle persone ci deve essere un controllo che verifichi che non ci
					//siano casi in cui il campo id possa essere utilizzato per associare il badge alla persona
					//e lo stesso valore dell'id esista già come oldId, altrimenti questa parte di codice non
					//funzionerebbe
					
					person = personDao.getPersonById(intMatricolaFirmaAsLong);
					//person = Person.find("Select p from Person p where p.id = ?", intMatricolaFirmaAsLong).first();
					if(person != null){
						Logger.debug("La persona corrispondente è: %s %s", person.name, person.surname);
						absence.personId = person.id;
						break;
					}
					
					continue;

				}
				
				if(tipo.equals("matricolaBadge")){
					
					//Rimuove tutti gli eventuali 0 iniziali alla stringa
					// http://stackoverflow.com/questions/2800739/how-to-remove-leading-zeros-from-alphanumeric-text
					String badgeNumber = matricolaFirma.replaceFirst("^0+(?!$)", "");
					
					person = personDao.getPersonByBadgeNumber(badgeNumber);
					//person = Person.find("Select p from Person p where p.badgeNumber = ?", badgeNumber).first();
					if(person != null){
						absence.personId = person.id;
						break;
					}
					continue;
					
				}
				
			}
	
			if(absence.personId == null){
				Logger.warn("Non è stato possibile recuperare l'id della persona a "
						+ "cui si riferisce la timbratura. Controllare il database");
				return null;
			}
			
			absence.code = jsonObject.get("code").getAsString();
			absence.date = new LocalDate(jsonObject.get("date").getAsString());
						
			return absence;
			
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding StampingFromClient: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
			return null;
		}
	}
}