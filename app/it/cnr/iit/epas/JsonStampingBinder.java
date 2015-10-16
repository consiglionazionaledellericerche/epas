/**
 * 
 */
package it.cnr.iit.epas;

import injection.StaticInject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import models.BadgeReader;
import models.Office;
import models.Person;
import models.StampType;
import models.User;
import models.exports.StampingFromClient;

import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.Request;

import controllers.Security;
import dao.BadgeReaderDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.StampingDao;


/**
 * @author cristian
 *
 */
@Slf4j
@Global
@StaticInject
public class JsonStampingBinder implements TypeBinder<StampingFromClient> {

	@Inject
	private static BadgeReaderDao badgeReaderDao;
	@Inject
	private static StampingDao stampingDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static OfficeDao officeDao;

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], 
	 * java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	
			Class actualClass, Type genericType) throws Exception {
		
		try {
			
			Optional<User> user = Security.getUser();
			if (!user.isPresent()) {
				log.info("StampingFromClient: {}, {}, {}, {}, {}", name, 
						annotations, value, actualClass, genericType);
				
				log.info("StampingFromClient: l'user non presente");
				return null;
			}
			Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());
			Person person = null;
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
			
			Logger.debug("jsonObject = %s", jsonObject);

			StampingFromClient stamping = new StampingFromClient();
			
			if(jsonObject.has("lettore")) {
				String badgeReaderCode = jsonObject.get("lettore").getAsString();
				if (! Strings.isNullOrEmpty(badgeReaderCode) ) {
					BadgeReader badgeReader = badgeReaderDao.getBadgeReaderByCode(badgeReaderCode);
					if (badgeReader == null) {
						//Logger.warn("Lettore di badge con codice %s non presente sul database/sconosciuto", badgeReaderCode);
					}
					stamping.badgeReader = badgeReader;
				}
			}
			
			Integer inOut = jsonObject.get("operazione").getAsInt();
			if(inOut != null){
				stamping.inOut = inOut;
			}
			
			if( jsonObject.has("causale") && !jsonObject.get("causale").isJsonNull() ) {
				String causale = jsonObject.get("causale").getAsString();
				if(!Strings.isNullOrEmpty(causale)){
					StampType stampType = stampingDao.getStampTypeByCode(causale);
					if (stampType == null) {
						throw new IllegalArgumentException(String
								.format("Causale con codice %s sconosciuta.", causale));
					}
					stamping.stampType = stampType;
				}
			}
			
			if( jsonObject.has("admin") && !jsonObject.get("admin").isJsonNull() ) {
				String admin = jsonObject.get("admin").getAsString();
				if(admin.equals("true")) { 
					stamping.markedByAdmin = true;
				}
			}
								
			/**
			 * Cercare la persona in funzione del tipo di matricolaFirma.
			 * Nel campo matricolaFirma decido di riportare il valore dell'id con cui viene salvata la persona sul db invece che la 
			 * matricola
			 */
			JsonArray tipoMatricola = jsonObject.getAsJsonArray("tipoMatricolaFirma");
			String matricolaFirma = jsonObject.get("matricolaFirma").getAsString();
			
			Logger.trace("L'array json di tipoMatricola: %s", tipoMatricola);

			for (int i = 0; i < tipoMatricola.size() ; i++) {
				String tipo = tipoMatricola.get(i).getAsString();				
				Logger.trace("Il tipo di matricolaFirma che sto controllando per la matricola %s e': %s", matricolaFirma, tipo);

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
						person = personDao.getPersonByNumber(firma, Optional.fromNullable(offices));
						//person = Person.find("Select p from Person p where p.number = ?", firma).first();
					} catch (NumberFormatException nfe) {
						Logger.debug("Impossibile cercare una persona tramite la matricola se la matricola non e' numerica. Matricola = %s", matricolaFirma);
						continue;
					}
					
					if(person != null){
						stamping.personId = person.id;
						break;
					}
					continue;

				}
				
				if(tipo.equals("idTabellaINT")){
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
					person = personDao.getPersonByOldID(intMatricolaFirmaAsLong, Optional.fromNullable(offices));
					//person = Person.find("Select p from Person p where p.oldId = ?", intMatricolaFirmaAsLong).first();
					if(person != null){
						stamping.personId = person.id;
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
						stamping.personId = person.id;
						break;
					}
					
					continue;

				}
				
				if(tipo.equals("matricolaBadge")){
					
					//Rimuove tutti gli eventuali 0 iniziali alla stringa
					// http://stackoverflow.com/questions/2800739/how-to-remove-leading-zeros-from-alphanumeric-text
					String badgeNumber = matricolaFirma.replaceFirst("^0+(?!$)", "");
					
					person = personDao.getPersonByBadgeNumber(badgeNumber, Optional.fromNullable(offices));
					//person = Person.find("Select p from Person p where p.badgeNumber = ?", badgeNumber).first();
					if(person != null){
						stamping.personId = person.id;
						break;
					}
					continue;
					
				}
				
			}
	
			if(stamping.personId == null){
				log.warn("Non e' stato possibile recuperare la persona a cui si riferisce la timbratura,"
						+ " matricolaFirma={}. Controllare il database.", matricolaFirma);
				return null;
			}
						
			Integer anno = jsonObject.get("anno").getAsInt();
			Integer mese = jsonObject.get("mese").getAsInt();
			Integer giorno = jsonObject.get("giorno").getAsInt();
			Integer ora = jsonObject.get("ora").getAsInt();
			Integer minuti = jsonObject.get("minuti").getAsInt();
			if(anno != null && mese != null && giorno != null && ora != null && minuti != null){
				LocalDateTime date = new LocalDateTime(anno, mese, giorno, ora, minuti, 0);				
				stamping.dateTime = date;
			}	
			else{
				Logger.warn("Uno dei parametri relativi alla data è risultato nullo. Impossibile crearla. StampingFromClient: %s, %s, %s, %s, %s", 
						name, annotations, value, actualClass, genericType);
				return null;
			}
			
			Logger.debug("Effettuato il binding, stampingFromClient = %s", stamping.toString());
			
			return stamping;
			
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding StampingFromClient: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
			return null;
		}
	}
}