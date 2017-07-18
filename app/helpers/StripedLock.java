package helpers;

import com.google.common.base.Verify;
import com.google.common.util.concurrent.Striped;

import java.util.concurrent.locks.Lock;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper per gli Striped<Lock> perché non funziona
 * la @Inject nei controller di un oggetto con i generic.
 * 
 * @author cristian
 *
 */
@Getter @RequiredArgsConstructor
public class StripedLock {

  private final Striped<Lock> lock;
  
  public Lock get(Object lockId) {
    Verify.verifyNotNull(lock);
    return lock.get(lockId);
  }
}
