package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ContractStampProfile;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractStampProfile is a Querydsl query type for ContractStampProfile
 */
@Generated("com.querydsl.codegen.EntitySerializer")
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

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public QContractStampProfile(String variable) {
        this(ContractStampProfile.class, forVariable(variable), INITS);
    }

    public QContractStampProfile(Path<? extends ContractStampProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractStampProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractStampProfile(PathMetadata metadata, PathInits inits) {
        this(ContractStampProfile.class, metadata, inits);
    }

    public QContractStampProfile(Class<? extends ContractStampProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

