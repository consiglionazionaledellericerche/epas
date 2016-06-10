package dao;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import helpers.jpa.ModelQuery.SimpleResults;

import java.util.Collection;
import java.util.List;

/**
 * It stores the results of expensive function calls and returns the cached Collection when the same
 * inputs occur again.
 *
 * @param <T> the collection objects type
 * @author marco
 * @author cristian
 */
public class MemoizedResults<T> implements MemoizedCollection<T> {

  private static final int FIRST_ITEMS = 5;

  private final Supplier<Long> count;
  private final Supplier<Collection<T>> partial;
  private final Supplier<Collection<T>> list;

  MemoizedResults(final Supplier<SimpleResults<T>> results) {

    count = Suppliers.memoize(new Supplier<Long>() {
      @Override
      public Long get() {
        return results.get().count();
      }
    });
    partial = Suppliers.memoize(new Supplier<Collection<T>>() {
      @Override
      public List<T> get() {
        return results.get().list(FIRST_ITEMS);
      }
    });
    list = Suppliers.memoize(new Supplier<Collection<T>>() {
      @Override
      public List<T> get() {
        return results.get().list();
      }
    });
  }

  public long getCount() {
    return count.get();
  }

  public Collection<T> getPartialList() {
    return partial.get();
  }

  public Collection<T> getList() {
    return list.get();
  }

  public static <T> MemoizedResults<T> memoize(Supplier<SimpleResults<T>> result) {
    return new MemoizedResults<>(result);
  }
}
