package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.PersonCompetenceCodes;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QPersonCompetenceCodes is a Querydsl query type for PersonCompetenceCodes
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QPersonCompetenceCodes extends EntityPathBase<PersonCompetenceCodes> {

    private static final long serialVersionUID = 1175887140L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonCompetenceCodes personCompetenceCodes = new QPersonCompetenceCodes("personCompetenceCodes");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final QCompetenceCode competenceCode;

    public final DatePath<org.joda.time.LocalDate> enablingDate = createDate("enablingDate", org.joda.time.LocalDate.class);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final QPerson person;

    public QPersonCompetenceCodes(String variable) {
        this(PersonCompetenceCodes.class, forVariable(variable), INITS);
    }

    public QPersonCompetenceCodes(Path<? extends PersonCompetenceCodes> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonCompetenceCodes(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QPersonCompetenceCodes(PathMetadata<?> metadata, PathInits inits) {
        this(PersonCompetenceCodes.class, metadata, inits);
    }

    public QPersonCompetenceCodes(Class<? extends PersonCompetenceCodes> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceCode = inits.isInitialized("competenceCode") ? new QCompetenceCode(forProperty("competenceCode"), inits.get("competenceCode")) : null;
        this.person = inits.isInitialized("person") ? new QPerson(forProperty("person"), inits.get("person")) : null;
    }

}

