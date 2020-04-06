package helpers;

import java.util.List;

/**
 * Paginatore per le liste.
 * 
 * @author alessandro
 */
public final class PaginableList<T> {

  public static final int PAGE_SIZE = 10;
  private final List<T> items;
  public int page = 0;
  public int pageSize = PAGE_SIZE;
  public int totalPage = 0;

  /**
   * Costrusice un oggetto paginabile.
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
