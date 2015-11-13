*{ caso action con target }*

#{if _target}
  #{if session.username && controllers.Resecure.check(_arg, _target)}
    #{doBody /}
  #{/if}
#{/if}

*{ caso action generica }*

#{else}
  #{if session.username && controllers.Resecure.checkAction(_arg)}
    #{doBody /}
  #{/if}
#{/else}
