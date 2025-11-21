package models.query;

import static com.querydsl.core.types.PathMetadataFactory.*;
import models.ContractMonthRecap;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractMonthRecap is a Querydsl query type for ContractMonthRecap
 */
@Generated("com.querydsl.codegen.EntitySerializer")
public class QContractMonthRecap extends EntityPathBase<ContractMonthRecap> {

    private static final long serialVersionUID = 2028405493L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractMonthRecap contractMonthRecap = new QContractMonthRecap("contractMonthRecap");

    public final models.base.query.QBaseModel _super = new models.base.query.QBaseModel(this);

    public final NumberPath<Integer> buoniPastoConsegnatiNelMese = createNumber("buoniPastoConsegnatiNelMese", Integer.class);

    public final NumberPath<Integer> buoniPastoDaInizializzazione = createNumber("buoniPastoDaInizializzazione", Integer.class);

    public final NumberPath<Integer> buoniPastoDalMesePrecedente = createNumber("buoniPastoDalMesePrecedente", Integer.class);

    public final NumberPath<Integer> buoniPastoUsatiNelMese = createNumber("buoniPastoUsatiNelMese", Integer.class);

    public final QContract contract;

    //inherited
    public final SimplePath<Object> entityId = _super.entityId;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> initMonteOreAnnoCorrente = createNumber("initMonteOreAnnoCorrente", Integer.class);

    public final NumberPath<Integer> initMonteOreAnnoPassato = createNumber("initMonteOreAnnoPassato", Integer.class);

    public final NumberPath<Integer> initResiduoAnnoCorrenteNelMese = createNumber("initResiduoAnnoCorrenteNelMese", Integer.class);

    public final NumberPath<Integer> month = createNumber("month", Integer.class);

    public final NumberPath<Integer> oreLavorate = createNumber("oreLavorate", Integer.class);

    //inherited
    public final BooleanPath persistent = _super.persistent;

    public final BooleanPath possibileUtilizzareResiduoAnnoPrecedente = createBoolean("possibileUtilizzareResiduoAnnoPrecedente");

    public final NumberPath<Integer> progressivoFinaleMese = createNumber("progressivoFinaleMese", Integer.class);

    public final NumberPath<Integer> progressivoFinaleNegativoMeseImputatoAnnoCorrente = createNumber("progressivoFinaleNegativoMeseImputatoAnnoCorrente", Integer.class);

    public final NumberPath<Integer> progressivoFinaleNegativoMeseImputatoAnnoPassato = createNumber("progressivoFinaleNegativoMeseImputatoAnnoPassato", Integer.class);

    public final NumberPath<Integer> progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese = createNumber("progressivoFinaleNegativoMeseImputatoProgressivoFinalePositivoMese", Integer.class);

    public final NumberPath<Integer> progressivoFinalePositivoMese = createNumber("progressivoFinalePositivoMese", Integer.class);

    public final NumberPath<Integer> progressivoFinalePositivoPerStraordinari = createNumber("progressivoFinalePositivoPerStraordinari", Integer.class);

    public final NumberPath<Integer> recoveryDayUsed = createNumber("recoveryDayUsed", Integer.class);

    public final NumberPath<Integer> remainingMealTickets = createNumber("remainingMealTickets", Integer.class);

    public final NumberPath<Integer> remainingMinutesCurrentYear = createNumber("remainingMinutesCurrentYear", Integer.class);

    public final NumberPath<Integer> remainingMinutesLastYear = createNumber("remainingMinutesLastYear", Integer.class);

    public final NumberPath<Integer> riposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente = createNumber("riposiCompensativiChiusuraEnteMinutiImputatoAnnoCorrente", Integer.class);

    public final NumberPath<Integer> riposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato = createNumber("riposiCompensativiChiusuraEnteMinutiImputatoAnnoPassato", Integer.class);

    public final NumberPath<Integer> riposiCompensativiChiusuraEnteMinutiImputatoProgressivoFinalePositivoMese = createNumber("riposiCompensativiChiusuraEnteMinutiImputatoProgressivoFinalePositivoMese", Integer.class);

    public final NumberPath<Integer> riposiCompensativiChiusuraEnteMinutiPrint = createNumber("riposiCompensativiChiusuraEnteMinutiPrint", Integer.class);

    public final NumberPath<Integer> riposiCompensativiMinutiImputatoAnnoCorrente = createNumber("riposiCompensativiMinutiImputatoAnnoCorrente", Integer.class);

    public final NumberPath<Integer> riposiCompensativiMinutiImputatoAnnoPassato = createNumber("riposiCompensativiMinutiImputatoAnnoPassato", Integer.class);

    public final NumberPath<Integer> riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese = createNumber("riposiCompensativiMinutiImputatoProgressivoFinalePositivoMese", Integer.class);

    public final NumberPath<Integer> riposiCompensativiMinutiPrint = createNumber("riposiCompensativiMinutiPrint", Integer.class);

    public final NumberPath<Integer> straordinariMinutiS1Print = createNumber("straordinariMinutiS1Print", Integer.class);

    public final NumberPath<Integer> straordinariMinutiS2Print = createNumber("straordinariMinutiS2Print", Integer.class);

    public final NumberPath<Integer> straordinariMinutiS3Print = createNumber("straordinariMinutiS3Print", Integer.class);

    //inherited
    public final NumberPath<Integer> version = _super.version;

    public final NumberPath<Integer> year = createNumber("year", Integer.class);

    public QContractMonthRecap(String variable) {
        this(ContractMonthRecap.class, forVariable(variable), INITS);
    }

    public QContractMonthRecap(Path<? extends ContractMonthRecap> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractMonthRecap(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractMonthRecap(PathMetadata metadata, PathInits inits) {
        this(ContractMonthRecap.class, metadata, inits);
    }

    public QContractMonthRecap(Class<? extends ContractMonthRecap> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

