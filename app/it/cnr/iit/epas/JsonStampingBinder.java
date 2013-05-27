/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.BadgeReader;
import models.Person;
import models.PersonReperibilityType;
import models.StampType;
import models.enumerate.StampTypeValues;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;
import models.exports.StampingFromClient;
import models.exports.StampingFromClient.TipoMatricolaFirma;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.Play;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * @author cristian
 *
 */
@Global
public class JsonStampingBinder implements TypeBinder<StampingFromClient> {

	public static String mySqldriver = Play.configuration.getProperty("db.old.driver");
	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.debug("binding StampingFromClient: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		try {
			
			Person person = null;
			JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
			
			Logger.debug("jsonObject = %s", jsonObject);

			StampingFromClient stamping = new StampingFromClient();
			
			String badgeReaderCode = jsonObject.get("lettore").getAsString();
			if (!badgeReaderCode.isEmpty()) {
				BadgeReader badgeReader = BadgeReader.find("byCode", badgeReaderCode).first();
				if (badgeReader == null) {
					//throw new IllegalArgumentException(
					//	String.format("Lettore con codice %s sconosciuto. Abilitare in configurazione", badgeReaderCode));
					Logger.info("Lettore di badge con codice %s non presente sul database/sconosciuto", badgeReaderCode);
				}
				stamping.badgeReader = badgeReader;
			}
			
			Integer inOut = jsonObject.get("operazione").getAsInt();
			if(inOut != null){
				stamping.inOut = inOut;
			}
			
			
			JsonElement jsel = jsonObject.get("causale");
			if(!jsel.isJsonNull()){
				String stampTypeCode = jsel.getAsString();
				if (!stampTypeCode.isEmpty()) {
					StampType stampType = StampType.find("byCode", stampTypeCode).first();
					if (stampType == null) {
						throw new IllegalArgumentException(
							String.format("Causale con codice %s sconosciuta.", stampTypeCode));
					}
					stamping.stampType = stampType;
				}
			}
								
			/**
			 * Cercare la persona in funzione del tipo di matricolaFirma.
			 * Nel campo matricolaFirma decido di riportare il valore dell'id con cui viene salvata la persona sul db invece che la 
			 * matricola
			 */
			String matricolaFirma = jsonObject.get("matricolaFirma").getAsString();
			Logger.debug("La matricola firma è del tipo: %s", matricolaFirma);
			if(matricolaFirma.contains("INT")){
				Logger.debug("Sono entrato nel controllo if delle matricole di tipo 000000INT123");
				/**
				 * in questo caso dal client arriva la timbratura con la firma specificata secondo lo schema INT123.
				 * Si fa quindi una substring sulla matricolafirma e ciò che si ottiene è l'id di tipo long per fare la ricerca sulla
				 * tabella persone per capire a chi è relativa quella timbratura...
				 * 
				 * ho aggiunto il campo oldId alla tabella Person e con quello farò la query per recuperare la persona 
				 */
				String lessSign = matricolaFirma.substring(14,matricolaFirma.length());
				Logger.debug("L'id recuperato è: %s", lessSign);
				long personId = Long.parseLong(lessSign);
				
				//person = Person.findById(personId);
				//if(person == null){
				person = Person.find("Select p from Person p where p.oldId = ?", personId).first();
				Logger.debug("La persona è: %s %s", person.name, person.surname);
					
				//}
				
				stamping.matricolaFirma = person.oldId;
			}
			else{
				/**
				 * si cerca se quel che è stato passato può essere il campo "number" (matricola) della tabella Person
				 */
				int firma = Integer.parseInt(matricolaFirma);
				person = Person.find("Select p from Person p where p.number = ?", firma).first();
				if(person == null){
					/**
					 * la persona ritornata è null, quindi si cerca sempre su tabella Person però restringendo sul campo badgeNumber
					 * (matricolaBadge)
					 */
					person = Person.find("Select p from Person p where p.badgeNumber = ?", matricolaFirma).first();
					
				}
				stamping.matricolaFirma = person.id;
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
				throw new IllegalArgumentException("Uno dei parametri relativi alla data è risultato nullo. Impossibile crearla.");
			}
			
			Logger.debug("Effettuato il binding, stampingFromClient = %s", stamping);
			
			return stamping;
			
			
		} catch (Exception e) {
			Logger.error(e, "Problem during binding StampingFromClient.");
			throw e;
		}
	}
	
//	public static void main(String[]args){
//		String s = "00000000000INT252";
//		if(s.contains("INT")){
//			System.out.println("TRovato!");
//			String lessSign = s.substring(14,s.length());
//			long personId = Long.parseLong(lessSign);
//			System.out.println("L'id è: "+personId);
//		}
//		
//	}
	
}