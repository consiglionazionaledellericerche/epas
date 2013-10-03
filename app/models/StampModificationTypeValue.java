package models;

import play.db.jpa.JPA;

public enum StampModificationTypeValue {

    FOR_DAILY_LUNCH_TIME(1l),
    FOR_MIN_LUNCH_TIME(2l),
    MARKED_BY_ADMIN(3l),
    TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT(4l),
    ACTUAL_TIME_AT_WORK(5l),
    FIXED_WORKINGTIME(6l);
    
    
    private Long id;
    
    private StampModificationTypeValue(Long id) {
            this.id = id;
    }
    
    public Long getId() {
            return id;
    }
    
    public StampModificationType getStampModificationType(){
    	return JPA.em().getReference(StampModificationType.class, id);
    }

}
