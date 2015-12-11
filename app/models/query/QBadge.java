package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.Badge;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;


/**
 * QBadge is a Querydsl query type for Badge
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QBadge extends EntityPathBase<Badge> {

    private static final long serialVersionUID = -1439233281L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBadge badge = new QBadge("badge");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QBadgeReader badgeReader;

    public final QBadgeSystem badgeSystem;

    public final StringPath code = createString("code");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public QBadge(String variable) {
        this(Badge.class, forVariable(variable), INITS);
    }

    public QBadge(Path<? extends Badge> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QBadge(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QBadge(PathMetadata<?> metadata, PathInits inits) {
        this(Badge.class, metadata, inits);
    }

    public QBadge(Class<? extends Badge> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.badgeReader = inits.isInitialized("badgeReader") ? new QBadgeReader(forProperty("badgeReader"), inits.get("badgeReader")) : null;
        this.badgeSystem = inits.isInitialized("badgeSystem") ? new QBadgeSystem(forProperty("badgeSystem"), inits.get("badgeSystem")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

