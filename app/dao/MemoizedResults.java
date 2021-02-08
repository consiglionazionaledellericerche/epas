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

package dao;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import helpers.jpa.ModelQuery.SimpleResults;
import java.util.Collection;

/**
 * It stores the results of expensive function calls and returns the cached Collection when the same
 * inputs occur again.
 *
 * @param <T> the collection objects type
 * @author Marco Andreini
 * @author Cristian Lucchesi
 */
public class MemoizedResults<T> implements MemoizedCollection<T> {

  private static final int FIRST_ITEMS = 5;

  private final Supplier<Long> count;
  private final Supplier<Collection<T>> partial;
  private final Supplier<Collection<T>> list;

  MemoizedResults(final Supplier<SimpleResults<T>> results) {
    count = Suppliers.memoize(() -> results.get().count());
    partial = Suppliers.memoize(() -> results.get().list(FIRST_ITEMS));
    list = Suppliers.memoize(() -> results.get().list());
  }

  /**
   * Getter dei count.
   */
  public long getCount() {
    return count.get();
  }

  /**
   * Getter della lista dei partial.
   */
  public Collection<T> getPartialList() {
    return partial.get();
  }

  /**
   * Getter della lista.
   */
  public Collection<T> getList() {
    return list.get();
  }

  /**
   * Memorizza i risultati per non ripetere la computazione.
   */
  public static <T> MemoizedResults<T> memoize(Supplier<SimpleResults<T>> result) {
    return new MemoizedResults<>(result);
  }
}