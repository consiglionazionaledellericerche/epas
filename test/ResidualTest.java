import play.test.UnitTest;

public class ResidualTest extends UnitTest {

  /*
  @Inject
  public ConsistencyManager consistencyManager;

  @Inject
  public ContractYearRecapManager contractYearRecapManager;

  @Inject
  public PersonResidualYearRecapFactory yearFactory;

  @Inject
  public PersonManager personManager;

  @Inject
  public VacationsRecapFactory vacationsFactory;

    @Test
    public void residualLucchesi() throws EmailException {
      LocalDate dateToTest = new LocalDate(2014,2,28);
      int month = 2;
      int year = 2014;
      Person person = Person.find("bySurname", "Lucchesi").first();
      assertEquals(Double.valueOf(146), Double.valueOf(person.id));

      //Ricalcolo tutti i personday

       consistencyManager.fixPersonSituation(
         Optional.of(person),Optional.of(person.user), new LocalDate(2013, 1,1), false);

       JPAPlugin.startTx(false);

      //Ricalcolo tutti i contract year recap
      List<Contract> monthContracts = personManager.getMonthContracts(person,month, year);
      for(Contract contract : monthContracts)
    {
        contractYearRecapManager.buildContractYearRecap(contract);
    }
      assertEquals(monthContracts.size(),1);

      //Costruisco la situazione residuale al 28 febbraio (già concluso)
    List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
    for(Contract contract : monthContracts)
    {
      PersonResidualYearRecap c =
          yearFactory.create(contract, year, dateToTest);
      if(c.getMese(month)!=null)
        contractMonths.add(c.getMese(month));
    }

    //Costruisco la situazione ferie al 28 febbraio (già concluso)
    List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
    for(Contract contract : monthContracts)
    {
      Optional<VacationsRecap> vr = vacationsFactory.create(2014, contract, dateToTest, true);
      contractVacationRecap.add(vr.get());
    }
    JPAPlugin.closeTx(false);


    assertEquals(contractMonths.size(),1);
    assertEquals(contractVacationRecap.size(),1);

    //asserzioni sui residui
      PersonResidualMonthRecap february = contractMonths.get(0);
      assertEquals(february.monteOreAnnoPassato, 0);
      assertEquals(february.monteOreAnnoCorrente, 1445);

      VacationsRecap februaryVacation = contractVacationRecap.get(0);
      //asserzioni sui vacation recap
      //maturate(tutte) meno usate 27 - 1
      assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(25));
      //totali meno usate 28-0
      assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28));
      assertEquals(februaryVacation.permissionUsed.size(), 2);
      assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(2));

    }

    @Test
    public void residualSanterini() throws EmailException {
      LocalDate dateToTest = new LocalDate(2014,2,28);
      int month = 2;
      int year = 2014;

      JPAPlugin.startTx(false);
      Person person = Person.find("bySurnameAndName", "Santerini", "Paolo").first();
      assertEquals(Double.valueOf(32), Double.valueOf(person.id));


      //Ricalcolo tutti i personday

       consistencyManager.fixPersonSituation(
         Optional.of(person),Optional.of(person.user), new LocalDate(2013, 1,1), false);

      JPAPlugin.startTx(false);

      //Ricalcolo tutti i contract year recap
      List<Contract> monthContracts = personManager.getMonthContracts(person, month, year);
      for(Contract contract : monthContracts)
    {
        contractYearRecapManager.buildContractYearRecap(contract);
    }
      assertEquals(monthContracts.size(),1);

      //Costruisco la situazione residuale al 28 febbraio (già concluso)
    List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
    for(Contract contract : monthContracts)
    {
      PersonResidualYearRecap c =
          yearFactory.create(contract, year, dateToTest);
      if(c.getMese(month)!=null)
        contractMonths.add(c.getMese(month));
    }

    //Costruisco la situazione ferie al 28 febbraio (già concluso)
    List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
    for(Contract contract : monthContracts)
    {
      Optional<VacationsRecap> vr = vacationsFactory.create(2014, contract, dateToTest, true);
      contractVacationRecap.add(vr.get());
    }
    JPAPlugin.closeTx(false);


    assertEquals(contractMonths.size(),1);
    assertEquals(contractVacationRecap.size(),1);

    //asserzioni sui residui
      PersonResidualMonthRecap february = contractMonths.get(0);
      assertEquals(february.monteOreAnnoPassato, 3207);
      assertEquals(february.monteOreAnnoCorrente, 2453);

      VacationsRecap februaryVacation = contractVacationRecap.get(0);
      //asserzioni sui vacation recap
      //maturate(tutte) meno usate
      assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(28));
      //totali meno usate
      assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28));
      assertEquals(februaryVacation.permissionUsed.size(), 0);
      assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }

    @Test
    public void residualMartinelli() throws EmailException {
      LocalDate dateToTest = new LocalDate(2014,2,28);
      int month = 2;
      int year = 2014;

      JPAPlugin.startTx(false);
      Person person = Person.find("bySurnameAndName", "Martinelli", "Maurizio").first();
      assertEquals(Double.valueOf(25), Double.valueOf(person.id));


      //Ricalcolo tutti i personday

       consistencyManager.fixPersonSituation(
         Optional.of(person),Optional.of(person.user), new LocalDate(2013, 1,1), false);

      JPAPlugin.startTx(false);

      //Ricalcolo tutti i contract year recap
      List<Contract> monthContracts = personManager.getMonthContracts(person, month, year);
      for(Contract contract : monthContracts)
    {
        contractYearRecapManager.buildContractYearRecap(contract);
    }
      assertEquals(monthContracts.size(),1);

      //Costruisco la situazione residuale al 28 febbraio (già concluso)
    List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
    for(Contract contract : monthContracts)
    {
      PersonResidualYearRecap c =
          yearFactory.create(contract, year, dateToTest);
      if(c.getMese(month)!=null)
        contractMonths.add(c.getMese(month));
    }

    //Costruisco la situazione ferie al 28 febbraio (già concluso)
    List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
    for(Contract contract : monthContracts)
    {
      Optional<VacationsRecap> vr = vacationsFactory.create(2014, contract, dateToTest, true);
      contractVacationRecap.add(vr.get());
    }
    JPAPlugin.closeTx(false);


    assertEquals(contractMonths.size(),1);
    assertEquals(contractVacationRecap.size(),1);

    //asserzioni sui residui
      PersonResidualMonthRecap february = contractMonths.get(0);
      assertEquals(february.monteOreAnnoPassato, 29196);
      assertEquals(february.monteOreAnnoCorrente, 2166);

      VacationsRecap februaryVacation = contractVacationRecap.get(0);
      //asserzioni sui vacation recap
      //maturate(tutte) meno usate
      assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(25));
      //totali meno usate
      assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28));
      assertEquals(februaryVacation.permissionUsed.size(), 0);
      assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }

    @Test
    public void residualSuccurro() throws EmailException {
      LocalDate dateToTest = new LocalDate(2014,2,28);
      int month = 2;
      int year = 2014;

      JPAPlugin.startTx(false);
      Person person = Person.find("bySurname", "Succurro").first();
      assertEquals(Double.valueOf(224), Double.valueOf(person.id));


      //Ricalcolo tutti i personday

       consistencyManager.fixPersonSituation(
         Optional.of(person),Optional.of(person.user), new LocalDate(2013, 1,1), false);

      JPAPlugin.startTx(false);

      //Ricalcolo tutti i contract year recap
      List<Contract> monthContracts = personManager.getMonthContracts(person, month, year);
      for(Contract contract : monthContracts)
    {
        contractYearRecapManager.buildContractYearRecap(contract);
    }
      assertEquals(monthContracts.size(),1);

      //Costruisco la situazione residuale al 28 febbraio (già concluso)
    List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
    for(Contract contract : monthContracts)
    {
      PersonResidualYearRecap c =
          yearFactory.create(contract, year, dateToTest);
      if(c.getMese(month)!=null)
        contractMonths.add(c.getMese(month));
    }

    //Costruisco la situazione ferie al 28 febbraio (già concluso)
    List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
    for(Contract contract : monthContracts)
    {
      Optional<VacationsRecap> vr = vacationsFactory.create(2014, contract, dateToTest, true);
      contractVacationRecap.add(vr.get());
    }
    JPAPlugin.closeTx(false);


    assertEquals(contractMonths.size(),1);
    assertEquals(contractVacationRecap.size(),1);

    //asserzioni sui residui
      PersonResidualMonthRecap february = contractMonths.get(0);
      assertEquals(february.monteOreAnnoPassato, 32991);
      assertEquals(february.monteOreAnnoCorrente, 6487);

      VacationsRecap februaryVacation = contractVacationRecap.get(0);
      //asserzioni sui vacation recap
      //maturate(tutte) meno usate
      assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(24));
      //totali meno usate
      assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28));
      assertEquals(februaryVacation.permissionUsed.size(), 0);
      assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }

    @Test
    public void residualAbba() throws EmailException {
      LocalDate dateToTest = new LocalDate(2014,2,28);
      int month = 2;
      int year = 2014;

      JPAPlugin.startTx(false);
      Person person = Person.find("bySurname", "Abba").first();
      assertEquals(Double.valueOf(2), Double.valueOf(person.id));


      //Ricalcolo tutti i personday

       consistencyManager.fixPersonSituation(
         Optional.of(person),Optional.of(person.user), new LocalDate(2013, 1,1), false);

       JPAPlugin.startTx(false);

      //Ricalcolo tutti i contract year recap
      List<Contract> monthContracts = personManager.getMonthContracts(person,month, year);
      for(Contract contract : monthContracts)
    {
        contractYearRecapManager.buildContractYearRecap(contract);
    }
      assertEquals(monthContracts.size(),1);

      //Costruisco la situazione residuale al 28 febbraio (già concluso)
    List<PersonResidualMonthRecap> contractMonths = new ArrayList<PersonResidualMonthRecap>();
    for(Contract contract : monthContracts)
    {
      PersonResidualYearRecap c =
          yearFactory.create(contract, year, dateToTest);
      if(c.getMese(month)!=null)
        contractMonths.add(c.getMese(month));
    }

    //Costruisco la situazione ferie al 28 febbraio (già concluso)
    List<VacationsRecap> contractVacationRecap = new ArrayList<VacationsRecap>();
    for(Contract contract : monthContracts)
    {
      Optional<VacationsRecap> vr = vacationsFactory.create(2014, contract, dateToTest, true);
      contractVacationRecap.add(vr.get());
    }
    JPAPlugin.closeTx(false);


    assertEquals(contractMonths.size(),1);
    assertEquals(contractVacationRecap.size(),1);

    //asserzioni sui residui
      PersonResidualMonthRecap february = contractMonths.get(0);
      assertEquals(february.monteOreAnnoPassato, 0);
      assertEquals(february.monteOreAnnoCorrente, 0);

      VacationsRecap februaryVacation = contractVacationRecap.get(0);
      //asserzioni sui vacation recap
      //maturate(tutte) meno usate
      assertEquals(februaryVacation.vacationDaysLastYearNotYetUsed, new Integer(19));
      //totali meno usate
      assertEquals(februaryVacation.vacationDaysCurrentYearNotYetUsed, new Integer(28));
      assertEquals(februaryVacation.permissionUsed.size(), 0);
      assertEquals(februaryVacation.persmissionNotYetUsed, new Integer(4));

    }
   */

}
