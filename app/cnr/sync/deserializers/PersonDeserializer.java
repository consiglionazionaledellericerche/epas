package cnr.sync.deserializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import models.Contract;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.reflect.Type;

public class PersonDeserializer implements JsonDeserializer<Person> {

  // Pattern per il parsing delle date
  static final DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd");

  @Override
  public Person deserialize(JsonElement json, Type arg1,
                            JsonDeserializationContext arg2) throws JsonParseException {

    JsonObject jsonPerson = json.getAsJsonObject();

    Person person = new Person();

    person.name = jsonPerson.has("otherNames")
            ? jsonPerson.get("firstname").getAsString() + " "
              + jsonPerson.get("otherNames").getAsString()
            : jsonPerson.get("firstname").getAsString();

    person.surname = jsonPerson.has("otherSurnames")
            ? jsonPerson.get("surname").getAsString() + " "
              + jsonPerson.get("otherSurnames").getAsString()
            : jsonPerson.get("surname").getAsString();

    person.birthday = LocalDate.parse(jsonPerson.get("birthDate").getAsString(), dtf);

    person.email = jsonPerson.get("email").getAsString();
    person.number = jsonPerson.get("number").getAsInt();

    JsonArray badges = jsonPerson.get("badges").getAsJsonArray();

    if (badges.iterator().hasNext()) {
      JsonObject badge = badges.iterator().next().getAsJsonObject();
    }

    JsonArray contacts = jsonPerson.get("contacts").getAsJsonArray();

    if (contacts.iterator().hasNext()) {
      JsonObject contact = contacts.iterator().next().getAsJsonObject();
      person.telephone = contact.get("telephone").getAsString();
      person.fax = contact.get("fax").getAsString();
      person.mobile = contact.get("mobile").getAsString();
    }

    JsonArray contracts = jsonPerson.get("contracts").getAsJsonArray();

    if (contracts.iterator().hasNext()) {
      for (JsonElement je : contracts) {
        JsonObject jcontract = (JsonObject) je;
        Contract contract = new Contract();
        contract.beginDate = LocalDate.parse(jcontract.get("beginContract").getAsString(), dtf);
      }
      JsonObject contact = contacts.iterator().next().getAsJsonObject();
      person.telephone = contact.get("telephone").getAsString();
      person.fax = contact.get("fax").getAsString();
      person.mobile = contact.get("mobile").getAsString();
    }


    return person;
  }
}
