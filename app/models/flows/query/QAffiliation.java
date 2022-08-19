package models.flows.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.flows.Affiliation;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAffiliation is a Querydsl query type for Affiliation
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QAffiliation extends EntityPathBase<Affiliation> {

    private static final long serialVersionUID = 248687843L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAffiliation affiliation = new QAffiliation("affiliation");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final DatePath<java.time.LocalDate> beginDate = createDate("beginDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath externalId = createString("externalId");

    public final QGroup group;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<java.math.BigDecimal> percentage = createNumber("percentage", java.math.BigDecimal.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final models.query.QPerson person;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QAffiliation(String variable) {
        this(Affiliation.class, forVariable(variable), INITS);
    }

    public QAffiliation(Path<? extends Affiliation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAffiliation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAffiliation(PathMetadata metadata, PathInits inits) {
        this(Affiliation.class, metadata, inits);
    }

    public QAffiliation(Class<? extends Affiliation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.group = inits.isInitialized("group") ? new QGroup(forProperty("group"), inits.get("group")) : null;
        this.person = inits.isInitialized("person") ? new models.query.QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

