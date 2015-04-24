package models.base.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.base.Revision;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QRevision is a Querydsl query type for Revision
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QRevision extends EntityPathBase<Revision> {

    private static final long serialVersionUID = -1702410764L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRevision revision = new QRevision("revision");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath ipaddress = createString("ipaddress");

    public final models.query.QUser owner;

    public final NumberPath<Long> timestamp = createNumber("timestamp", Long.class);

    public QRevision(String variable) {
        this(Revision.class, forVariable(variable), INITS);
    }

    public QRevision(Path<? extends Revision> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QRevision(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QRevision(PathMetadata<?> metadata, PathInits inits) {
        this(Revision.class, metadata, inits);
    }

    public QRevision(Class<? extends Revision> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new models.query.QUser(forProperty("owner"), inits.get("owner")) : null;
    }

}

