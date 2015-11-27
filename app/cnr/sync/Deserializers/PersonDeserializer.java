package cnr.sync.Deserializers;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import models.Contract;
import models.Person;

public class PersonDeserializer implements JsonDeserializer<Person> {

  //	Pattern per il parsing delle date
  final static DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd");

  @Override
  public Person deserialize(JsonElement json, Type arg1,
                            JsonDeserializationContext arg2) throws JsonParseException {

    JsonObject jPerson = json.getAsJsonObject();

    Person person = new Person();

    person.name = jPerson.has("otherNames") ?
            jPerson.get("firstname").getAsString() + " " + jPerson.get("otherNames").getAsString()
            : jPerson.get("firstname").getAsString();

    person.surname = jPerson.has("otherSurnames") ?
            jPerson.get("surname").getAsString() + " " + jPerson.get("otherSurnames").getAsString()
            : jPerson.get("surname").getAsString();

    person.birthday = LocalDate.parse(jPerson.get("birthDate").getAsString(), dtf);

    person.email = jPerson.get("email").getAsString();
//		person.cnr_email=jPerson.get("emailCnr").getAsString();
    person.number = jPerson.get("number").getAsInt();

    JsonArray badges = jPerson.get("badges").getAsJsonArray();

    if (badges.iterator().hasNext()) {
      JsonObject badge = badges.iterator().next().getAsJsonObject();
//			person.badgeNumber = badge.get("number").getAsString();
    }

    JsonArray contacts = jPerson.get("contacts").getAsJsonArray();

    if (contacts.iterator().hasNext()) {
      JsonObject contact = contacts.iterator().next().getAsJsonObject();
      person.telephone = contact.get("telephone").getAsString();
      person.fax = contact.get("fax").getAsString();
      person.mobile = contact.get("mobile").getAsString();
    }

    JsonArray contracts = jPerson.get("contracts").getAsJsonArray();

    if (contracts.iterator().hasNext()) {
      for (JsonElement je : contracts) {
        JsonObject jcontract = (JsonObject) je;
        Contract contract = new Contract();
        contract.beginContract = LocalDate.parse(jcontract.get("beginContract").getAsString(), dtf);
      }
      JsonObject contact = contacts.iterator().next().getAsJsonObject();
      person.telephone = contact.get("telephone").getAsString();
      person.fax = contact.get("fax").getAsString();
      person.mobile = contact.get("mobile").getAsString();
    }


    return person;
  }
}
