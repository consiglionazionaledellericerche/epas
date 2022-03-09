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

package common.security;

import com.google.common.base.MoreObjects;
import lombok.Getter;

/**
 * Seam like check.
 *
 * @author Marco Andreini
 */
@Getter
public class PermissionCheck {

  private final PermissionCheckKey key;
  private boolean granted = false;

  public PermissionCheck(Object target, String action) {
    key = new PermissionCheckKey(target, action);
  }

  public String getAction() {
    return key.getAction();
  }

  public Object getTarget() {
    return key.getTarget();
  }

  public void grant() {
    this.granted = true;
  }

  public void revoke() {
    this.granted = false;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).omitNullValues()
        .add("action", getAction())
        .add("target", getTarget())
        .addValue(granted ? "GRANTED" : "DENIED").toString();
  }
}
