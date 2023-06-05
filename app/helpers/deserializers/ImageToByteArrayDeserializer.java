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
 * @author Marco Andreini
 *
 */
public class ImageToByteArrayDeserializer implements JsonDeserializer<byte[]> {

  private static final String IMAGE_MAGIK = "data:image/png;base64,";

  /*
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

