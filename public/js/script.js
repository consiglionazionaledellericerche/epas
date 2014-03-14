/* Author: 

*/



$(function($) {
	// $.fn.editable.defaults.mode = 'inline';
	$('a[data-x-editable]').editable();
	
	$('a.popup_window').popupWindow({ 
		height:600, 
		width:1000,
		scrollbars:1,
		resizable:0,
		top:50, 
		left:50 
		});
	$('a.popup_window_mini').popupWindow({ 
		height:600, 
		width:800,
		scrollbars:1,
		resizable:0,
		top:50, 
		left:50 
		}); 
	
	//$(':input[type=date]').datepicker();
	
	
});

$(function () {
    $("a[data-popover]").popover();
});

$(function() {
    $( "#datepicker1" ).datepicker();
  });

$(function() {
    $( "#datepicker2" ).datepicker();
  });

$(function() {
    $( "#datepicker3" ).datepicker();
  });

$('#myModal1').on('show', function () {
	$('#myModal2').modal('hide');
})

$('#myModal2').on('show', function () {
	$('#myModal1').modal('hide');
})

$('#myModal1').on('hide', function(){
    $(this).data('modal', null);
});

$('#myModal2').on('hide', function(){
    $(this).data('modal', null);
});




$('#myModal1').on('hidden', function(){
    $(this).data('modal', null);
});

$('#myModal2').on('hidden', function(){
    $(this).data('modal', null);
});

$('#myModal3').on('hidden', function(){
    $(this).data('modal', null);
});

$('#myModal4').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-insert-contract').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-edit-contract').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-edit-source-contract').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-terminate-person').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-edit-vacationperiod').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-insert-vacationperiod').on('hidden', function(){
    $(this).data('modal', null);
});

$('#modal-absencetype-month').on('hidden', function(){
    $(this).data('modal', null);
});


$('#select1').editable(); 

$('#select2').editable(); 

$('#select3').editable(); 

$('#select4').editable(); 

$('#select5').editable(); 

$('#select6').editable(); 

$('#simpleText1').editable(); 

$('#simpleText2').editable();

$('#simpleText3').editable(); 


$('#textComments1').editable({
    showbuttons: 'bottom'
}); 



	


