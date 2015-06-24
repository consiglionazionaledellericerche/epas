/* Author: 
 */

$(function($){
	
	$.fn.initepas = function() {
		
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
			  format: "yyyy-mm-dd",
			  startView: 2,
			  todayBtn: "linked",
			  language: "it",
			  autoclose: true,
			  todayHighlight: true
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
			  format: "yyyy-mm-dd",
			  todayBtn: "linked",
			  language: "it",
			  autoclose: true,
			  todayHighlight: true
			});
		
		this.find('data-tooltip').tooltip();
		
		this.find('.my-modal').on('hidden.bs.modal', function(){
			$(this).data('bs.modal', null);
		    /* $(this).find('.modal-content').empty(); */
		});
		
		this.find('.my-modal-large').on('hidden.bs.modal', function(){
			$(this).data('bs.modal', null); /* vecchio metodo che non svuotava il modale*/
			/* $(this).removeData('bs.modal'); per bootstrap precedente al 3*/
			/* $(this).find('.modal-content').empty(); nuovo metodo che però non funziona */
		});
		
		// $.fn.editable.defaults.mode = 'inline';
		this.find('a[data-x-editable]').editable();
		
		this.find("a[data-popover]").popover();
		this.find("input[data-datepicker]").datepicker();
		this.find("#datepicker1" ).datepicker();
		this.find(".datepicker" ).datepicker();
		this.find("#datepicker3" ).datepicker();
		
		this.find('#myModal1').on('show', function () {
			$('#myModal2').modal('hide');
		})

		this.find('#myModal2').on('show', function () {
			$('#myModal1').modal('hide');
		})

		this.find('#myModal1').on('hide', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal2').on('hide', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal1').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal2').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#myModal3').on('hidden.bs.modal', function(){
		    $(this).data('bs.modal', null);
		});
		

		this.find('#myModal4').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-insert-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-source-contract').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-terminate-person').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-edit-vacationperiod').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-insert-vacationperiod').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#modal-absencetype-month').on('hidden', function(){
		    $(this).data('modal', null);
		});

		this.find('#select1').editable(); 
		this.find('#select2').editable(); 
		this.find('#select3').editable(); 
		this.find('#select4').editable(); 
		this.find('#select5').editable(); 
		this.find('#select6').editable(); 
		this.find('#simpleText1').editable(); 
		this.find('#simpleText2').editable();
		this.find('#simpleText3').editable(); 
		

		this.find('#textComments1').editable({
		    showbuttons: 'bottom'
		});
		
		this.find('#dob1').editable();
		
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
	    
	    this.find('a[data-modal]').click(function(e) {
			var $this = $(this);
			var url = $this.attr('href');
			var $modal = $($this.data('modal'));
			var $modalbody = $modal.modal('show').find('.modal-content');
			$modalbody.load(url, function() {
				$modalbody.initepas();
			});
			e.preventDefault();
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

function generateUserName(){
 var name = $('#manager_name').val().replace(/\W/g, '').toLowerCase();
 var surname = $('#manager_surname').val().replace(/\W/g, '').toLowerCase();

 var $el = $("#manager_username");
   $el.empty(); // remove old options

   var options = [
   {text: name+'.'+surname, value: name+'.'+surname},
   {text: name.charAt(0)+'.'+surname, value: name.charAt(0)+'.'+surname},
   {text: name+'_'+surname, value: name+'_'+surname}
   ];

   $.each(options, function(index, option) {
    $option = $("<option></option>")
    .attr("value", option.value)
    .text(option.text);
    $el.append($option);
  });
 }









	


