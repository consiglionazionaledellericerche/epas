#{if session.username && (_target == null || controllers.Resecure.check(_arg, _target))}
    #{doBody /}
#{/if}
