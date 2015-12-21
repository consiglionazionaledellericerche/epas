package helpers.deserializers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.google.common.io.ByteSource;


/**
 * Uno stram handler per fare il mock di dati in memoria.
 * 
 * @author marco
 *
 */
public class InlineStreamHandler extends URLStreamHandler {
	
	public static class InlineURLConnection extends URLConnection {
		
		private final byte[] data;
		private final String contentType;

		protected InlineURLConnection(URL url, byte[] data, String contentType) {
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
	
	public InlineStreamHandler(byte[] data, String type) {
		super();
		this.data = data;
		this.type = type;
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new InlineURLConnection(url, data, type);
	}
}