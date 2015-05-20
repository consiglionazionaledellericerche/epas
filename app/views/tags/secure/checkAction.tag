#{if session.username && controllers.Resecure.checkAction(_arg)}
    #{doBody /}
#{/if}