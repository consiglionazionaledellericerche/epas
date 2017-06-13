package events;

import com.google.common.base.Verify;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import injection.StaticInject;
import models.base.BaseModel;

/**
 * @author daniele
 * @since 10/06/17.
 */
@StaticInject
public final class EntityEvents {

  @Inject
  static EventBus eventBus;

  /**
   * Propaga un evento consistente nel `model` stesso, da chiamare
   * successivamente agli eventi C-U-D. Da notare che in caso sia stato
   * cancellato l'oggetto sarà non più persistent.
   */
  public static void changed(BaseModel model) {
    Verify.verifyNotNull(eventBus).post(model);
  }

  public static void deleted(BaseModel model) {
    // TODO
  }
}

