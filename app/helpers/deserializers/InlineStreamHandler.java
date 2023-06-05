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

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;


/**
 * Uno stream handler per fare il mock di dati in memoria.
 *
 * @author Marco Andreini
 *
 */
public class InlineStreamHandler extends URLStreamHandler {

  /**
   * classe per le connesioni.
   *
   * @author dario
   *
   */
  public static class InlineUrlConnection extends URLConnection {

    private final byte[] data;
    private final String contentType;

    protected InlineUrlConnection(URL url, byte[] data, String contentType) {
      super(url);
      this.data = data;
      this.contentType = contentType;
    }

    @Override
    public void connect() throws IOException {
      // do nothing
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return ByteSource.wrap(data).openStream();
    }

    @Override
    public String getContentType() {
      return contentType;
    }
  }

  private final byte[] data;
  private final String type;

  /**
   * Costruttore.
   *
   * @param data array di dati
   * @param type il tipo
   */
  public InlineStreamHandler(byte[] data, String type) {
    super();
    this.data = data;
    this.type = type;
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new InlineUrlConnection(url, data, type);
  }
}