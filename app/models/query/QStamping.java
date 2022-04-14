package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Stamping;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStamping is a Querydsl query type for Stamping
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QStamping extends EntityPathBase<Stamping> {

    private static final long serialVersionUID = -374367133L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStamping stamping = new QStamping("stamping");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DateTimePath<org.joda.time.LocalDateTime> date = createDateTime("date", org.joda.time.LocalDateTime.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath markedByAdmin = createBoolean("markedByAdmin");

    public final BooleanPath markedByEmployee = createBoolean("markedByEmployee");

    public final BooleanPath markedByTelework = createBoolean("markedByTelework");

    public final StringPath note = createString("note");

    public final QPerson owner;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPersonDay personDay;

    public final StringPath place = createString("place");

    public final StringPath reason = createString("reason");

    public final StringPath stampingZone = createString("stampingZone");

    public final QStampModificationType stampModificationType;

    public final EnumPath<models.enumerate.StampTypes> stampType = createEnum("stampType", models.enumerate.StampTypes.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final EnumPath<Stamping.WayType> way = createEnum("way", Stamping.WayType.class);

    public final ComparablePath<org.joda.time.YearMonth> yearMonth = createComparable("yearMonth", org.joda.time.YearMonth.class);

    public QStamping(String variable) {
        this(Stamping.class, forVariable(variable), INITS);
    }

    public QStamping(Path<? extends Stamping> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStamping(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStamping(PathMetadata metadata, PathInits inits) {
        this(Stamping.class, metadata, inits);
    }

    public QStamping(Class<? extends Stamping> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new QPerson(forProperty("owner"), inits.get("owner")) : null;
        this.personDay = inits.isInitialized("personDay") ? new QPersonDay(forProperty("personDay"), inits.get("personDay")) : null;
        this.stampModificationType = inits.isInitialized("stampModificationType") ? new QStampModificationType(forProperty("stampModificationType")) : null;
    }

}

