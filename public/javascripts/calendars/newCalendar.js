
$(document).ready(function() {

	$('[data-calendar]', this).each(function() {
		var $this = $(this);
		var data = {
				columnFormat: 'dddd',
				selectable: true,
				fixedWeekCount: false,
				droppable: true,
				loading: function(loading) {
					if (loading) {
						$this.addClass('reloading');
						var $spinner = $('<span class="text-primary" style="position:absolute; z-index: 10"><i class="fa fa-spin fa-spinner fa-2x"></i</span>');
						$this.before($spinner);
						var offset = $spinner.offset();
						$spinner.offset({
							top: offset.top + 10,
							left: offset.left + 10
						});
					} else {
						$this.removeClass('reloading');
						$this.prev('span.text-primary').remove();
					}
				},
				eventClick: function(event, jsEvent, view) {
					if (event.url) {
						$.ajax({
							type: 'POST',
							url: event.url,
							data: {'psd.id': event.personShiftDayId},
							error: function(response) {
								new PNotify({
									title: "Errore",
									text: response.responseText,
									type: "error",
									remove: true
								});

							},
							success: function() {
								$this.fullCalendar('removeEvents', event._id);
								new PNotify({
									title: "Ok",
									text: response.responseText,
									type: "success",
									remove: true
								});
							}
						});
					}
					return false;
				},
				select: function( start, end, jsEvent, view ) {
//					$(".fc-highlight").css("background", "red");
					new PNotify({
						title: "Errore",
						text: "eliminare date dal " + start.format() + " al " + end.format(),
						type: "error",
						remove: true
					});
				},
//				dayRender: function( date, cell ) {

//				cell.hover(function() {
//				cell.css('background-color', 'red')
//				},function() {
//				cell.css('background-color', 'white')
//				});
//				},
//				dayClick: function(date, jsEvent, view) {
//
//					// change the day's background color just for fun
//					$(this).css('background-color', 'yellow');
//
//				},

				eventSources: []
		};

		data['eventSources'].push({
			events: function(start, end, timezone, callback) {
				var shiftType = $('#activity').val();
				$.ajax({
					url: $this.data('calendarSource'),
					type: 'GET',
					data: {
						'shiftType.id': shiftType,
						start: start.format(),
						end: end.subtract(1, 'days').format()
					},
					success: function(response) {
						callback(response);
					},
					error: function() {
						alert('there was an error while fetching data!');
						callback();
					}
				});
			}
		});

		if ($this.data('calendar-date')) {
			data['defaultDate'] = $this.data('calendar-date');
		}

//		if ($this.data('calendar-click')) {
//		data['dayClick'] = function(date, jsEvent, view) {
//		$('#dialog-form').data('date', date).dialog('open');
//		var url = $this.data('calendar-click');
//		var shiftType = $('#activity').val();
//		$.ajax({
//		type: 'POST',
//		url: url,
//		// aggiungere a data tutti i parametri che si vogliono passare al metodo del controller
//		data: {
//		'shiftType.id': shiftType,
//		date: date.format()
//		},
//		error: function() {
//		revertFunc();
//		},
//		success: function() {}
//		});

//		}
//		}

		if ($this.data('calendar-drop')) {
			data['eventStartEditable'] = true;
			data['eventDrop'] = function(event, delta, revertFunc) {
				var url = $this.data('calendar-drop');
				$.ajax({
					type: 'POST',
					url: url,
					data: {
						personShiftDayId: event.personShiftDayId,
						newDate: event.start.format()
					},
					error: function(response) {
						new PNotify({
							title: "dramma",
							text: response.responseText,
							type: "error",
							remove: true
						});
						revertFunc();
					},
					success: function(response) {
						new PNotify({
							title: "Ok",
							text: response.responseText,
							type: "success",
							remove: true
						});
					}
				});
			}
		}


		// Viene chiamata dopo che si trascina un evento esterno sul calendario
		if ($this.data('calendar-external-drop')) {
			data['eventReceive'] = function(event) {
				var url = $this.data('calendar-external-drop');
				// Recupero il valore del radiobutton relativo allo slot per passarlo al controller
				var shiftSlot = $('input[name="shiftSlot"]:checked').val();
				// Recupero il valore dell'attuale ShiftType selezionato dalla select
				var shiftType = $('#activity').val();
				$.ajax({
					type: 'POST',
					url: url,
					data: {
						personId: event.personId,
						date: event.start.format(),
						shiftSlot: shiftSlot,
						'shiftType.id': shiftType
					},
					error: function() {
						// Non essendoci la revertFunc() eliminiamo il nuovo evento in caso di 'errore' (turno non inseribile)
						$this.fullCalendar('removeEvents', event._id);
					},
					success: function(response) {
						console.log(JSON.stringify(response));
						event.personShiftDayId = response;
						//            console.log(JSON.stringify(event));
					}
				});
			}
		}
		$this.fullCalendar(data);
	});

	$('[data-draggable]').draggable({
		revert: true, // immediately snap back to original position
		revertDuration: 0 //
	});
});