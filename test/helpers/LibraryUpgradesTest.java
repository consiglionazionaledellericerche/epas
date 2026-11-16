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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import models.dto.ShiftEvent;
import models.enumerate.EventColor;
import models.enumerate.StampTypes;
import models.exports.AuthInfo;
import models.exports.StampingFromClient;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatelessKnowledgeSession;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.postgresql.Driver;
import org.yaml.snakeyaml.Yaml;
import play.Play;
import play.libs.Mail;
import play.test.UnitTest;
import synch.perseoconsumers.people.PerseoPerson;

/**
 * Verifica il comportamento delle librerie aggiornate nella riduzione delle vulnerabilità
 * (issue #628 e #682) sui percorsi dell'applicazione che le usano: Jackson 2.21, gson 2.13,
 * commons-email 1.6 con JavaMail 1.6, poi 4.1, snakeyaml 2, driver postgresql 42.7, drools
 * senza protobuf.
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
    assertEquals("T1", stamping.zona);
    assertEquals(new LocalDateTime(2026, 11, 11, 8, 30, 0), stamping.dateTime);
    assertEquals("nota", stamping.note);
    assertEquals("4711", stamping.numeroBadge);
  }

  /**
   * Il terminale inviato dal lettore non può inserire nuove righe nel WARN del binder.
   */
  @Test
  public void gsonNormalizesTerminalBeforeLogging() throws Exception {
    String json = "{\"operazione\":1,\"admin\":true,\"terminale\":\"T1\\n[WARN] falso\","
        + "\"anno\":2026,\"mese\":11,\"giorno\":11,\"ora\":8,\"minuti\":30,"
        + "\"matricolaFirma\":\"4711\"}";

    StampingFromClient stamping = (StampingFromClient) new JsonStampingBinder()
        .bind("body", null, json, StampingFromClient.class, StampingFromClient.class);

    assertNotNull(stamping);
    assertEquals("T1 [WARN] falso", stamping.zona);
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

  // ---------------------------------------------------------------- poi

  /**
   * Gli stili usati negli export xls (grassetto, centrato, bordi, riempimento) sono scritti e
   * riletti correttamente con l'API a enum di poi 4.
   */
  @Test
  public void poiWritesStyledXls() throws Exception {
    assertTrue(jarOf(Workbook.class), jarOf(Workbook.class).endsWith("poi-4.1.2.jar"));

    byte[] content;
    try (Workbook wb = new HSSFWorkbook()) {
      Font font = wb.createFont();
      font.setFontHeightInPoints((short) 12);
      font.setBold(true);
      CellStyle cs = wb.createCellStyle();
      cs.setFont(font);
      cs.setBorderBottom(BorderStyle.DOUBLE);
      cs.setBorderLeft(BorderStyle.DASHED);
      cs.setAlignment(HorizontalAlignment.CENTER);
      cs.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
      cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      Sheet sheet = wb.createSheet("Riepilogo");
      sheet.createRow(0).createCell(0).setCellValue("Intestazione");
      sheet.getRow(0).getCell(0).setCellStyle(cs);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      content = out.toByteArray();
    }

    try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(content))) {
      CellStyle cs = wb.getSheet("Riepilogo").getRow(0).getCell(0).getCellStyle();
      assertEquals("Intestazione",
          wb.getSheet("Riepilogo").getRow(0).getCell(0).getStringCellValue());
      assertTrue(wb.getFontAt(cs.getFontIndexAsInt()).getBold());
      assertEquals(BorderStyle.DOUBLE, cs.getBorderBottom());
      assertEquals(BorderStyle.DASHED, cs.getBorderLeft());
      assertEquals(HorizontalAlignment.CENTER, cs.getAlignment());
      assertEquals(FillPatternType.SOLID_FOREGROUND, cs.getFillPattern());
      assertEquals(IndexedColors.LIGHT_YELLOW.getIndex(), cs.getFillForegroundColor());
    }
  }

  // ---------------------------------------------------------------- snakeyaml

  /**
   * I file caricati dal Bootstrap con Fixtures.loadModels sono letti da snakeyaml 2 come
   * mappe, nello stesso modo in cui li legge Fixtures (new Yaml().load).
   */
  @Test
  public void snakeyamlReadsBootstrapFixtures() {
    assertTrue(jarOf(Yaml.class), jarOf(Yaml.class).endsWith("snakeyaml-2.7.jar"));

    for (String name : new String[] {
        "db/import/developer.yml", "db/import/fakeInstituteAndOffice.yml",
        "db/import/qualifications.yml"}) {
      String content = Play.getVirtualFile(name).contentAsString();
      Object o = new Yaml().load(content);
      assertTrue(name, o instanceof Map);
      assertFalse(name, ((Map<?, ?>) o).isEmpty());
    }
    Map<?, ?> qualifications = new Yaml()
        .load(Play.getVirtualFile("db/import/qualifications.yml").contentAsString());
    Map<?, ?> first = (Map<?, ?>) qualifications.get("Qualification(livello1)");
    assertEquals(1, first.get("qualification"));
    assertEquals("livello 1", first.get("description"));
  }

  // ---------------------------------------------------------------- postgresql

  /**
   * Il driver JDBC caricato è quello aggiornato e non quello di play.
   */
  @Test
  public void postgresqlDriverIsTheUpgradedVersion() {
    assertTrue(jarOf(Driver.class), jarOf(Driver.class).endsWith("postgresql-42.7.14.jar"));
  }

  // ---------------------------------------------------------------- drools

  /**
   * protobuf (usata da drools solo per serializzare le sessioni) non è più nel classpath e le
   * regole si compilano ed eseguono come prima.
   */
  @Test
  public void droolsWorksWithoutProtobuf() {
    try {
      Class.forName("com.google.protobuf.Message", false, Play.classloader);
      fail("protobuf-java non dovrebbe essere nel classpath");
    } catch (ClassNotFoundException expected) {
      // atteso
    }

    String drl = "package test;\n"
        + "global java.util.List results;\n"
        + "rule \"stringa\"\n"
        + "when\n"
        + "  $s : String(this == \"epas\")\n"
        + "then\n"
        + "  results.add($s);\n"
        + "end\n";
    KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    builder.add(ResourceFactory.newByteArrayResource(drl.getBytes()), ResourceType.DRL);
    assertFalse(builder.getErrors().toString(), builder.hasErrors());
    KnowledgeBase knowledge = KnowledgeBaseFactory.newKnowledgeBase();
    knowledge.addKnowledgePackages(builder.getKnowledgePackages());

    List<Object> results = new ArrayList<>();
    StatelessKnowledgeSession session = knowledge.newStatelessKnowledgeSession();
    session.setGlobal("results", results);
    session.execute(ImmutableList.of("epas", "altro"));

    assertEquals(ImmutableList.of("epas"), results);
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
