package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.StampProfile;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QStampProfile is a Querydsl query type for StampProfile
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStampProfile extends EntityPathBase<StampProfile> {

    private static final long serialVersionUID = -219349622;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStampProfile stampProfile = new QStampProfile("stampProfile");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> endTo = createDate("endTo", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath fixedWorkingTime = createBoolean("fixedWorkingTime");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public final DatePath<org.joda.time.LocalDate> startFrom = createDate("startFrom", org.joda.time.LocalDate.class);

    public QStampProfile(String variable) {
        this(StampProfile.class, forVariable(variable), INITS);
    }

    public QStampProfile(Path<? extends StampProfile> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QStampProfile(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QStampProfile(PathMetadata<?> metadata, PathInits inits) {
        this(StampProfile.class, metadata, inits);
    }

    public QStampProfile(Class<? extends StampProfile> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

