package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.Stamping;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QStamping is a Querydsl query type for Stamping
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QStamping extends EntityPathBase<Stamping> {

    private static final long serialVersionUID = -374367133;

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

    public final StringPath note = createString("note");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonDay personDay;

    public final BooleanPath serviceStamping = createBoolean("serviceStamping");

    public final QStampModificationType stampModificationType;

    public final QStampType stampType;

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
        this.badgeReader = inits.isInitialized("badgeReader") ? new QBadgeReader(forProperty("badgeReader")) : null;
        this.personDay = inits.isInitialized("personDay") ? new QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
        this.stampModificationType = inits.isInitialized("stampModificationType") ? new QStampModificationType(forProperty("stampModificationType")) : null;
        this.stampType = inits.isInitialized("stampType") ? new QStampType(forProperty("stampType")) : null;
    }

}

