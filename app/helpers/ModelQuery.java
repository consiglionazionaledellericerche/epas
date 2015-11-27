package helpers;

import com.mysema.query.SearchResults;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;
import com.mysema.query.types.Expression;

import play.db.jpa.JPA;

import java.util.List;

import javax.inject.Provider;
import javax.persistence.EntityManager;

/**
 * @author marco
 */
@Deprecated
public final class ModelQuery {

  public static final int PAGE_SIZE = 200;
  private static JPAQueryFactory factory =
          new JPAQueryFactory(new Provider<EntityManager>() {

            @Override
            public EntityManager get() {
              return JPA.em();
            }
          });

  private ModelQuery() {
  }

  /**
   * @return un query factory per il querydsl
   */
  @Deprecated
  public static JPQLQueryFactory queryFactory() {
    return factory;
  }

  public static <T> SimpleResults<T> simpleResults(JPQLQuery query, Expression<T> e) {
    return new SimpleResults<T>(query, e);
  }

  /**
   * @author marco
   */
  @Deprecated
  public static class SimpleResults<T> {
    private final Expression<T> e;
    private final JPQLQuery query;
    public int count = 0;
    public int page = 0;
    public int page_size = PAGE_SIZE;
    public int totalPage = 0;

    SimpleResults(JPQLQuery query, Expression<T> e) {
      this.query = query;
      this.count = (int) query.count();
      this.totalPage = this.count / this.page_size;
      if (this.count % this.page_size != 0)
        this.totalPage++;
      this.e = e;
    }

    public List<T> list() {
      return query.list(e);
    }

    public SearchResults<T> paginated(int page) {
      this.page = page;
      return query.offset(page * PAGE_SIZE)
              .limit(PAGE_SIZE)
              .listResults(e);
    }
  }
}
