package manager;

import models.Contract;
import models.ContractStampProfile;
import org.joda.time.LocalDate;

import javax.inject.Inject;

public class ContractStampProfileManager {

	@Inject
	private ContractManager contractManager;

	/**
	 * 
	 * @param contract
	 * @param splitDate
	 */
	public void splitContractStampProfile(ContractStampProfile contract, LocalDate splitDate){
		ContractStampProfile csp2 = new ContractStampProfile();
		csp2.contract = contract.contract;
		csp2.startFrom = splitDate;
		csp2.endTo = contract.endTo;
		csp2.fixedworkingtime = contract.fixedworkingtime;
		csp2.save();

		contract.endTo = splitDate.minusDays(1);
		contract.save();
	}

	/**
	 * 
	 * @param contract
	 * @param index
	 * @param csp
	 */
	public void deleteContractStampProfile(Contract contract, int index,ContractStampProfile csp){
		ContractStampProfile previous = contractManager.getContractStampProfileAsList(contract).get(index-1);
		previous.endTo = csp.endTo;
		previous.save();
		csp.delete();

		//Ricalcolo i valori
		//FIXME, trovare un modo per pulire lo heap che ci trovo sempre quello eliminato
		for(ContractStampProfile cspp : contract.contractStampProfile){
			if(cspp.id.equals(csp.id)) {
				contract.contractStampProfile.remove(cspp);
			}
		}
	}
}
