package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.BadgeReader;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBadgeReader is a Querydsl query type for BadgeReader
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QUser user;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final ListPath<models.Zone, QZone> zones = this.<models.Zone, QZone>createList("zones", models.Zone.class, QZone.class, PathInits.DIRECT2);

    public QBadgeReader(String variable) {
        this(BadgeReader.class, forVariable(variable), INITS);
    }

    public QBadgeReader(Path<? extends BadgeReader> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBadgeReader(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBadgeReader(PathMetadata metadata, PathInits inits) {
        this(BadgeReader.class, metadata, inits);
    }

    public QBadgeReader(Class<? extends BadgeReader> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

