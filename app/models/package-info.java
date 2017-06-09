@org.hibernate.annotations.TypeDefs({
  @org.hibernate.annotations.TypeDef(name = "YearMonth",
          defaultForType = org.joda.time.YearMonth.class,
          typeClass = org.jadira.usertype.dateandtime.joda.PersistentYearMonthAsString.class)}
)
package models;