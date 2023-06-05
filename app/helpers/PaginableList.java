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

import java.util.List;

/**
 * Paginatore per le liste.
 *
 * @author Alessandro Martelli
 */
public final class PaginableList<T> {

  public static final int PAGE_SIZE = 10;
  private final List<T> items;
  public int page = 0;
  public int pageSize = PAGE_SIZE;
  public int totalPage = 0;

  /**
   * Costrusice un oggetto paginabile.
   *
   * @param items la lista di oggetti
   * @param page il numero di pagine
   */
  public PaginableList(List<T> items, int page) {
    this.items = items;
    this.page = page;
    int count = (int) items.size();
    this.totalPage = count / this.pageSize;
    if (count % this.pageSize != 0) {
      this.totalPage++;
    }
  }

  public List<T> items() {
    return items;
  }

  /**
   * Lista di elementi paginati.
   *
   * @return lista di elementi paginati.
   */
  public List<T> getPaginatedItems() {
    int offset = this.page * PAGE_SIZE;
    if (offset + PAGE_SIZE >= items.size()) {
      return this.items.subList(offset, items.size());
    } else {
      return this.items.subList(offset, offset + PAGE_SIZE);
    }

  }

}
