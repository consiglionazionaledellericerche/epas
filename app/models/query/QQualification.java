package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.Qualification;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQualification is a Querydsl query type for Qualification
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QQualification extends EntityPathBase<Qualification> {

    private static final long serialVersionUID = -1883508405L;

    public static final QQualification qualification1 = new QQualification("qualification1");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final ListPath<models.absences.AbsenceType, models.absences.query.QAbsenceType> absenceTypes = this.<models.absences.AbsenceType, models.absences.query.QAbsenceType>createList("absenceTypes", models.absences.AbsenceType.class, models.absences.query.QAbsenceType.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath label = createString("label");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final ListPath<models.Person, QPerson> persons = this.<models.Person, QPerson>createList("persons", models.Person.class, QPerson.class, PathInits.DIRECT2);

    public final NumberPath<Integer> qualification = createNumber("qualification", Integer.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QQualification(String variable) {
        super(Qualification.class, forVariable(variable));
    }

    public QQualification(Path<? extends Qualification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQualification(PathMetadata metadata) {
        super(Qualification.class, metadata);
    }

}

