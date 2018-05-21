package models.absences.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.absences.ContractualClause;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContractualClause is a Querydsl query type for ContractualClause
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractualClause extends EntityPathBase<ContractualClause> {

    private static final long serialVersionUID = -1252213783L;

    public static final QContractualClause contractualClause = new QContractualClause("contractualClause");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final SetPath<models.absences.CategoryGroupAbsenceType, QCategoryGroupAbsenceType> categoryGroupAbsenceTypes = this.<models.absences.CategoryGroupAbsenceType, QCategoryGroupAbsenceType>createSet("categoryGroupAbsenceTypes", models.absences.CategoryGroupAbsenceType.class, QCategoryGroupAbsenceType.class, PathInits.DIRECT2);

    public final SetPath<models.absences.ContractualReference, QContractualReference> contractualReferences = this.<models.absences.ContractualReference, QContractualReference>createSet("contractualReferences", models.absences.ContractualReference.class, QContractualReference.class, PathInits.DIRECT2);

    public final StringPath description = createString("description");

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QContractualClause(String variable) {
        super(ContractualClause.class, forVariable(variable));
    }

    public QContractualClause(Path<? extends ContractualClause> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContractualClause(PathMetadata<?> metadata) {
        super(ContractualClause.class, metadata);
    }

}

