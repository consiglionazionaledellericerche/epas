package helpers;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;

import dao.PersonDao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.ICertificationService;
import manager.attestati.service.OauthToken;
import manager.attestati.service.PersonCertData;

import models.Office;
import models.Person;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;


/**
 * @author daniele
 * @since 28/11/16.
 */
@Slf4j
@RequiredArgsConstructor
public final class CacheValues {

  private static final int FIVE_MINUTES = 5 * DateTimeConstants.SECONDS_PER_MINUTE;

  private final CertificationsComunication certification;
  private final ICertificationService certService;
  private final PersonDao personDao;

  public LoadingCache<String, OauthToken> oauthToken = CacheBuilder.newBuilder()
      // Considerando che attestati fornisce i nuovi token con validità di 9:30 e i token tramite
      // refresh-token con validità 12 ore, una scadenza di 13 ore sembra un buon default...
      .expireAfterWrite(13, TimeUnit.HOURS)
      .refreshAfterWrite(1, TimeUnit.MINUTES)
      .build(new OauthTokenCacheLoader());

  public LoadingCache<Map.Entry<Office, YearMonth>, Set<Integer>> attestatiSerialNumbers =
      CacheBuilder.newBuilder()
          .expireAfterWrite(20, TimeUnit.MINUTES)
          .build(new CacheLoader<Map.Entry<Office, YearMonth>, Set<Integer>>() {
            @Override
            public Set<Integer> load(Map.Entry<Office, YearMonth> key)
                throws ExecutionException, NoSuchFieldException {
              return certification.getPeopleList(
                  key.getKey(), key.getValue().getYear(), key.getValue().getMonthOfYear());
            }
          });


  // Indica la percentuale di ogni persona nell'eleborazione totale dell'ufficio
  // viene utilizzatpo per la progressione della progressbar di caricamento e invio
  public LoadingCache<Map.Entry<Office, YearMonth>, Double> elaborationStep =
      CacheBuilder.newBuilder()
          .expireAfterWrite(20, TimeUnit.MINUTES)
          .build(new StepCacheLoader());

  /**
   * Occhio a questi valori che vengono rinnovati nel caso vengano inviate le informazioni
   * ad attestati.
   * Bisognerebbe evitare di impacchettare i dati presenti su epas e quelli presenti in attestati
   * in un unica struttura ed effettuare le elaborazioni a runtime, o eventualmente salvare
   * in cache anche quelle.
   * <p>
   * RICORDARSI di AGGIORNARE I VALORI CON UNA
   * personStatus.put(Map.Entry&lt;Long, YearMonth&gt;, PersonCertData>)
   * DOPO OGNI INVIO!!!!
   * </p>
   * E' ANCHE ALTAMENTE CONSIGLIATO INVALIDARE TUTTI I VALORI DI UN DETERMINATO UFFICIO
   * QUADO SI RIEFFETTUA IL REFRESH DELLA SCHERMATA DI ATTESTATI
   */
  public LoadingCache<Map.Entry<Person, YearMonth>, PersonCertData> personStatus =
      CacheBuilder.newBuilder()
          .expireAfterWrite(20, TimeUnit.MINUTES)
          .build(
              new CacheLoader<Map.Entry<Person, YearMonth>, PersonCertData>() {
                @Override
                public PersonCertData load(Map.Entry<Person, YearMonth> key)
                    throws ExecutionException {
                  final Person person = key.getKey();
                  int year = key.getValue().getYear();
                  int month = key.getValue().getMonthOfYear();
                  return certService
                      .buildPersonStaticStatus(person, year, month);
                }
              }
          );


  private class OauthTokenCacheLoader extends CacheLoader<String, OauthToken> {
    @Override
    public OauthToken load(String key) throws NoSuchFieldException {
      log.info("Nessun Token Oauth presente in cache, nuova richiesta token oauth");
      return certification.getToken();
    }

    // Refresh automatico (in asincrono) del token se sta per scadere
    // (meno di 5 minuti rimasti)
    @Override
    public ListenableFuture<OauthToken> reload(final String key, OauthToken token)
        throws NoSuchFieldException {
      // Se non sta per scadere restituisco quello che ho già
      if (!LocalDateTime.now().isAfter(token.taken_at
          .plusSeconds(token.expires_in - FIVE_MINUTES))) {
        log.info("Token Oauth gia' presente in cache: token {}, expires_in {}", token.access_token,
            token.expires_in);
        return Futures.immediateFuture(token);
      } else if (LocalDateTime.now().isAfter(token.taken_at.plusSeconds(token.expires_in))) {
        // Se è già scaduto lo richiedo in maniera sincrona
        log.info("Token Oauth presente in cache scaduto. Invio nuova richiesta per un " 
            + "refresh-token: token {}, expires_in {}", token.access_token, token.expires_in);
        return Futures.immediateFuture(certification.refreshToken(token));
      } else {
        log.info("Token Oauth presente in cache in scadenza. Restituito valore attuale e invio " 
            + "nuova richiesta (in modalita' asincrona) di un refresh Token: token {}, expires_in "
            + "{}", token.access_token, token.expires_in);
        // Faccio il refresh in maniera asincrona
        ListenableFutureTask<OauthToken> task = ListenableFutureTask
            .create(() -> certification.refreshToken(token));
        MoreExecutors.directExecutor().execute(task);
        return task;
      }
    }
  }

  private class StepCacheLoader extends CacheLoader<Map.Entry<Office, YearMonth>, Double> {
    @Override
    public Double load(Map.Entry<Office, YearMonth> key) throws ExecutionException {
      final Set<Integer> matricoleAttestati = attestatiSerialNumbers.get(key);
      final int year = key.getValue().getYear();
      final int month = key.getValue().getMonthOfYear();

      final LocalDate monthBegin = new LocalDate(year, month, 1);
      final LocalDate monthEnd = monthBegin.dayOfMonth().withMaximumValue();

      final List<Person> people = personDao.list(Optional.absent(),
          Sets.newHashSet(Lists.newArrayList(key.getKey())), false,
          monthBegin, monthEnd, true).list();

      final Set<Integer> matricoleEpas = people.stream().map(person -> person.number)
          .distinct().collect(Collectors.toSet());

      final Set<Integer> matchNumbers = Sets.newHashSet(matricoleEpas);
      matchNumbers.retainAll(matricoleAttestati);
      log.debug("Calcolata percentuale caricamento per persona per l'ufficio {} - mese {}/{}",
          key.getKey(), month, year);
      return 100 / (double) matchNumbers.size();
    }
  }
}
