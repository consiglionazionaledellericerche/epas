package helpers;

import java.util.Optional;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import play.db.jpa.Blob;


/**
 * Funzioni di utilità per le immagini.
 *
 * @author Cristian Lucchesi
 *
 */
public class ImageUtils {

  /**
   * Deduce l'estensione del file dal MimeType del Blob 
   * (utilizzando le Apache Tika).
   */
  public static Optional<String> fileExtension(Blob blob) {
    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
    MimeType type;
    try {
      type = allTypes.forName(blob.type());
      return Optional.of(type.getExtension());
    } catch (MimeTypeException e) {
      return Optional.empty();
    }
  }
}
