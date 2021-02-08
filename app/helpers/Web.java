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

package helpers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import play.i18n.Messages;

/**
 * Contiene metodi per ottenere i messaggi per le crud ed altre utilità per la parte template.
 *
 * @author Marco Andreini
 */
public final class Web {
  public static final String MSG_CANCELLED = "crud.cancelled";

  /**
   * Attenzione: richiesto il parametro object.
   */
  public static final String MSG_SAVED = "crud.saved";

  /**
   * Attenzione: richiesto il parametro object.
   */
  public static final String MSG_CREATED = "crud.created";

  /**
   * Attenzione: richiesto il parametro object.
   */
  public static final String MSG_MODIFIED = "crud.modified";

  /**
   * Attenzione richiesto il parametro object.
   */
  public static final String MSG_DELETED = "crud.deleted";

  public static final String MSG_HAS_ERRORS = "crud.hasErrors";
  
  public static final String MSG_DISABLED = "crud.disabled";

  private Web() {
  }

  private static String toName(Class<?> cls) {
    return Character.toLowerCase(cls.getSimpleName().charAt(0))
            + cls.getSimpleName().substring(1);
  }

  public static String msgCancelled() {
    return Messages.get(MSG_CANCELLED);
  }

  public static String msgSaved(Class<?> cls) {
    return Messages.get(MSG_SAVED, toName(cls));
  }

  public static String msgCreated(Class<?> cls) {
    return Messages.get(MSG_CREATED, toName(cls));
  }

  public static String msgModified(Class<?> cls) {
    return Messages.get(MSG_MODIFIED, toName(cls));
  }

  public static String msgDeleted(Class<?> cls) {
    return Messages.get(MSG_DELETED, toName(cls));
  }

  public static String msgHasErrors() {
    return Messages.get(MSG_HAS_ERRORS);
  }
  
  public static String msgDisabled(Class<?> cls) {
    return Messages.get(MSG_DISABLED, toName(cls));
  }

  /**
   * Serializza gli attributi.
   *
   * @return gli attributi serializzati.
   */
  public static String serialize(Map<String, ?> attributes, String... unless) {
    return Joiner.on(' ').skipNulls().join(Iterables.transform(Maps.filterKeys(attributes,
            Predicates.not(Predicates.in(ImmutableSet.copyOf(unless)))).entrySet(),
            EntryToAttribute.INSTANCE));
  }

  enum EntryToAttribute implements Function<Entry<String, ?>, String> {
    INSTANCE;

    /**
     * Senza riguardo per l'elemento in cui sono riportati.
     */
    static final Set<String> BOOLEAN_ATTRIBUTES = ImmutableSet
            .of("checked", "selected", "disabled",
                    "readonly", "multiple", "ismap");

    @Override
    public String apply(Entry<String, ?> entry) {
      final String key = entry.getKey();
      final Object value = entry.getValue();
      if (BOOLEAN_ATTRIBUTES.contains(key)) {
        if (value == null || value.equals(false)) {
          return null;
        } else {
          return key + "=\"" + key + "\"";
        }
      } else {
        return entry.getKey() + "=\"" + value.toString() + "\"";
      }
    }
  }

  ;
}
