package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.PersonCompetenceCodes;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonCompetenceCodes is a Querydsl query type for PersonCompetenceCodes
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QPersonCompetenceCodes extends EntityPathBase<PersonCompetenceCodes> {

    private static final long serialVersionUID = 1175887140L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonCompetenceCodes personCompetenceCodes = new QPersonCompetenceCodes("personCompetenceCodes");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final QCompetenceCode competenceCode;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QPersonCompetenceCodes(String variable) {
        this(PersonCompetenceCodes.class, forVariable(variable), INITS);
    }

    public QPersonCompetenceCodes(Path<? extends PersonCompetenceCodes> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonCompetenceCodes(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonCompetenceCodes(PathMetadata metadata, PathInits inits) {
        this(PersonCompetenceCodes.class, metadata, inits);
    }

    public QPersonCompetenceCodes(Class<? extends PersonCompetenceCodes> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceCode = inits.isInitialized("competenceCode") ? new QCompetenceCode(forProperty("competenceCode"), inits.get("competenceCode")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

