#{if session.username && controllers.Secure.Security.invoke("permissionCheck")}
    #{doBody /}
#{/if}