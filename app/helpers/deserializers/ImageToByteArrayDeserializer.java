package helpers.deserializers;

import com.google.common.base.Verify;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Base64;

/**
 * Converte una immagine codificata in image/png dal browser in una array di
 * byte.
 *
 * @author marco
 *
 */
public class ImageToByteArrayDeserializer implements JsonDeserializer<byte[]> {

  private static final String IMAGE_MAGIK = "data:image/png;base64,";

  /**
   * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement,
   *    java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
   */
  @Override
  public byte[] deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    final String value = json.getAsString();
    Verify.verify(value.startsWith(IMAGE_MAGIK));
    return Base64.getDecoder().decode(value
        .substring(IMAGE_MAGIK.length()));
  }
}

