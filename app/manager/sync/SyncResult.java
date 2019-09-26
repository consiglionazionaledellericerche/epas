package manager.sync;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contiene le informazioni relative ad una richiesta di sincronizzazione.
 * @author cristian
 */
@Data
@NoArgsConstructor
public class SyncResult {

  private boolean success = true;
  private List<String> messages = Lists.newArrayList();
  
  /**
   * Combina i risultati delle due sincronizzazioni.
   * La sincronizzazione è success solo se lo sono entrambe.
   */
  public SyncResult add(SyncResult other) {
    success = success && other.success;
    messages.addAll(other.getMessages());
    return this;
  }
  
  public SyncResult add(String message) {
    messages.add(message);
    return this;
  }

  public SyncResult setFailed() {
    success = false;
    return this;
  }  
  
  @Override
  public String toString() {
    return String.format("Sicronizzazione %s. %s",
        success ? "avvenuta con successo" : "fallita",
        Joiner.on(" ").join(messages));
  }
}