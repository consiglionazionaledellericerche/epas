package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.TeleworkValidation;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTeleworkValidation is a Querydsl query type for TeleworkValidation
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QTeleworkValidation extends EntityPathBase<TeleworkValidation> {

    private static final long serialVersionUID = 1678377656L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTeleworkValidation teleworkValidation = new QTeleworkValidation("teleworkValidation");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<java.time.LocalDate> approvationDate = createDate("approvationDate", java.time.LocalDate.class);

    public final BooleanPath approved = createBoolean("approved");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QTeleworkValidation(String variable) {
        this(TeleworkValidation.class, forVariable(variable), INITS);
    }

    public QTeleworkValidation(Path<? extends TeleworkValidation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTeleworkValidation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTeleworkValidation(PathMetadata metadata, PathInits inits) {
        this(TeleworkValidation.class, metadata, inits);
    }

    public QTeleworkValidation(Class<? extends TeleworkValidation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

