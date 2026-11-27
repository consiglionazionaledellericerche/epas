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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.Properties;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.kiesession.rulebase.KnowledgeBaseFactory;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.vfs.VirtualFile;

/**
 * Plugin per il caricamento dei componenti necessari alle Drools.
 *
 * @author Marco Andreini
 */
public class SecureRulesPlugin extends PlayPlugin {

  private static final Logger log = LoggerFactory.getLogger(SecureRulesPlugin.class);
  private static final String FILENAME = "permissions.drl";

  static KieBase knowledgeBase;
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

    // Le regole sono compilate con il compilatore Eclipse (JDT), che legge il bytecode dei
    // tipi importati come risorse .class: il classloader ponte le espone per le classi
    // applicative. Il livello del linguaggio è fissato perché la versione di JDT nel
    // classpath non conosce quello della JVM in uso.
    Properties properties = new Properties();
    properties.put("drools.dialect.java.compiler", "ECLIPSE");
    properties.put("drools.dialect.java.compiler.lnglevel", "1.8");
    Thread.currentThread().setContextClassLoader(Play.classloader);
    final KnowledgeBuilderConfiguration configuration = KnowledgeBuilderFactory
            .newKnowledgeBuilderConfiguration(properties, new BytecodeBridge(Play.classloader));
    final KnowledgeBuilder builder = KnowledgeBuilderFactory
            .newKnowledgeBuilder(configuration);

    // Compile the rules file.
    builder.add(ResourceFactory.newByteArrayResource(rulesContent), ResourceType.DRL);
    if (builder.hasErrors()) {
      log.error(builder.getErrors().toString());
      throw new RuntimeException("Drools compilation failed: "
              + builder.getErrors().size() + " errors");
    }

    InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
    kbase.addPackages(builder.getKnowledgePackages());
    knowledgeBase = kbase;
  }

  /**
   * Espone come risorse .class il bytecode delle classi applicative, che il classloader di Play
   * non rende disponibile: è preso dalle classi compilate in memoria o da quelle precompilate.
   */
  private static final class BytecodeBridge extends ClassLoader {

    BytecodeBridge(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected URL findResource(String name) {
      final byte[] bytecode = bytecode(name);
      if (bytecode == null) {
        return null;
      }
      try {
        return new URL(null, "bytecode:" + name, new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {
              @Override
              public void connect() {
                // il contenuto è già in memoria
              }

              @Override
              public InputStream getInputStream() {
                return new ByteArrayInputStream(bytecode);
              }
            };
          }
        });
      } catch (MalformedURLException e) {
        return null;
      }
    }

    private static byte[] bytecode(String name) {
      if (!name.endsWith(".class")) {
        return null;
      }
      String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
      ApplicationClass applicationClass = Play.classes.getApplicationClass(className);
      if (applicationClass != null && applicationClass.enhancedByteCode != null) {
        return applicationClass.enhancedByteCode;
      }
      File precompiled = Play.getFile("precompiled/java/" + name);
      if (Play.usePrecompiled && precompiled.exists()) {
        try {
          return Files.readAllBytes(precompiled.toPath());
        } catch (IOException e) {
          log.warn("bytecode precompilato non leggibile: {}", precompiled, e);
        }
      }
      return null;
    }
  }
}
