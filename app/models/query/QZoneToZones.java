package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ZoneToZones;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QZoneToZones is a Querydsl query type for ZoneToZones
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QZoneToZones extends EntityPathBase<ZoneToZones> {

    private static final long serialVersionUID = 541318556L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QZoneToZones zoneToZones = new QZoneToZones("zoneToZones");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> delay = createNumber("delay", Integer.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final QZone zoneBase;

    public final QZone zoneLinked;

    public QZoneToZones(String variable) {
        this(ZoneToZones.class, forVariable(variable), INITS);
    }

    public QZoneToZones(Path<? extends ZoneToZones> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QZoneToZones(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QZoneToZones(PathMetadata<?> metadata, PathInits inits) {
        this(ZoneToZones.class, metadata, inits);
    }

    public QZoneToZones(Class<? extends ZoneToZones> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.zoneBase = inits.isInitialized("zoneBase") ? new QZone(forProperty("zoneBase"), inits.get("zoneBase")) : null;
        this.zoneLinked = inits.isInitialized("zoneLinked") ? new QZone(forProperty("zoneLinked"), inits.get("zoneLinked")) : null;
    }

}

