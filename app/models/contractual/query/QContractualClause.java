package models.contractual.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.contractual.ContractualClause;


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

    private static final long serialVersionUID = 1014177433L;

    public static final QContractualClause contractualClause = new QContractualClause("contractualClause");

    public final models.base.query.QPeriodModel _super = new models.base.query.QPeriodModel(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final SetPath<models.absences.CategoryGroupAbsenceType, models.absences.query.QCategoryGroupAbsenceType> categoryGroupAbsenceTypes = this.<models.absences.CategoryGroupAbsenceType, models.absences.query.QCategoryGroupAbsenceType>createSet("categoryGroupAbsenceTypes", models.absences.CategoryGroupAbsenceType.class, models.absences.query.QCategoryGroupAbsenceType.class, PathInits.DIRECT2);

    public final EnumPath<models.enumerate.ContractualClauseContext> context = createEnum("context", models.enumerate.ContractualClauseContext.class);

    public final SetPath<models.contractual.ContractualReference, QContractualReference> contractualReferences = this.<models.contractual.ContractualReference, QContractualReference>createSet("contractualReferences", models.contractual.ContractualReference.class, QContractualReference.class, PathInits.DIRECT2);

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final StringPath fruitionTime = createString("fruitionTime");

    public final StringPath howToRequest = createString("howToRequest");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath legalAndEconomic = createString("legalAndEconomic");

    public final StringPath name = createString("name");

    public final StringPath otherInfos = createString("otherInfos");

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final StringPath supportingDocumentation = createString("supportingDocumentation");

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

