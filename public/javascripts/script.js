/* Author:
 */
$(function($) {
	/* Gestione tab di autenticazione */
    $('#login-idp-link').click(function(e) {
		$("#login-idp").delay(100).fadeIn(100);
 		$("#login-form").fadeOut(100);
		$('#login-form-link').removeClass('active');
		$(this).addClass('active');
		e.preventDefault();
	});
	$('#login-form-link').click(function(e) {
		$("#login-form").delay(100).fadeIn(100);
 		$("#login-idp").fadeOut(100);
		$('#login-idp-link').removeClass('active');
		$(this).addClass('active');
		e.preventDefault();
	});

    //Utilizzata sulle select o i campi input (non datepicker) per fare il
    // submit automatico della form 
	$(document.body).on('change', 'select.auto-submit-parent, input:not([datepicker]).auto-submit-parent',
	function () {
	    var $select = $(this);
		$select.parents("form").submit();
		return true;
	});

	//Gestione del campo note nell'inserimento delle assenze nel passato.
	$(document.body).on('keyup', 'input.absenceRequest_note_input',
	function () {
		var $input = $(this);
		if ($input.val().length > 3) {
			$("#absenceRequest_insertButton").prop('disabled',false);
		} else {
			$("#absenceRequest_insertButton").prop('disabled',true);
		}
		$(".absenceRequest_note_hidden").val($input.val());
		return true;
	});

	//Non viene utilizzata la auto-submit perché non permette
	//di scrivere correttamente la data ma effettua la submit
	//prima di terminare di scrivere la data completa
	$(document.body).on('change', 'input[datepicker].auto-submit-parent', 
	function () {
		var $input = $(this);
		if ($input.val().length == 10) {
			$input.parents("form").submit();
		}
		return true;
	});


	/**
	 * evita i doppi invii sulle submit delle form.
	 */
	$(document.body).on('submit', 'form:not(.js-allow-double-submission,[data-reload])', function (e) {
		var $form = $(this)
		if ($form.data('submitted') === true) {
	      e.preventDefault();
	    } else {
	      $form.data('submitted', true);
	      $(':input', $form).prop('readOnly', true);
	      $(":button[type='submit']").prop('disabled',true);	      
	      //Non funziona nelle modali di inserimento assenze
	      //$('a', $form).disable(true); 
	      $('.btn', $form).addClass('disabled');
	    }
	    return true;
	  });
	  
  /**
   * Opposto di serialize(). Dall'url ai form params.
   */
  $.queryParams = function(url) {
    if (url.indexOf('#') !== -1) {
      url = url.substring(0, url.indexOf('#'));
    }
    return (function(a) {
      if (a == "") return {};
      var b = {};
      for (var i = 0; i < a.length; ++i) {
        var p = a[i].split('=');
        if (p.length != 2) continue;
        b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
      }
      return b;
    })(url.substring(url.indexOf('?') + 1).split('&'));
  };
  /**
   * Author: Alessandro
   * Se si vuole utilizzare il caricamento async da <a> anzichè <form>.
   * Aggiungere all'ancora l'attributo data-async="#divTarget" ed eventualmente il data-spinner.
   * TODO: implementare async-error
   */
  $(document.body).on('click', 'a[data-async]', function(e) {
    var $body = $('body');
    var $flashDiv = $body.find('#flashDiv');
    if ($flashDiv) {
      $flashDiv.remove();
    }
    var $a = $(this);
    var target = $a.data('async');
    var $target = $(target);
    var contentType = "application/x-www-form-urlencoded; charset=UTF-8";
    var url = $a.attr('href')
    var formData = $.queryParams(url);
    var spinner = $a.data('spinner');
    var $spinner = $(spinner);
    $spinner.removeClass('hidden');
    $spinner.addClass('visible');
    url = url.substring(0, url.indexOf('?'));
    method = 'get';
    // TODO: chiamata ajax simile alla versione form. 
    $.ajax({
      type: method,
      url: url,
      data: formData,
      contentType: contentType
    }).done(function(data, status) {
      $target.replaceWith($(target, data));
      $('body').initepas();
    }).fail(function(xhr, status, error) {
      if (xhr.status == 400) {
        var $res = $(errorTarget, xhr.responseText);
        var $etarget = errorTarget ? $(errorTarget) : $form;
        $etarget.html($res.html()).parent().initepas();
      } else {
        //bootbox.alert('Si è verificato un errore: '+ error);
      } // else segnala l'errore in qualche modo.
    }).always(function() {
      $spinner.removeClass('visible');
      $spinner.addClass('hidden');
    });
    e.preventDefault();
  });
  //	$(document.body).on('change', 'form[disable-onchange'), function(e) {
  //		var $form = $(this);
  //		//TODO tutti se sono più di uno
  //    	$form.find('button').attr('disabled', 'disabled');
  //	}
  /**
   * Author: Marco
   * form ajax attivate con l'attributo data-async:
   *   data-async deve contenere il target per le risposte di successo;
   *   data-async-error deve contenere il target per gli errori.
   */
  $(document.body).on('submit', 'form[data-async]', function(e) {
    var $form = $(this);
    var target = $form.data('async');
    var errorTarget = $form.data('async-error');
    var $target = $(target);
    //patch per multipart (blob)
    var contentType = "application/x-www-form-urlencoded; charset=UTF-8";
    var formData = $form.serialize();
    if ($form.attr('enctype') === 'multipart/form-data') {
      contentType = false;
      formData = new FormData($form[0]) //IE9? issue stackoverflow 20795449
    }
    //spinner
    var spinner = $form.data('spinner');
    var $spinner = $(spinner);
    $spinner.removeClass('hidden');
    $spinner.addClass('visible');
    //se presente l'attributo data-related disabilita
    //tutte le form della pagina con lo stesso identificativo
    if ($form.data('related')) {
      $("form").each(function() {
        related = $(this).data('related');
        if (related === $form.data('related')) {
          $button = $(this).find('button');
          $button.attr('disabled', 'disabled');
        }
      });
    }
    //        $form.find(':input').prop("readonly", true);
    //        var bgcolor = $form.css('background-color');
    //        $form.css('backround-color', '#e0e0e0');
    $.ajax({
      type: $form.attr('method'),
      url: $form.attr('action'),
      data: formData,
      contentType: contentType
    }).done(function(data, status) {
      $target.replaceWith($(target, data));
      // TODO: verificare se occorre fare unwrap
      $('body').initepas();
      // disattiva la modale sopra (se c'è).
      $form.parents('.modal').modal('hide');
    }).fail(function(xhr, status, error) {
      if (xhr.status == 400) {
        var $res = $(errorTarget, xhr.responseText);
        var $etarget = errorTarget ? $(errorTarget) : $form;
        $etarget.html($res.html()).parent().initepas();
      } else {
        //bootbox.alert('Si è verificato un errore: '+ error);
      } // else segnala l'errore in qualche modo.
    }).always(function() {
      $spinner.removeClass('visible');
      $spinner.addClass('hidden');
      //        	$form.find(':input').prop('readonly', true);
      //        	$form.css('background-color', bgcolor);
    });
    e.preventDefault();
  });
  /**
   * Per impostare il tag da visualizzare quando ricarica la pagina.
   */
  $(document.body).on('submit', 'form[data-anchor]', function(e) {
    e.preventDefault();
    var $form = $(this);
    var url = $form.attr('action') + $form.data('anchor');
    $form.removeAttr('data-anchor');
    $form.attr('action', url).submit();
  });
  PNotify.prototype.options.styling = "fontawesome";
  /**
   * Author: Marco
   */
  $(document.body).on('click', 'a[data-async-modal]', function(e) {
    var $body = $('body');
    var $flashDiv = $body.find('#flashDiv');
    if ($flashDiv) {
      $flashDiv.remove();
    }
    var $this = $(this);
    var $modal = $($this.data('async-modal'));
    var url = $this.attr('href');
    $body.modalmanager('loading');
    $modal.load(url, '', function() {
      $modal.modal().initepas();
    });
    e.preventDefault();
  });
  
  bootbox.setDefaults({
    locale: 'it',
    className: 'bootbox_modal'
  });

  /**
   * conferma sulle form.
   */
  $(document.body).on('click', 'input[data-confirm], button[data-confirm], a[data-confirm]', function (e) {
    var $button = $(this)
    var confirm = $button.data('confirm')
    if (confirm) {
      bootbox.confirm(confirm, function (result) {
        if (result) {
          $button.data('confirm', '').trigger('click')
        }
      })
      e.preventDefault()
      e.stopPropagation()
    }
  });
  
  function toggleChevron(e) {
    var $fa = $(e.target).prev('.panel-heading').find('i.fa');
    if (e.type == "hide") {
      $fa.addClass('fa-chevron-up').removeClass('fa-chevron-down');
    } else {
      $fa.addClass('fa-chevron-down').removeClass('fa-chevron-up');
    }
  }
  $(document.body).on('hide.bs.collapse', 'section,div', toggleChevron);
  $(document.body).on('show.bs.collapse', 'section,div', toggleChevron);
  
  
  //  <div class="panel panel-info" id="notifications" data-load-async="@{Application.test()}">
  //  <div class="panel-heading">
  //  <i class="fa fa-spin fa-spinner fa-2x"></i> Caricamento test in corso...
  //  </div>
  //</div>  
  $('[data-load-async]', this).each(function() {
    var $this = $(this);
    $this.load($this.data('loadAsync'), function() {
      $this.initepas();
    });
  });
  $.fn.initepas = function() {
	  
   this.find('textarea[wysiwyg!="false"]').trumbowyg({
       lang: 'it',
       autogrow: true,
       btns: [
         ['viewHTML'],
         ['undo', 'redo'], // Only supported in Blink browsers
         ['formatting'],
         ['strong', 'em', 'del'],
         ['superscript', 'subscript'],
         ['link'],
         ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
         ['unorderedList', 'orderedList'],
         ['horizontalRule'],
         ['removeformat'],
         ['fullscreen']    	   
       ]
   });
	  
    $('[data-notify]', this).each(function() {
      var $this = $(this);
      var title = $this.data('notify')
      var text = $this.text();
      var type = $this.data('notify-type');
      new PNotify({
        title: title,
        text: text,
        type: type,
        remove: true
      });
    });
    $(':input[data-switcher]', this).select2({
      width: 'resolve',
      containerCssClass: "switcher",
      dropdownCssClass: "switcher",
    });
    $(':input[select2]', this).select2({
      width: 'resolve',
      allowClear: true,
      theme: "bootstrap",
      placeholder: "Seleziona un valore",
    });
    $(':input[select2Table]', this).select2({
      minimumResultsForSearch: 25
    });
    $('[popover]').popover({
      trigger: "focus",
      placement: 'right auto',
      container: 'body'
    });
    $('[popover-hover]').popover({
      trigger: "hover",
      placement: 'right auto',
      container: 'body'
    });
    $('[popover-hover-2]').popover({ /*per non avere il cambio di sfondo */
      trigger: "hover",
      placement: 'top auto',
      container: 'body'
    });
    /**
     * Esempio utilizzo <div webui-popover-over data-url="#id"
     * https://github.com/sandywalker/webui-popover
     **/
    $('[webui-popover-hover]').webuiPopover({
      placement: 'auto',
      trigger: 'hover',
      type: 'html',
      //style:'inverse',
      animation: 'pop',
      dismissible: true,
      delay: { //show and hide delay time of the popover, works only when trigger is 'hover',the value can be number or object
        show: null,
        hide: null
      }
    });
    //Attributo da inserire all'anchor se si vuole far scomparire il popover dopo il click
    $('a[hide-popover-on-click]').click(function() {
      var $a = $(this);
      //popover direttamente collegato all'anchor
      var target = $a.attr('data-target');
      if (!target) {
        //popover collegato in uno dei figli
        target = $a.children("[data-target]:first").attr('data-target');
        if (!target) {
          //popover collegato a uno dei padri
          target = $a.closest(".webui-popover").attr('id');
        }
      }
      $('[data-target="' + target + '"]').webuiPopover('hide');
    });
    $.fn.dataTable.moment( 'DD/MM/YYYY', 'it' );
    this.find('[datatable]').DataTable({
      "pageLength": 15,
      "lengthMenu": [
        [10, 15, 20, 25, 50, 100, -1],
        [10, 15, 20, 25, 50, 100, "Tutti"]
      ],
      "language": {
        "url": "/public/i18n/DataTablesItalian.json"
      }
    });
    this.find('[datatable-nopaging]').DataTable({
      paging: false,
      "language": {
        "url": "/public/i18n/DataTablesItalian.json"
      }
    });
    this.find('[datatable-small]').DataTable({
      "pageLength": 10,
      "lengthMenu": [
        [10, 15, 20, 25, 50, 100, -1],
        [10, 15, 20, 25, 50, 100, "Tutti"]
      ],
      "language": {
        "url": "/public/i18n/DataTablesItalian.json"
      }
    });
    //le richieste di assenza hanno bisogno di un ordinamento decrescente sulla prima colonna
    this.find('[datatable-absencerequest]').DataTable({
      "order": [
        [0, "desc"]
      ],
      "pageLength": 10,
      "lengthMenu": [
        [10, 15, 20, 25, 50, 100, -1],
        [10, 15, 20, 25, 50, 100, "Tutti"]
      ],
      "language": {
        "url": "/public/i18n/DataTablesItalian.json"
      }
    });    
    //i buoni pasto hanno bisogno di un doppio ordinamento... per quello hanno una regola speciale.
    this.find('[datatable-mealTicket]').DataTable({
      "order": [
        [2, "desc"],
        [0, "desc"]
      ],
      "pageLength": 10,
      "lengthMenu": [
        [10, 15, 20, 25, 50, 100, -1],
        [10, 15, 20, 25, 50, 100, "Tutti"]
      ],
      "language": {
        "url": "/public/i18n/DataTablesItalian.json"
      }
    });
    //Datatables. Se imposto lo scrollX devo ricordarmi di non avere
    //il plugin responsive abilitato sulla tabella(sono incompatibili)
    this.find('.datatable-test').DataTable({
      dom: 'Rlfrtip', //per drag drop colonne
      "scrollX": true,
      "columnDefs": [{
        "width": "150px",
        "targets": 0
      }], // NB: serve per il Nome Cognome.
      "lengthMenu": [
        [10, 25, 50, -1],
        [10, 25, 50, "All"]
      ]
    });
    this.find('span[notAllowed]').tooltip();
    // Quando ridisegno la datatables devo rieseguire la initepas per inizializzare
    // javascript sulle linee visualizzate per la prima volta. (esempio next page)
    this.find('.datatable-test').on('draw.dt', function() {
      var $this = $(this);
      /* alert( 'Table redrawn' ); */
      $this.initepas();
    });
    this.find('input[datepicker-year]').datepicker({
      format: "dd/mm/yyyy",
      startView: 2,
      todayBtn: "linked",
      language: "it",
      autoclose: true,
      todayHighlight: true,
      startDate: '-100y',
      endDate: '+100y'
    });
    this.find('input[datepicker-month]').datepicker({
      format: "dd/mm",
      startDate: "1/1",
      endDate: "31/12",
      language: 'it',
      autoclose: true,
      todayHighlight: true
    });
    this.find('input[datepicker]').datepicker({
      format: "dd/mm/yyyy",
      todayBtn: "linked",
      language: "it",
      autoclose: true,
      startDate: '-100y',
      endDate: '+100y'
    });
    this.find('data-tooltip').tooltip();
    this.find('a[data-x-editable][data-type="textarea"]').editable({
      showbuttons: 'bottom'
    });
    // $.fn.editable.defaults.mode = 'inline';
    this.find('a[data-x-editable]').editable();
    this.find("a[data-popover]").popover();
    this.find('form[data-reload-no-ajax] input[type=text]').on('input', function(e) {
      var $form = $(this).closest("form");
      var $this = $(this);
      var autochange_timeout = $this.data('autochange_timeout')
      if (autochange_timeout) {
        clearTimeout(autochange_timeout);
        $this.removeData('autochange_timeout', autochange_timeout);
      }
      $this.data('autochange_timeout', setTimeout(function() {
        $form.submit();
      }, 500));
    });
    this.find('form[data-reload] :input').on('change', function(e) {
      var $form = $(this).closest("form");
      var selector = $form.data('reload');
      var $target = $(selector);
      $target.addClass('reloading');
      var $spinner = $('<span class="text-primary" style="position:absolute; z-index: 10"><i class="fa fa-spin fa-spinner fa-2x"></i></span>').prependTo($target);
      var offset = $spinner.offset();
      $spinner.offset({
        top: offset.top + 1,
        left: offset.left + 250
      });
      var url = $form.prop('action') + '?' + $form.find(":input").serialize();
      $target.load(url + ' ' + selector, function(response, status, request) {
        // History.replaceState(null, $('title').text, url);
        $target.removeClass('reloading');
        $target.initepas();
      });
    });
    this.find('form[data-reload] input[type=text]').on('input', function(e) {
      var $this = $(this);
      var autochange_timeout = $this.data('autochange_timeout')
      if (autochange_timeout) {
        clearTimeout(autochange_timeout);
        $this.removeData('autochange_timeout', autochange_timeout);
      }
      $this.data('autochange_timeout', setTimeout(function() {
        $this.trigger('change');
      }, 500));
    });
    this.find('#buttonError').click(function() {
      $('#flash-error').hide();
    });
    this.find('#buttonSuccess').click(function() {
      $('#flash-success').hide();
    });
    this.find('.auto-submit').change(function() {
      $(this).submit();
    });
    this.find('.delete-confirmed').click(function() {
      var $delete = $(this).find('.delete');
      var $deleteFirst = $(this).find('.delete-first');
      $deleteFirst.hide();
      $delete.show("fast");
    });
  } /* fine initepas() */
  $('body').initepas();
}); /* fine on document load */

function generateUserName(name, surname, username) {
  var name = name.val().replace(/\W/g, '').toLowerCase();
  var surname = surname.val().replace(/\W/g, '').toLowerCase();
  username.empty(); // remove old options
  var options = [{
    text: null,
    value: null
  }, {
    text: name + '.' + surname,
    value: name + '.' + surname
  }, {
    text: name.charAt(0) + '.' + surname,
    value: name.charAt(0) + '.' + surname
  }, {
    text: name + '_' + surname,
    value: name + '_' + surname
  }];
  $.each(options, function(index, option) {
    $option = $("<option></option>").attr("value", option.value).text(option.text);
    username.append($option);
  });
}
moment.locale('it_IT');