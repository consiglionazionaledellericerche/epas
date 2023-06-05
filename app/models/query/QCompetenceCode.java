package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.CompetenceCode;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCompetenceCode is a Querydsl query type for CompetenceCode
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QCompetenceCode extends EntityPathBase<CompetenceCode> {

    private static final long serialVersionUID = 435340666L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCompetenceCode competenceCode = new QCompetenceCode("competenceCode");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final StringPath code = createString("code");

    public final StringPath codeToPresence = createString("codeToPresence");

    public final ListPath<models.Competence, QCompetence> competence = this.<models.Competence, QCompetence>createList("competence", models.Competence.class, QCompetence.class, PathInits.DIRECT2);

    public final QCompetenceCodeGroup competenceCodeGroup;

    public final StringPath description = createString("description");

    public final BooleanPath disabled = createBoolean("disabled");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final ListPath<models.MonthlyCompetenceType, QMonthlyCompetenceType> holidaysCodes = this.<models.MonthlyCompetenceType, QMonthlyCompetenceType>createList("holidaysCodes", models.MonthlyCompetenceType.class, QMonthlyCompetenceType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<models.enumerate.LimitType> limitType = createEnum("limitType", models.enumerate.LimitType.class);

    public final EnumPath<models.enumerate.LimitUnit> limitUnit = createEnum("limitUnit", models.enumerate.LimitUnit.class);

    public final NumberPath<Integer> limitValue = createNumber("limitValue", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonCompetenceCodes, QPersonCompetenceCodes> personCompetenceCodes = this.<models.PersonCompetenceCodes, QPersonCompetenceCodes>createList("personCompetenceCodes", models.PersonCompetenceCodes.class, QPersonCompetenceCodes.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final ListPath<models.MonthlyCompetenceType, QMonthlyCompetenceType> workdaysCodes = this.<models.MonthlyCompetenceType, QMonthlyCompetenceType>createList("workdaysCodes", models.MonthlyCompetenceType.class, QMonthlyCompetenceType.class, PathInits.DIRECT2);

    public QCompetenceCode(String variable) {
        this(CompetenceCode.class, forVariable(variable), INITS);
    }

    public QCompetenceCode(Path<? extends CompetenceCode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCompetenceCode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCompetenceCode(PathMetadata metadata, PathInits inits) {
        this(CompetenceCode.class, metadata, inits);
    }

    public QCompetenceCode(Class<? extends CompetenceCode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.competenceCodeGroup = inits.isInitialized("competenceCodeGroup") ? new QCompetenceCodeGroup(forProperty("competenceCodeGroup")) : null;
    }

}

