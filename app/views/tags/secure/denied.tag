#{if !(session.username && (controllers.Resecure.check(_arg, _target)))}
    #{doBody /}
#{/if}