package helpers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.OauthToken;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDateTime;

import injection.StaticInject;
import static play.Invoker.executor;

import java.util.concurrent.TimeUnit;

/**
 * @author daniele
 * @since 28/11/16.
 */
@Slf4j
@StaticInject
public class CacheValues {

  private static final int FIVE_MINUTES = 5 * DateTimeConstants.SECONDS_PER_MINUTE;
  @Inject
  static CertificationsComunication certification;

  // Meglio non statico??
  public static LoadingCache<String, OauthToken> oauthToken = CacheBuilder.newBuilder()
      .refreshAfterWrite(1, TimeUnit.MINUTES)
      .build(
          new CacheLoader<String, OauthToken>() {
            @Override
            public OauthToken load(String key) {
              return certification.getToken();
            }

            // Refresh automatico (in asincrono) del token se sta per scadere
            // (meno di 5 minuti rimasti)
            // TODO scrivere metodo per la richiesta di un refresh token invece
            // che chiedere un nuovo token
            @Override
            public ListenableFuture<OauthToken> reload(final String key, OauthToken token) {
              // Se non sta per scadere restituisco quello che ho già
              if (!LocalDateTime.now().isAfter(token.took_at
                  .plusSeconds(token.expires_in - FIVE_MINUTES))) {
                return Futures.immediateFuture(token);
              } else if (LocalDateTime.now().isAfter(token.took_at.plusSeconds(token.expires_in))) {
                // Se è già scaduto lo richiedo in maniera sincrona
                return Futures.immediateFuture(certification.getToken());
              } else {
                // Altrimenti ne faccio il refresh in maniera asincrona
                ListenableFutureTask<OauthToken> task = ListenableFutureTask
                    .create(() -> certification.getToken());
                executor.execute(task);
                return task;
              }
            }
          });
}
