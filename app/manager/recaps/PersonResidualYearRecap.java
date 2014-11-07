package manager.recaps;

import java.util.List;

/**
 * 
 * @author alessandro
 *
 */
public class PersonResidualYearRecap {

	public List<PersonResidualMonthRecap> mesi;

	public PersonResidualYearRecap() {}
	
	/**
	 * 
	 * @param month
	 * @return
	 */
	public PersonResidualMonthRecap getMese(int month){
		if(this.mesi==null)
			return null;
		for(PersonResidualMonthRecap mese : this.mesi)
			if(mese.mese==month)
				return mese;
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public List<PersonResidualMonthRecap> getMesi() {
		return this.mesi;
	}

}
