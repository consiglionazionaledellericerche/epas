*{ arg: (href) required
target: optional target for security check
class: optional classes for a element
fa: optional fontawesome code
display: optional (hidden, denied) display the link as denied or display nothing if not permitted
}*
#{if controllers.Resecure.check(_arg.action, _target)}
<a href="${_arg}"#{if _class} class="${_class}"#{/if}
${helpers.Web.serialize(_attrs, "arg", "class", "fa", "target", "hide").raw()}>
#{if _fa}<i class="fa fa-${_fa}"></i><span class="hidden-xs">#{/if}
  #{doBody/}
#{if _fa}</span>#{/if}#{if _title}${_title}#{/if}
</a>
#{/if}
#{else}
#{if _display == 'denied'}<span notAllowed data-original-title="&{'link.denied'}">#{/if}
  #{if _fa}<i class="fa fa-${_fa}"></i><span class="hidden-xs">#{/if}
    #{doBody/}
  #{if _fa}</span>#{/if}#{if _title}${_title}#{/if}
#{/else}