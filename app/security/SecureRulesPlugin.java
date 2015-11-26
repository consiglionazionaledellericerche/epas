package security;

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

import java.util.Properties;

/**
 * @author marco
 */
public class SecureRulesPlugin extends PlayPlugin {

  private final static Logger log = LoggerFactory.getLogger(SecureRulesPlugin.class);
  private final static String FILENAME = "permissions.drl";

  static KnowledgeBase knowledgeBase;
  Long lastModified;
  // private static String hash;


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
    // LOG.info("(re)loading drools.");

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
      throw new RuntimeException("Drools compilation failed: " +
              builder.getErrors().size() + " errors");
    }

    knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
    knowledgeBase.addKnowledgePackages(builder.getKnowledgePackages());
  }
}
