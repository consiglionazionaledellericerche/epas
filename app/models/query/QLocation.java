package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Location;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QLocation is a Querydsl query type for Location
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QLocation extends EntityPathBase<Location> {

    private static final long serialVersionUID = 214606553L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLocation location = new QLocation("location");

    public final play.db.jpa.query.QModel _super = new play.db.jpa.query.QModel(this);

    public final StringPath department = createString("department");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath headOffice = createString("headOffice");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final StringPath room = createString("room");

    public QLocation(String variable) {
        this(Location.class, forVariable(variable), INITS);
    }

    public QLocation(Path<? extends Location> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QLocation(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QLocation(PathMetadata<?> metadata, PathInits inits) {
        this(Location.class, metadata, inits);
    }

    public QLocation(Class<? extends Location> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

