/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.enumerate;

/**
 * Tutte le possibili tipologie ferie e permessi legge, compreso
 * il piano di maturazione.
 */
public enum VacationCode {

  CODE_28_4("28+4", 28, 4),
  CODE_27_4("27+4", 27, 4),
  CODE_26_4("26+4", 26, 4),
  CODE_25_4("25+4", 25, 4),
  CODE_21_4("21+4", 21, 4),

  // Part time verticale 2 giorni
  CODE_11_2("11+2", 11, 2),
  CODE_10_2("10+2", 10, 2),
  CODE_10_1("10+1", 10, 1),
  CODE_9_1("9+1", 9, 1),
  
  // Part-time verticali 3 giorni
  CODE_16_2("16+2", 16, 2),
  CODE_17_2("17+2", 17, 2),
  CODE_15_2("15+2", 15, 2),
  
  // Part time verticali 50% o periodici 50%
  CODE_13_2("13+2", 13, 2),
  CODE_14_2("14+2", 14, 2),

  //Part time 70%
  CODE_20_3("20+3", 20, 3),

  // Part time verticale 4 giorni
  CODE_22_3("22+3", 22, 3),
  CODE_21_3("21+3", 21, 3),
  
  //Part time ciclico verticale 83,3%
  CODE_23_3("23+3", 23, 3),

  
  /* Vecchie progressioni obsolete (ma attuate in qualche caso) */
  CODE_30_4("30+4", 30, 4),
  CODE_32_4("32+4", 32, 4);

  public final String name;
  public final int vacations;
  public final int permissions;

  VacationCode(String name, int vacations, int permissions) {
    this.name = name;
    this.vacations = vacations;
    this.permissions = permissions;
  }

  /**
   * Conversione giorni passati / ferie maturate.
   *
   * @param days giorni passati
   * @return ferie maturate.
   */
  public int accruedVacations(int days) {

    if (this.vacations == 9) {
      return accruedProgression9(days);
    }
    if (this.vacations == 10) {
      return accruedProgression10(days);
    }
    if (this.vacations == 11) {
      return accruedProgression11(days);
    }
    if (this.vacations == 13) {
      return accruedProgression13(days);
    }
    if (this.vacations == 14) {
      return accruedProgression14(days);
    }
    if (this.vacations == 15) {
      return accruedProgression15(days);
    }
    if (this.vacations == 16) {
      return accruedProgression16(days);
    }
    if (this.vacations == 17) {
      return accruedProgression17(days);
    }
    if (this.vacations == 28) {
      return accruedProgression28(days);
    }
    if (this.vacations == 26) {
      return accruedProgression26(days);
    }
    if (this.vacations == 25) {
      return accruedProgression25(days);
    }
    if (this.vacations == 27) {
      return accruedProgression27(days);
    }
    if (this.vacations == 22) {
      return accruedProgression22(days);
    }
    if (this.vacations == 21) {
      return accruedProgression21(days);
    }
    if (this.vacations == 20) {
      return accruedProgression20(days);
    }
    if (this.vacations == 30) {
      return accruedProgression30(days);
    }
    if (this.vacations == 32) {
      return accruedProgression32(days);
    }
    if (this.vacations == 23) {
      return accruedProgression23(days);
    }
    return 0;
  }

  /**
   * Conversione giorni passati / permessi maturati.
   *
   * @param days giorni passati
   * @return permessi maturati.
   */
  public int accruedPermissions(int days) {
    if (this.permissions == 4) {
      return accruedProgression4(days);
    }
    if (this.permissions == 3) {
      return accruedProgression3(days);
    }
    if (this.permissions == 2) {
      return accruedProgression2(days);
    }
    if (this.permissions == 1) {
      return accruedProgression1(days);
    }
    return 0;
  }
  
  /**
   * Progressione su 26 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression25(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 4;
    }
    if (days >= 76 && days <= 106) {
      return 6;
    }
    if (days >= 107 && days <= 136) {
      return 8;
    }
    if (days >= 137 && days <= 167) {
      return 10;
    }
    if (days >= 168 && days <= 197) {
      return 12;
    }
    if (days >= 198 && days <= 227) {
      return 14;
    }
    if (days >= 228 && days <= 258) {
      return 16;
    }
    if (days >= 259 && days <= 288) {
      return 18;
    }
    if (days >= 289 && days <= 319) {
      return 20;
    }
    if (days >= 320 && days <= 349) {
      return 22;
    } else {
      return 25;
    }
  }


  /**
   * Progressione su 26 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression26(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 4;
    }
    if (days >= 76 && days <= 106) {
      return 6;
    }
    if (days >= 107 && days <= 136) {
      return 8;
    }
    if (days >= 137 && days <= 167) {
      return 10;
    }
    if (days >= 168 && days <= 197) {
      return 13;
    }
    if (days >= 198 && days <= 227) {
      return 15;
    }
    if (days >= 228 && days <= 258) {
      return 17;
    }
    if (days >= 259 && days <= 288) {
      return 19;
    }
    if (days >= 289 && days <= 319) {
      return 21;
    }
    if (days >= 320 && days <= 349) {
      return 23;
    } else {
      return 26;
    }
  }

  /**
   * Progressione su 28 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression27(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 4;
    }
    if (days >= 76 && days <= 106) {
      return 6;
    }
    if (days >= 107 && days <= 136) {
      return 9;
    }
    if (days >= 137 && days <= 167) {
      return 11;
    }
    if (days >= 168 && days <= 197) {
      return 14;
    }
    if (days >= 198 && days <= 227) {
      return 16;
    }
    if (days >= 228 && days <= 258) {
      return 18;
    }
    if (days >= 259 && days <= 288) {
      return 20;
    }
    if (days >= 289 && days <= 319) {
      return 22;
    }
    if (days >= 320 && days <= 349) {
      return 24;
    } else {
      return 27;
    }
  }
  
  
  /**
   * Progressione su 28 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression28(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 4;
    }
    if (days >= 76 && days <= 106) {
      return 7;
    }
    if (days >= 107 && days <= 136) {
      return 9;
    }
    if (days >= 137 && days <= 167) {
      return 11;
    }
    if (days >= 168 && days <= 197) {
      return 14;
    }
    if (days >= 198 && days <= 227) {
      return 16;
    }
    if (days >= 228 && days <= 258) {
      return 18;
    }
    if (days >= 259 && days <= 288) {
      return 21;
    }
    if (days >= 289 && days <= 319) {
      return 23;
    }
    if (days >= 320 && days <= 349) {
      return 25;
    } else {
      return 28;
    }
  }

  /**
   * Progressione su 30 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression30(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 5;
    }
    if (days >= 76 && days <= 106) {
      return 7;
    }
    if (days >= 107 && days <= 136) {
      return 10;
    }
    if (days >= 137 && days <= 167) {
      return 12;
    }
    if (days >= 168 && days <= 197) {
      return 15;
    }
    if (days >= 198 && days <= 227) {
      return 17;
    }
    if (days >= 228 && days <= 258) {
      return 20;
    }
    if (days >= 259 && days <= 288) {
      return 22;
    }
    if (days >= 289 && days <= 319) {
      return 25;
    }
    if (days >= 320 && days <= 349) {
      return 27;
    } else {
      return 30;
    }
  }

  /**
   * Progressione su 32 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression32(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 5;
    }
    if (days >= 76 && days <= 106) {
      return 8;
    }
    if (days >= 107 && days <= 136) {
      return 10;
    }
    if (days >= 137 && days <= 167) {
      return 13;
    }
    if (days >= 168 && days <= 197) {
      return 16;
    }
    if (days >= 198 && days <= 227) {
      return 18;
    }
    if (days >= 228 && days <= 258) {
      return 21;
    }
    if (days >= 259 && days <= 288) {
      return 24;
    }
    if (days >= 289 && days <= 319) {
      return 26;
    }
    if (days >= 320 && days <= 349) {
      return 29;
    } else {
      return 32;
    }
  }

  /**
   * Progressione su 20 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression20(int days) {

    if (days <= 0) {
      return 0;
    }

    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 3;
    }
    if (days >= 76 && days <= 106) {
      return 5;
    }
    if (days >= 107 && days <= 136) {
      return 6;
    }
    if (days >= 137 && days <= 167) {
      return 8;
    }
    if (days >= 168 && days <= 197) {
      return 10;
    }
    if (days >= 198 && days <= 227) {
      return 12;
    }
    if (days >= 228 && days <= 258) {
      return 14;
    }
    if (days >= 259 && days <= 288) {
      return 15;
    }
    if (days >= 289 && days <= 319) {
      return 17;
    }
    if (days >= 320 && days <= 349) {
      return 18;
    } else {
      return 20;
    }
  }

  /**
   * Progressione su 21 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression21(int days) {

    if (days <= 0) {
      return 0;
    }

    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 3;
    }
    if (days >= 76 && days <= 106) {
      return 5;
    }
    if (days >= 107 && days <= 136) {
      return 6;
    }
    if (days >= 137 && days <= 167) {
      return 8;
    }
    if (days >= 168 && days <= 197) {
      return 10;
    }
    if (days >= 198 && days <= 227) {
      return 12;
    }
    if (days >= 228 && days <= 258) {
      return 14;
    }
    if (days >= 259 && days <= 288) {
      return 15;
    }
    if (days >= 289 && days <= 319) {
      return 17;
    }
    if (days >= 320 && days <= 349) {
      return 18;
    } else {
      return 21;
    }
  }

  /**
   * Progressione su 22 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression22(int days) {
    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 3;
    }
    if (days >= 76 && days <= 106) {
      return 6;
    }
    if (days >= 107 && days <= 136) {
      return 7;
    }
    if (days >= 137 && days <= 167) {
      return 9;
    }
    if (days >= 168 && days <= 197) {
      return 11;
    }
    if (days >= 198 && days <= 227) {
      return 13;
    }
    if (days >= 228 && days <= 258) {
      return 14;
    }
    if (days >= 259 && days <= 288) {
      return 17;
    }
    if (days >= 289 && days <= 319) {
      return 18;
    }
    if (days >= 320 && days <= 349) {
      return 20;
    } else {
      return 22;
    }
  }

  /**
   * Progressione su 16 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression16(int days) {

    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 75) {
      return 2;
    }
    if (days >= 76 && days <= 106) {
      return 4;
    }
    if (days >= 107 && days <= 136) {
      return 5;
    }
    if (days >= 137 && days <= 167) {
      return 6;
    }
    if (days >= 168 && days <= 197) {
      return 8;
    }
    if (days >= 198 && days <= 227) {
      return 9;
    }
    if (days >= 228 && days <= 258) {
      return 10;
    }
    if (days >= 259 && days <= 288) {
      return 11;
    }
    if (days >= 289 && days <= 319) {
      return 13;
    }
    if (days >= 320 && days <= 349) {
      return 14;
    }
    return 16;
  }
  
  /**
   * Progressione su 15 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression15(int days) {

    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 75) {
      return 2;
    }
    if (days >= 76 && days <= 106) {
      return 4;
    }
    if (days >= 107 && days <= 136) {
      return 5;
    }
    if (days >= 137 && days <= 167) {
      return 6;
    }
    if (days >= 168 && days <= 197) {
      return 8;
    }
    if (days >= 198 && days <= 227) {
      return 9;
    }
    if (days >= 228 && days <= 258) {
      return 10;
    }
    if (days >= 259 && days <= 288) {
      return 11;
    }
    if (days >= 289 && days <= 319) {
      return 13;
    }
    if (days >= 320 && days <= 349) {
      return 14;
    }
    return 15;
  }

  /**
   * Progressione su 17 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression17(int days) {

    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 75) {
      return 2;
    }
    if (days >= 76 && days <= 106) {
      return 4;
    }
    if (days >= 107 && days <= 136) {
      return 5;
    }
    if (days >= 137 && days <= 167) {
      return 7;
    }
    if (days >= 168 && days <= 197) {
      return 8;
    }
    if (days >= 198 && days <= 227) {
      return 10;
    }
    if (days >= 228 && days <= 258) {
      return 11;
    }
    if (days >= 259 && days <= 288) {
      return 13;
    }
    if (days >= 289 && days <= 319) {
      return 14;
    }
    if (days >= 320 && days <= 349) {
      return 15;
    }
    return 17;
  }
  
  /**
   * Progressione su 10 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression10(int days) {
    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 106) {
      return 2;
    }
    if (days >= 107 && days <= 136) {
      return 3;
    }
    if (days >= 137 && days <= 167) {
      return 4;
    }
    if (days >= 168 && days <= 197) {
      return 5;
    }
    if (days >= 198 && days <= 227) {
      return 6;
    }
    if (days >= 228 && days <= 258) {
      return 7;
    }
    if (days >= 259 && days <= 319) {
      return 8;
    }
    if (days >= 320 && days <= 349) {
      return 9;
    }
    return 10;
  }
  
  /**
   * Progressione su 11 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression11(int days) {
    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 75) {
      return 2;
    }
    if (days >= 76 && days <= 106) {
      return 3;
    }
    if (days >= 107 && days <= 167) {
      return 4;
    }
    if (days >= 168 && days <= 227) {
      return 6;
    }
    if (days >= 228 && days <= 258) {
      return 7;
    }
    if (days >= 259 && days <= 288) {
      return 8;
    }
    if (days >= 289 && days <= 319) {
      return 9;
    }
    if (days >= 320 && days <= 349) {
      return 10;
    }
    return 11;
  }
  
  /**
   * Progressione su 9 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression9(int days) {
    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 1;
    }
    if (days >= 46 && days <= 106) {
      return 2;
    }
    if (days >= 107 && days <= 136) {
      return 3;
    }
    if (days >= 137 && days <= 167) {
      return 4;
    }
    if (days >= 168 && days <= 197) {
      return 5;
    }
    if (days >= 198 && days <= 227) {
      return 6;
    }
    if (days >= 228 && days <= 258) {
      return 7;
    }
    if (days >= 259 && days <= 319) {
      return 8;
    }
    return 9;
  }
  
  /**
   * Progressione su 13 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression13(int days) {

    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 106) {
      return 4;
    }
    if (days >= 107 && days <= 167) {
      return 6;
    }
    if (days >= 168 && days <= 227) {
      return 8;
    }
    if (days >= 228 && days <= 288) {
      return 10;
    }
    if (days >= 289 && days <= 365) {
      return 13;
    } else {
      return 13;
    }
    
  }
  
  /**
   * Progressione su 23 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression23(int days) {

    if (days <= 0) {
      return 0;
    }
    if (days >= 1 && days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 75) {
      return 4;
    }
    if (days >= 76 && days <= 106) {
      return 7;
    }
    if (days >= 107 && days <= 136) {
      return 9;
    }
    if (days >= 137 && days <= 167) {
      return 11;
    }
    if (days >= 168 && days <= 197) {
      return 14;
    }
    if (days >= 198 && days <= 227) {
      return 16;
    }
    if (days >= 228 && days <= 258) {
      return 18;
    }
    if (days >= 259 && days <= 288) {
      return 21;
    }
    if (days >= 289 && days <= 319) {
      return 23;
    } else {
      return 23;
    }
  }

  
  /**
   * Progressione su 14 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression14(int days) {

    if (days <= 15) {
      return 0;
    }
    if (days >= 16 && days <= 45) {
      return 2;
    }
    if (days >= 46 && days <= 106) {
      return 4;
    }
    if (days >= 107 && days <= 167) {
      return 7;
    }
    if (days >= 168 && days <= 227) {
      return 9;
    }
    if (days >= 228 && days <= 288) {
      return 11;
    }
    if (days >= 289 && days <= 365) {
      return 14;
    } else {
      return 14;
    }
  }

  /**
   * Progressione su 4 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression4(int days) {
    int permissionDays = 0;
    if (days >= 45 && days <= 135) {
      permissionDays = 1;
    }
    if (days >= 136 && days <= 225) {
      permissionDays = 2;
    }
    if (days >= 226 && days <= 315) {
      permissionDays = 3;
    }
    if (days >= 316 && days <= 366) {
      permissionDays = 4;
    }
    return permissionDays;
  }

  /**
   * Progressione su 3 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression3(int days) {
    int permissionDays = 0;
    if (days >= 45 && days <= 135) {
      permissionDays = 1;
    }
    if (days >= 136 && days <= 315) {
      permissionDays = 2;
    }
    if (days >= 316 && days <= 366) {
      permissionDays = 3;
    }
    return permissionDays;
  }

  /**
   * Progressione su 2 giorni.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression2(int days) {

    if (days >= 45 && days <= 225) {
      return 1;
    }
    if (days >= 226) {
      return 2;
    }
    return 0;
  }
  
  /**
   * Progressione su 1 giorno.
   *
   * @param days giorni passati
   * @return giorni maturati
   */
  private int accruedProgression1(int days) {

    if (days <= 225) {
      return 0;
    }

    return 1;
  }

}
