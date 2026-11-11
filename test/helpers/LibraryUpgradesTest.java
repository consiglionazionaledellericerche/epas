/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package helpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import common.oauth2.ProviderConfig;
import helpers.rest.GsonModule;
import helpers.rest.JacksonModule;
import it.cnr.iit.epas.AuthInfoBinder;
import it.cnr.iit.epas.JsonStampingBinder;
import it.cnr.iit.keycloak.model.UserRepresentation;
import java.util.List;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import models.dto.ShiftEvent;
import models.enumerate.EventColor;
import models.enumerate.StampTypes;
import models.exports.AuthInfo;
import models.exports.StampingFromClient;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import play.libs.Mail;
import play.test.UnitTest;
import synch.perseoconsumers.people.PerseoPerson;

/**
 * Verifica il comportamento delle librerie aggiornate nella riduzione delle vulnerabilità
 * (issue #628) sui percorsi dell'applicazione che le usano: Jackson 2.21, gson 2.13,
 * commons-email 1.6 con JavaMail 1.6.
 */
public class LibraryUpgradesTest extends UnitTest {

  private static String jarOf(Class<?> clazz) {
    return clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
  }

  // ---------------------------------------------------------------- Jackson

  /**
   * Gli eventi del calendario (DTO Lombok @Builder con date joda) sono serializzati con il
   * mapper dell'applicazione come prima: date ISO, campi null omessi, campi @JsonIgnore esclusi.
   */
  @Test
  public void jacksonSerializesCalendarEvents() throws Exception {
    ObjectMapper mapper = new JacksonModule().getObjectMapper();
    ShiftEvent event = ShiftEvent.builder()
        .title("Turno")
        .start(new LocalDateTime(2026, 11, 11, 8, 0))
        .end(new LocalDateTime(2026, 11, 11, 14, 0))
        .personId(42L)
        .eventColor(EventColor.values()[0])
        .troubles(ImmutableList.of("problema"))
        .build();

    JsonNode json = mapper.readTree(mapper.writeValueAsString(ImmutableList.of(event)));

    JsonNode node = json.get(0);
    assertEquals("Turno", node.get("title").asText());
    assertEquals("2026-11-11T08:00:00.000", node.get("start").asText());
    assertEquals("2026-11-11T14:00:00.000", node.get("end").asText());
    assertEquals(42L, node.get("personId").asLong());
    assertEquals(EventColor.values()[0].name(), node.get("eventColor").asText());
    assertEquals("problema", node.get("troubles").get(0).asText());
    assertFalse(node.has("url"));
    assertFalse(node.has("organizationShiftslot"));
  }

  /**
   * La configurazione del provider OpenID (snake_case, proprietà sconosciute) è letta come
   * prima.
   */
  @Test
  public void jacksonReadsOpenIdProviderConfig() throws Exception {
    ObjectMapper mapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    String json = "{\"issuer\":\"https://idp.example.org/realms/epas\","
        + "\"authorization_endpoint\":\"https://idp.example.org/auth\","
        + "\"token_endpoint\":\"https://idp.example.org/token\","
        + "\"jwks_uri\":\"https://idp.example.org/certs\","
        + "\"scopes_supported\":[\"openid\",\"email\"],"
        + "\"proprieta_sconosciuta\":{\"annidata\":true}}";

    ProviderConfig config = mapper.readValue(json, ProviderConfig.class);

    assertEquals("https://idp.example.org/realms/epas", config.getIssuer());
    assertEquals("https://idp.example.org/auth", config.getAuthorizationEndpoint());
    assertEquals("https://idp.example.org/token", config.getTokenEndpoint());
    assertEquals("https://idp.example.org/certs", config.getJwksUri());
    assertEquals(ImmutableList.of("openid", "email"), config.getScopesSupported());
  }

  /**
   * I modelli del client keycloak (generati da openapi, compilati con Jackson 2.12) sono letti
   * e scritti dalla nuova versione di Jackson. Non si usa l'ObjectMapper di ApiClient perche'
   * richiede jackson-datatype-threetenbp e jackson-databind-nullable, che keycloak-client non
   * dichiara e non sono nel classpath (gia' prima dell'aggiornamento).
   */
  @Test
  public void jacksonReadsKeycloakModels() throws Exception {
    ObjectMapper mapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    String json = "[{\"id\":\"abc\",\"username\":\"mario.rossi\",\"firstName\":\"Mario\","
        + "\"lastName\":\"Rossi\",\"email\":\"mario.rossi@example.org\",\"enabled\":true,"
        + "\"createdTimestamp\":1731312000000,\"attributes\":{\"matricola\":[\"1234\"]},"
        + "\"nuovoCampo\":1}]";

    List<UserRepresentation> users = mapper.readValue(json,
        mapper.getTypeFactory().constructCollectionType(List.class, UserRepresentation.class));

    assertEquals(1, users.size());
    UserRepresentation user = users.get(0);
    assertEquals("mario.rossi", user.getUsername());
    assertEquals("Rossi", user.getLastName());
    assertTrue(user.getEnabled());
    assertEquals(Long.valueOf(1731312000000L), user.getCreatedTimestamp());
    assertEquals(ImmutableList.of("1234"), user.getAttributes().get("matricola"));

    String written = mapper.writeValueAsString(user);
    assertTrue(written.contains("\"username\":\"mario.rossi\""));
  }

  // ---------------------------------------------------------------- gson

  /**
   * Il binder delle timbrature dai lettori (JsonParser deprecato di gson) funziona come prima.
   */
  @Test
  public void gsonBindsStampingFromClient() throws Exception {
    String json = "{\"operazione\":1,\"causale\":\"motiviDiServizio\",\"admin\":\"true\","
        + "\"terminale\":\"T1\","
        + "\"anno\":2026,\"mese\":11,\"giorno\":11,\"ora\":8,\"minuti\":30,"
        + "\"note\":\"nota\",\"matricolaFirma\":\"4711\"}";

    StampingFromClient stamping = (StampingFromClient) new JsonStampingBinder()
        .bind("body", null, json, StampingFromClient.class, StampingFromClient.class);

    assertNotNull(stamping);
    assertEquals(Integer.valueOf(1), stamping.inOut);
    assertEquals(StampTypes.MOTIVI_DI_SERVIZIO, stamping.stampType);
    assertTrue(stamping.markedByAdmin);
    assertEquals("T1", stamping.zona);
    assertEquals(new LocalDateTime(2026, 11, 11, 8, 30, 0), stamping.dateTime);
    assertEquals("nota", stamping.note);
    assertEquals("4711", stamping.numeroBadge);
  }

  /**
   * Il binder delle credenziali JSON funziona come prima.
   */
  @Test
  public void gsonBindsAuthInfo() throws Exception {
    AuthInfo info = (AuthInfo) new AuthInfoBinder().bind("body", null,
        "{\"username\":\"lettore\",\"password\":\"segreta\"}", AuthInfo.class, AuthInfo.class);

    assertEquals("lettore", info.getUsername());
    assertEquals("segreta", info.getPassword());
  }

  /**
   * Il GsonBuilder dell'applicazione (convertitori joda e java.time) serializza e deserializza
   * le date come prima.
   */
  @Test
  public void gsonBuilderHandlesJodaAndJavaTime() {
    Gson gson = new GsonModule().builderFactory().create();

    JsonObject json = gson.toJsonTree(new Dates()).getAsJsonObject();
    assertEquals("2026-11-11", json.get("jodaDate").getAsString());
    assertEquals("2026-11-11T08:30:00.000", json.get("jodaDateTime").getAsString());
    assertEquals("2026-11-11", json.get("javaDate").getAsString());
    assertTrue(json.has("empty"));
    assertTrue(json.get("empty").isJsonNull());

    Dates read = gson.fromJson(json, Dates.class);
    assertEquals(new LocalDate(2026, 11, 11), read.jodaDate);
    assertEquals(new LocalDateTime(2026, 11, 11, 8, 30), read.jodaDateTime);
    assertEquals(java.time.LocalDate.of(2026, 11, 11), read.javaDate);
  }

  /**
   * I consumer di Perseo deserializzano liste usando il TypeToken di guava.
   */
  @Test
  public void gsonReadsListsWithGuavaTypeToken() {
    String json = "[{\"id\":1,\"firstname\":\"Mario\",\"surname\":\"Rossi\",\"number\":\"1234\","
        + "\"qualification\":4,\"departmentId\":10,\"campoNuovo\":\"x\"}]";

    List<PerseoPerson> people = new Gson().fromJson(json, new TypeToken<List<PerseoPerson>>() {
      private static final long serialVersionUID = 1L;
    }.getType());

    assertEquals(1, people.size());
    assertEquals("Rossi", people.get(0).surname);
    assertEquals(Integer.valueOf(4), people.get(0).qualification);
    assertEquals(Long.valueOf(10), people.get(0).departmentId);
  }

  /**
   * renderJSON di play usa gson: verifica che la versione caricata sia quella aggiornata.
   */
  @Test
  public void gsonIsTheUpgradedVersion() {
    assertTrue(jarOf(Gson.class), jarOf(Gson.class).endsWith("gson-2.13.2.jar"));
  }

  // ---------------------------------------------------------------- mail

  /**
   * commons-email 1.6 gira su JavaMail 1.6 (non sulla mail-1.4.7 di play) e costruisce i
   * messaggi.
   */
  @Test
  public void commonsEmailBuildsMessagesWithJavaMail16() throws Exception {
    assertTrue(jarOf(Session.class), jarOf(Session.class).endsWith("javax.mail-1.6.2.jar"));
    assertTrue(jarOf(SimpleEmail.class), jarOf(SimpleEmail.class)
        .endsWith("commons-email-1.6.0.jar"));

    HtmlEmail email = new HtmlEmail();
    email.setHostName("localhost");
    email.setCharset("UTF-8");
    email.setFrom("epas@example.org", "ePAS");
    email.addTo("mario.rossi@example.org");
    email.setSubject("Prova àèìòù");
    email.setHtmlMsg("<p>Messaggio</p>");
    email.setTextMsg("Messaggio");
    email.buildMimeMessage();

    MimeMessage message = email.getMimeMessage();
    assertEquals("Prova àèìòù", message.getSubject());
    assertEquals("mario.rossi@example.org", message.getAllRecipients()[0].toString());
  }

  /**
   * L'invio tramite play.libs.Mail (mailer mock in test) funziona con commons-email 1.6.
   */
  @Test
  public void playMailSendsWithCommonsEmail16() throws Exception {
    SimpleEmail email = new SimpleEmail();
    email.setFrom("epas@example.org");
    email.addTo("destinatario.upgrade@example.org");
    email.setSubject("Prova invio");
    email.setMsg("Corpo del messaggio");

    assertTrue(Mail.send(email).get());

    String sent = play.libs.Mail.Mock.getLastMessageReceivedBy("destinatario.upgrade@example.org");
    assertNotNull(sent);
    assertTrue(sent, sent.contains("Prova invio"));
    assertTrue(sent, sent.contains("Corpo del messaggio"));
  }

  /**
   * Classe di appoggio per la serializzazione delle date.
   */
  static class Dates {
    LocalDate jodaDate = new LocalDate(2026, 11, 11);
    LocalDateTime jodaDateTime = new LocalDateTime(2026, 11, 11, 8, 30);
    java.time.LocalDate javaDate = java.time.LocalDate.of(2026, 11, 11);
    String empty = null;
  }
}
