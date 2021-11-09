package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.CheckGreenPass;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCheckGreenPass is a Querydsl query type for CheckGreenPass
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCheckGreenPass extends EntityPathBase<CheckGreenPass> {

    private static final long serialVersionUID = -550901328L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCheckGreenPass checkGreenPass = new QCheckGreenPass("checkGreenPass");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<org.joda.time.LocalDate> checkDate = createDate("checkDate", org.joda.time.LocalDate.class);

    public final BooleanPath checked = createBoolean("checked");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QCheckGreenPass(String variable) {
        this(CheckGreenPass.class, forVariable(variable), INITS);
    }

    public QCheckGreenPass(Path<? extends CheckGreenPass> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCheckGreenPass(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCheckGreenPass(PathMetadata metadata, PathInits inits) {
        this(CheckGreenPass.class, metadata, inits);
    }

    public QCheckGreenPass(Class<? extends CheckGreenPass> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

