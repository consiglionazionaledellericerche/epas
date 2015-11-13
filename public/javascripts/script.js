/* Author: 
 */

$(function($){
	
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
        if($form.attr('enctype') === 'multipart/form-data') {
        	contentType = false;
        	formData = new FormData($form[0]) //IE9? issue stackoverflow 20795449 
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
            $(target).parent().initepas();
            // disattiva la modale sopra (se c'è).
            $form.parents('.modal').modal('hide');
        }).fail(function(xhr, status, error) {
        	if (xhr.status == 400) {
        		var $res = $(errorTarget, xhr.responseText);
        		var $etarget = errorTarget ? $(errorTarget) : $form;
        		$etarget.html($res.html()).parent().initepas();
        	} else {
        		
        		//bootbox.alert('Si è verificato un errore: '+ error);
        	}// else segnala l'errore in qualche modo.
        }).always(function() {
//        	$form.find(':input').prop('readonly', true);
//        	$form.css('background-color', bgcolor);
        });
        e.preventDefault();
    });
    
    /**
     * Author: Marco
     */
    $(document.body).on('click', 'a[data-async-modal]', function(e) {
    	var $this = $(this);
    	var $modal = $($this.data('async-modal'));
    	var url = $this.attr('href');
    	$('body').modalmanager('loading');

    	$modal.load(url, '', function() {
    		$modal.modal().initepas();
    	});
    	e.preventDefault();
    });
    
    bootbox.setDefaults({locale: 'it', className: 'bootbox_modal'});

    /* switcher 2/3 tabs. 
     * mettere come id:
     * ai li pills1 pills2 pills3
     * ai div divpills1 divpills2 divpills3
     */ 
    $.fn.switchpills = function() {
    	var divpills1 = $('#divpills1');
    	var divpills2 = $('#divpills2');
    	var divpills3 = $('#divpills3');
		var pills1 = $('#pills1');
		var pills2 = $('#pills2');
		var pills3 = $('#pills3');
		
		function toggle() {
			divpills1.toogle(0);
			divpills2.toogle(0);
			divpills3.toogle(0);
		}  
    	
		$(pills1.click(function(){
			pills1.addClass('active');
			pills2.removeClass('active');
			pills3.removeClass('active');
			divpills1.show();
			divpills2.hide();
			divpills3.hide();
			
		}));
		$(pills2.click(function(){
			pills1.removeClass('active');
			pills2.addClass('active');
			pills3.removeClass('active');
			divpills1.hide();
			divpills2.show();
			divpills3.hide();
			
		}));
		$(pills3.click(function(){
			pills1.removeClass('active');
			pills2.removeClass('active');
			pills3.addClass('active');
			divpills1.hide();
			divpills2.hide();
			divpills3.show();
			
		}));
    }
	
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

	$.fn.initepas = function() {
		
		//$(':input[data-selectize]', this).select2({allowClear:true});

		$(':input[select2]', this).select2({allowClear: true,theme: "bootstrap", placeholder: "Seleziona un valore"});
		$(':input[select2Table]', this).select2({minimumResultsForSearch: 25});

		$('[popover]').popover({trigger: "focus",placement: 'right auto',container: 'body'});
		$('[popover-hover]').popover({trigger: "hover",placement: 'right auto',container: 'body'});

		this.find('[datatable]').DataTable({
			"lengthMenu": [ [10, 25, 50,100, -1], [10, 25, 50,100, "Tutti"] ],
			"language": {"url": "/public/i18n/DataTablesItalian.json"}
		});

		//Datatables. Se imposto lo scrollX devo ricordarmi di non avere
		//il plugin responsive abilitato sulla tabella(sono incompatibili)
		this.find('.datatable-test').DataTable( {
	        dom: 'Rlfrtip', //per drag drop colonne
	        "scrollX": true,
	        "columnDefs": [{ "width": "150px", "targets": 0 }],	// NB: serve per il Nome Cognome.
	        "lengthMenu": [ [10, 25, 50, -1], [10, 25, 50, "All"] ]
	    } );
		
		// Quando ridisegno la datatables devo rieseguire la initepas per inizializzare
		// javascript sulle linee visualizzate per la prima volta. (esempio next page)
		this.find('.datatable-test').on( 'draw.dt', function () {
			var $this = $(this);
		    /* alert( 'Table redrawn' ); */
		    $this.initepas();
		} );
		
		
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
	    	$spinner.offset({top:offset.top + 1, left:offset.left + 250});
	    	var url = $form.prop('action') + '?'+ $form.find(":input").serialize();
	    	$target.load(url + ' '+ selector, function(response, status, request) {
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
	    
	    this.find( '.delete-confirmed' ).click(function() {
	    	var $delete = $(this).find( '.delete' );
	    	var $deleteFirst = $(this).find( '.delete-first' );
	    	$deleteFirst.hide();
	    	$delete.show( "fast" );
	    });
	    
	    this.switchpills();
	    
	}	/* fine initepas() */

	$('body').initepas();
	



});	/* fine on document load */

function Change(){
	var assenzaFrequente = document.getElementById("assenzaFrequente");
	var absenceCode = document.getElementById("absenceCode");
	absenceCode.value = assenzaFrequente.value;
}
function Change2(){
	var tuttiCodici = document.getElementById("tuttiCodici");
	var absenceCode = document.getElementById("absenceCode");
	absenceCode.value = tuttiCodici.value;
}

function generateUserName(name,surname,username){
 var name = name.val().replace(/\W/g, '').toLowerCase();
 var surname = surname.val().replace(/\W/g, '').toLowerCase();

   username.empty(); // remove old options

   var options = [
   {text: null,value:null},
   {text: name+'.'+surname, value: name+'.'+surname},
   {text: name.charAt(0)+'.'+surname, value: name.charAt(0)+'.'+surname},
   {text: name+'_'+surname, value: name+'_'+surname}
   ];

   $.each(options, function(index, option) {
    $option = $("<option></option>")
    .attr("value", option.value)
    .text(option.text);
    username.append($option);
  });
}

moment.locale('it_IT');
