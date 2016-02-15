*{ arg: (href) required
target: optional target for security check
class: optional classes for a element
modal: optional id of modal div
fa: optional fontawesome code
}*
%{
  if ( _target){
    check = session.username && controllers.Resecure.check(_arg.action, _target);
  }
  else {
    check = session.username && controllers.Resecure.checkAction(_arg.action);
  }
}%
#{if check == true }
  <a href="${_arg}"#{if _class} class="${_class}"#{/if}#{if _modal} data-async-modal="${_modal}"#{/if}
  ${helpers.Web.serialize(_attrs, "arg", "class", "fa").raw()}>
  #{if _fa}<i class="fa fa-${_fa}"></i>
  <span class="hidden-xs"> #{/if}
    #{doBody/}
  #{if _fa}</span>#{/if}#{if _title}${_title}#{/if}
  </a>
#{/if}
#{else}
  <span notAllowed data-original-title="&{'link.denied'}">
  #{if _fa}<i class="fa fa-${_fa}"></i>
  <span class="hidden-xs"> #{/if}
   #{doBody/}
  </span>
#{/else}
