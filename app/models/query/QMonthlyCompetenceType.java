package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.MonthlyCompetenceType;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMonthlyCompetenceType is a Querydsl query type for MonthlyCompetenceType
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QMonthlyCompetenceType extends EntityPathBase<MonthlyCompetenceType> {

    private static final long serialVersionUID = 230983532L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMonthlyCompetenceType monthlyCompetenceType = new QMonthlyCompetenceType("monthlyCompetenceType");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final QCompetenceCode holidaysCode;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.PersonReperibilityType, QPersonReperibilityType> personReperibilityTypes = this.<models.PersonReperibilityType, QPersonReperibilityType>createList("personReperibilityTypes", models.PersonReperibilityType.class, QPersonReperibilityType.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final QCompetenceCode workdaysCode;

    public QMonthlyCompetenceType(String variable) {
        this(MonthlyCompetenceType.class, forVariable(variable), INITS);
    }

    public QMonthlyCompetenceType(Path<? extends MonthlyCompetenceType> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMonthlyCompetenceType(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMonthlyCompetenceType(PathMetadata metadata, PathInits inits) {
        this(MonthlyCompetenceType.class, metadata, inits);
    }

    public QMonthlyCompetenceType(Class<? extends MonthlyCompetenceType> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.holidaysCode = inits.isInitialized("holidaysCode") ? new QCompetenceCode(forProperty("holidaysCode"), inits.get("holidaysCode")) : null;
        this.workdaysCode = inits.isInitialized("workdaysCode") ? new QCompetenceCode(forProperty("workdaysCode"), inits.get("workdaysCode")) : null;
    }

}

