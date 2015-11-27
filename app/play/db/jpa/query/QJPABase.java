package play.db.jpa.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import play.db.jpa.JPABase;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QJPABase is a Querydsl query type for JPABase
 */
@Generated("com.mysema.query.codegen.SupertypeSerializer")
public class QJPABase extends EntityPathBase<JPABase> {

    private static final long serialVersionUID = 1552226979L;

    public static final QJPABase jPABase = new QJPABase("jPABase");

    public final SimplePath<Object> entityId = createSimple("entityId", Object.class);

    public final BooleanPath persistent = createBoolean("persistent");

    public QJPABase(String variable) {
        super(JPABase.class, forVariable(variable));
    }

    public QJPABase(Path<? extends JPABase> path) {
        super(path.getType(), path.getMetadata());
    }

    public QJPABase(PathMetadata<?> metadata) {
        super(JPABase.class, metadata);
    }

}

