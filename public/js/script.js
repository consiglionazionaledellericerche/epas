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


















