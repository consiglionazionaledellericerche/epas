/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cnr.sync.deserializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import models.Contract;
import models.Person;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PersonDeserializer implements JsonDeserializer<Person> {

  // Pattern per il parsing delle date
  static final DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd");

  @Override
  public Person deserialize(JsonElement json, Type arg1,
                            JsonDeserializationContext arg2) throws JsonParseException {

    JsonObject jsonPerson = json.getAsJsonObject();

    Person person = new Person();

    if (jsonPerson.has("otherNames")) {
      person.name = jsonPerson.get("firstname").getAsString() + " "
          + jsonPerson.get("otherNames").getAsString();
    } else {
      person.name = jsonPerson.get("firstname").getAsString();
    }

    if (jsonPerson.has("otherSurnames")) {
      person.surname = jsonPerson.get("surname").getAsString() + " "
          + jsonPerson.get("otherSurnames").getAsString();
    } else {
      person.surname = jsonPerson.get("surname").getAsString();
    }

    person.birthday = LocalDate.parse(jsonPerson.get("birthDate").getAsString(), dtf);

    person.email = jsonPerson.get("email").getAsString();
    person.number = jsonPerson.get("number").getAsString();

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
