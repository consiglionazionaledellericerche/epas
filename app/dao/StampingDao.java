package dao;

import javax.persistence.EntityManager;

import models.StampModificationType;
import models.StampModificationTypeCode;
import models.StampType;
import models.Stamping;
import models.query.QStampModificationType;
import models.query.QStampType;
import models.query.QStamping;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class StampingDao extends DaoBase {

	@Inject
	StampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		super(queryFactory, emp);
	}

	/**
	 * 
	 * @param id
	 * @return la timbratura corrispondente all'id passato come parametro
	 */
	public Stamping getStampingById(Long id){
		final QStamping stamping = QStamping.stamping;
		final JPQLQuery query = getQueryFactory().from(stamping)
				.where(stamping.id.eq(id));
		return query.singleResult(stamping);
	}
	
	/**
	 * //FIXME questo metodo è usato in un binder e ciò non è una buona cosa!!
	 * @param description
	 * @return lo stampType corrispondente alla descrizione passata come parametro
	 */
	@Deprecated
	public StampType getStampTypeByCode(String code){
		QStampType stampType = QStampType.stampType;
		final JPQLQuery query = getQueryFactory().from(stampType)
				.where(stampType.code.eq(code));
		return query.singleResult(stampType);
	}

	/**
	 * 
	 * @param id
	 * @return lo stampModificationType relativo all'id passato come parametro
	 */
	@Deprecated
	public StampModificationType getStampModificationTypeById(Long id){
		final QStampModificationType smt = QStampModificationType.stampModificationType;
		
		JPQLQuery query = getQueryFactory().from(smt)
				.where(smt.id.eq(id));
		return query.singleResult(smt);
	}
		
	/**
	 * 
	 * @param codeId
	 * @return lo stampModificationType relativo al codice code passato come parametro
	 */
	public StampModificationType getStampModificationTypeByCode(StampModificationTypeCode smtCode){
		
		Preconditions.checkNotNull(smtCode);
		
		final QStampModificationType smt = QStampModificationType.stampModificationType;
		
		JPQLQuery query = getQueryFactory().from(smt)
				.where(smt.code.eq(smtCode.getCode()));
		
		return query.singleResult(smt);
	}
}
