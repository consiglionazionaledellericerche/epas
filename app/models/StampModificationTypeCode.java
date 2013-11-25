package models;

import play.db.jpa.JPA;

public enum StampModificationTypeCode {

    FOR_DAILY_LUNCH_TIME("p"),
    FOR_MIN_LUNCH_TIME("e"),
    MARKED_BY_ADMIN("m"),
    TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT("x"),
    ACTUAL_TIME_AT_WORK("f"),
    FIXED_WORKINGTIME("d");
    
    
    private String code;
    
    private StampModificationTypeCode(String code) {
            this.code = code;
    }
    
    public String getCode() {
            return code;
    }

}
