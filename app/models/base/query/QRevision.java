package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.Revision;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QRevision is a Querydsl query type for Revision
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QRevision extends EntityPathBase<Revision> {

    private static final long serialVersionUID = -1702410764L;

    public static final QRevision revision = new QRevision("revision");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Long> timestamp = createNumber("timestamp", Long.class);

    public QRevision(String variable) {
        super(Revision.class, forVariable(variable));
    }

    public QRevision(Path<? extends Revision> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRevision(PathMetadata<?> metadata) {
        super(Revision.class, metadata);
    }

}

