import helpers.attestati.Dipendente;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
 


import controllers.UploadSituation;
import play.test.UnitTest;

public class SetsCompareTest {

	private Set<Dipendente> dipFromCNR = Sets.newLinkedHashSet();
	private Set<Dipendente> dipFromEPAS = Sets.newLinkedHashSet();
	
    private Dipendente d1 = new Dipendente("1116", "ABBA LAURA");
    private Dipendente d2 = new Dipendente("7792", "ALBERTARIO LUCA");

    private Dipendente e1 = new Dipendente("1116", "Abba Laura");
    private Dipendente e2 = new Dipendente("11428", "Ancillotti Emilio");

	@Before
	public void beforeClass() {
        dipFromCNR.add(d1);
        dipFromCNR.add(d2);

        dipFromEPAS.add(e1);
        dipFromEPAS.add(e2);
	}
	
    @Test
    public void testDifference() { 
        Assert.assertTrue(Sets.difference(dipFromCNR, dipFromEPAS).contains(d2));
        Assert.assertFalse(String.format("La persona %s è contenuta in entrambi", d1), Sets.difference(dipFromCNR, dipFromEPAS).contains(d1));
    }

    @Test
    public void testIntersection() {
    	Assert.assertEquals(1, Sets.intersection(dipFromCNR, dipFromEPAS).size());
    	Assert.assertTrue(Sets.intersection(dipFromCNR, dipFromEPAS).contains(d1));
    }
}
