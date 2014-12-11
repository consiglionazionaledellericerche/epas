package dao;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.Contract;
import models.query.QContract;

public class ContractDao {

	/**
	 * 
	 * @param id
	 * @return il contratto corrispondente all'id passato come parametro
	 */
	public static Contract getContractById(Long id){
		QContract contract = QContract.contract;
		final JPQLQuery query = ModelQuery.queryFactory().from(contract)
				.where(contract.id.eq(id));
		return query.singleResult(contract);
	}
}
