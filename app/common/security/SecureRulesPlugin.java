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

package common.security;

import java.util.Properties;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.vfs.VirtualFile;

/**
 * Plugin per il caricamento dei componenti necessari alle Drools.
 *
 * @author Marco Andreini
 */
public class SecureRulesPlugin extends PlayPlugin {

  private static final Logger log = LoggerFactory.getLogger(SecureRulesPlugin.class);
  private static final String FILENAME = "permissions.drl";

  static KnowledgeBase knowledgeBase;
  Long lastModified;

  @Override
  public void onApplicationStart() {
    loadRulesIfNecessary();
  }

  @Override
  public void detectChange() {
    loadRulesIfNecessary();
  }

  private void loadRulesIfNecessary() {
    final VirtualFile rulesFile = Play.getVirtualFile("conf").child(FILENAME);
    if (!rulesFile.exists()) {
      log.warn("file not found {}", rulesFile.relativePath());
    } else {
      if (Play.mode == Mode.PROD) {
        loadRules(rulesFile.content());
      } else {
        if (!rulesFile.lastModified().equals(lastModified)) {
          // Hashing.md5().hashBytes(rules);
          log.info("(re)loading drools ({} -> {})", lastModified,
                  rulesFile.lastModified());
          lastModified = rulesFile.lastModified();
          loadRules(rulesFile.content());
        }
      }
    }
  }

  private void loadRules(byte[] rulesContent) {

    // Configure the drools compiler to use Janino, instead of JDT, with the
    // Play classloader, so that compilation will load model classes from
    // the classloader, and not as .class file resources.
    // https://jira.jboss.org/browse/JBRULES-1229
    Properties properties = new Properties();
    properties.put("drools.dialect.java.compiler", "JANINO");
    Thread.currentThread().setContextClassLoader(Play.classloader);
    final KnowledgeBuilderConfiguration configuration = KnowledgeBuilderFactory
            .newKnowledgeBuilderConfiguration(properties, Play.classloader);
    final KnowledgeBuilder builder = KnowledgeBuilderFactory
            .newKnowledgeBuilder(configuration);

    // Compile the rules file.
    builder.add(ResourceFactory.newByteArrayResource(rulesContent), ResourceType.DRL);
    if (builder.hasErrors()) {
      log.error(builder.getErrors().toString());
      throw new RuntimeException("Drools compilation failed: "
              + builder.getErrors().size() + " errors");
    }

    knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
    knowledgeBase.addKnowledgePackages(builder.getKnowledgePackages());
  }
}
