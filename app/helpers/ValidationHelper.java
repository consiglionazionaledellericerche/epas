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
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.List;
import play.data.validation.Error;


/**
 * Classe di utilita' per visualizzare gli errori di validazione nel flash scope
 * recupera il nome e il messaggio di validazione dai Messages.
 *
 * @author Daniele Murgia
 */
public class ValidationHelper {
  //FIXME implementare la validazione nei modali oppure rimuovere i modali per le form

  /**
   * La stringa contenente gli errori segnalati.
   *
   * @param errors la lista degli errori
   * @return la stringa contenente gli errori passati come parametro.
   */
  public static String errorsMessages(List<Error> errors) {

    return FluentIterable.from(errors).filter(new Predicate<Error>() {
      @Override
      public boolean apply(Error input) {
        return !input.message().equals("Validation failed");
      }
    }).transform(ErrorToString.ISTANCE).join(Joiner.on(";  "));
  }


  /**
   * Trasforma un Error (play) in una stringa visualizzabile all'utente.
   */
  public enum ErrorToString implements Function<Error, String> {
    ISTANCE;

    @Override
    public String apply(Error input) {
      return input.getKey() + ":" + input.message();
    }
  }

}
