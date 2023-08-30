/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
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

package manager.services;

import controllers.SecurityTokens;
import dao.GeneralSettingDao;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import javax.inject.Inject;

/**
 * Inserirsce l'autenticazione Bearer Token tramite l'apposito header http, prelevando
 * il jwt presente per l'utente corrente.
 *
 * @author Cristian Lucchesi
 *
 */
public class AuthRequestInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    if (SecurityTokens.getCurrentJwt().isPresent()) {
      template.header(
          "Authorization", String.format("Bearer %s", SecurityTokens.getCurrentJwt().get()));
    }
  }
}