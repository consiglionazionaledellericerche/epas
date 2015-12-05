package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.BadgeReader;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QBadgeReader is a Querydsl query type for BadgeReader
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QBadgeReader extends EntityPathBase<BadgeReader> {

    private static final long serialVersionUID = 642176162L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBadgeReader badgeReader = new QBadgeReader("badgeReader");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final SetPath<models.Badge, QBadge> badges = this.<models.Badge, QBadge>createSet("badges", models.Badge.class, QBadge.class, PathInits.DIRECT2);

    public final ListPath<models.BadgeSystem, QBadgeSystem> badgeSystems = this.<models.BadgeSystem, QBadgeSystem>createList("badgeSystems", models.BadgeSystem.class, QBadgeSystem.class, PathInits.DIRECT2);

    public final StringPath code = createString("code");

    public final StringPath description = createString("description");

    public final BooleanPath enabled = createBoolean("enabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath location = createString("location");

    public final QOffice owner;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QUser user;

    public QBadgeReader(String variable) {
        this(BadgeReader.class, forVariable(variable), INITS);
    }

    public QBadgeReader(Path<? extends BadgeReader> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QBadgeReader(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QBadgeReader(PathMetadata<?> metadata, PathInits inits) {
        this(BadgeReader.class, metadata, inits);
    }

    public QBadgeReader(Class<? extends BadgeReader> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new QOffice(forProperty("owner"), inits.get("owner")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

