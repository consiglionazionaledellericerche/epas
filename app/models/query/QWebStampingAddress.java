package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.WebStampingAddress;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QWebStampingAddress is a Querydsl query type for WebStampingAddress
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QWebStampingAddress extends EntityPathBase<WebStampingAddress> {

    private static final long serialVersionUID = -7191067;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWebStampingAddress webStampingAddress = new QWebStampingAddress("webStampingAddress");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QConfiguration confParameters;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final StringPath webAddressType = createString("webAddressType");

    public QWebStampingAddress(String variable) {
        this(WebStampingAddress.class, forVariable(variable), INITS);
    }

    public QWebStampingAddress(Path<? extends WebStampingAddress> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QWebStampingAddress(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QWebStampingAddress(PathMetadata<?> metadata, PathInits inits) {
        this(WebStampingAddress.class, metadata, inits);
    }

    public QWebStampingAddress(Class<? extends WebStampingAddress> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.confParameters = inits.isInitialized("confParameters") ? new QConfiguration(forProperty("confParameters")) : null;
    }

}

