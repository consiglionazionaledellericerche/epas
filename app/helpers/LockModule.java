package helpers;

import com.google.common.util.concurrent.Striped;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import injection.AutoRegister;

/**
 * Fornisce l'accesso singleton ai lock dell'applicazione.
 * 
 * @author cristian
 *
 */
@AutoRegister
public class LockModule implements Module {
 
  private static int LOCK_POOL_SIZE = 1024;
  
  @Provides
  @Singleton
  public StripedLock getStripedLock() {
    return new StripedLock(Striped.lock(LOCK_POOL_SIZE));
  }
  
  @Override
  public void configure(Binder binder) {
    //Auto-generated method stub
  }

}
