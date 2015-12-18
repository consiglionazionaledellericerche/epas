package models.query;

import com.mysema.query.types.Path;
import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.EnumPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathInits;
import com.mysema.query.types.path.SimplePath;
import com.mysema.query.types.path.StringPath;

import models.Stamping;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

import javax.annotation.Generated;


/**
 * QStamping is a Querydsl query type for Stamping
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStamping extends EntityPathBase<Stamping> {

    private static final long serialVersionUID = -374367133L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStamping stamping = new QStamping("stamping");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QBadgeReader badgeReader;

    public final DateTimePath<org.joda.time.LocalDateTime> date = createDateTime("date", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath markedByAdmin = createBoolean("markedByAdmin");

    public final BooleanPath markedByEmployee = createBoolean("markedByEmployee");

    public final StringPath note = createString("note");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonDay personDay;

    public final QStampModificationType stampModificationType;

    public final EnumPath<models.enumerate.StampTypes> stampType = createEnum("stampType", models.enumerate.StampTypes.class);

    public final EnumPath<Stamping.WayType> way = createEnum("way", Stamping.WayType.class);

    public QStamping(String variable) {
        this(Stamping.class, forVariable(variable), INITS);
    }

    public QStamping(Path<? extends Stamping> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QStamping(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QStamping(PathMetadata<?> metadata, PathInits inits) {
        this(Stamping.class, metadata, inits);
    }

    public QStamping(Class<? extends Stamping> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.badgeReader = inits.isInitialized("badgeReader") ? new QBadgeReader(forProperty("badgeReader"), inits.get("badgeReader")) : null;
        this.personDay = inits.isInitialized("personDay") ? new QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
        this.stampModificationType = inits.isInitialized("stampModificationType") ? new QStampModificationType(forProperty("stampModificationType")) : null;
    }

}

