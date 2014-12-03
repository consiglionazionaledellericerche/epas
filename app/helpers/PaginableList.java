package helpers;

import java.util.List;

/**
 * @author alessandro
 *
 */
public final class PaginableList<T> {
	
	private final List<T> items;
	public int page = 0;
	public int page_size = PAGE_SIZE;
	public int totalPage = 0;
	
	public PaginableList(List<T> items, int page) {
		this.items = items;
		this.page = page;
		int count = (int)items.size();
		this.totalPage = count / this.page_size;
		if(count%this.page_size != 0)
			this.totalPage++;
	}
	
	public List<T> items() {
		return items;
	}
	
	public List<T> getPaginatedItems() {
		int offset = this.page * PAGE_SIZE;
		if(offset + PAGE_SIZE >= items.size())
			return this.items.subList(offset, items.size());
		else
			return this.items.subList(offset, offset + PAGE_SIZE);
			
	}
	
	public static final int PAGE_SIZE = 10;
	
}
