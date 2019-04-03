package dao;

import java.util.Collection;

/**
 * Interface to store the results of expensive function calls and return the cached Collection when
 * the same inputs occur again.
 *
 * @author cristian
 */
public interface MemoizedCollection<T> {

  long getCount();

  Collection<T> getPartialList();

  Collection<T> getList();

}
