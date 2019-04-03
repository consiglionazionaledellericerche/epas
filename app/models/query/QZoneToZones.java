package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ZoneToZones;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QZoneToZones is a Querydsl query type for ZoneToZones
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QZoneToZones(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QZoneToZones(PathMetadata metadata, PathInits inits) {
        this(ZoneToZones.class, metadata, inits);
    }

    public QZoneToZones(Class<? extends ZoneToZones> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.zoneBase = inits.isInitialized("zoneBase") ? new QZone(forProperty("zoneBase"), inits.get("zoneBase")) : null;
        this.zoneLinked = inits.isInitialized("zoneLinked") ? new QZone(forProperty("zoneLinked"), inits.get("zoneLinked")) : null;
    }

}

