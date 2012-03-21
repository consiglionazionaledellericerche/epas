package models;

public enum StampModificationTypeValue {

    FOR_DAILY_LUNCH_TIME(1l),
    FOR_MIN_LUNCH_TIME(2l),
    NOTHING_TO_CHANGE(3l);
    
    private Long id;
    
    private StampModificationTypeValue(Long id) {
            this.id = id;
    }
    
    public Long getId() {
            return id;
    }

}
