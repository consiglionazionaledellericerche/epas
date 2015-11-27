package models.query;

import static com.mysema.query.types.PathMetadataFactory.*;
import models.ContractStampProfile;


import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QContractStampProfile is a Querydsl query type for ContractStampProfile
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QContractStampProfile extends EntityPathBase<ContractStampProfile> {

    private static final long serialVersionUID = 1750135740L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractStampProfile contractStampProfile = new QContractStampProfile("contractStampProfile");

    public final models.base.query.QPropertyInPeriod _super = new models.base.query.QPropertyInPeriod(this);

    //inherited
    public final DatePath<org.joda.time.LocalDate> beginDate = _super.beginDate;

    public final QContract contract;

    //inherited
    public final DatePath<org.joda.time.LocalDate> endDate = _super.endDate;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    public final BooleanPath fixedworkingtime = createBoolean("fixedworkingtime");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public QContractStampProfile(String variable) {
        this(ContractStampProfile.class, forVariable(variable), INITS);
    }

    public QContractStampProfile(Path<? extends ContractStampProfile> path) {
        this(path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractStampProfile(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QContractStampProfile(PathMetadata<?> metadata, PathInits inits) {
        this(ContractStampProfile.class, metadata, inits);
    }

    public QContractStampProfile(Class<? extends ContractStampProfile> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

