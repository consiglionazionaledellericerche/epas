
$(document).ready(function() {
  $('[data-calendar]', this).each(function() {
    var $this = $(this);
    var data = {
      columnFormat: 'dddd',
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
      //      eventClick: function(event, jsEvent, view) {
      //        new PNotify({
      //          title: "dramma",
      //          text: "dramma",
      //          type: "success",
      //          remove: true
      //        });
      //        if (event.url === undefined) {
      //          return false;
      //        }
      //        window.open(event.url, '_self');
      //      },
      
      
      dayRender: function( date, cell ) {
        cell.hover(function() {
          cell.prepend("<i class='fa fa-trash' aria-hidden='true'></i>");
        },function() {
          cell.find( "i" ).remove();
        });
      },
      
      eventRender: function(event, element) {
        // Aggiunge l'icona per la rimozione dell'evento nel caso sia impostata la classe removable
        // nell'evento
        if ($.inArray("removable", event.className) != -1) {
          element.prepend("<button type='button' class='close' aria-label='Close'><span aria-hidden='true'>&times;</span></button>");
          element.find(".close").click(function() {
            if ($this.data('calendar-event-remove')) {
              var url = $this.data('calendar-event-remove');
              // Recupero il valore dell'attuale ShiftType selezionato dalla select
              var shiftType = $('#activity').val();
              $.ajax({
                type: 'POST',
                url: url,
                data: {
                  'shiftType.id': shiftType,
                  personId: event.personId,
                  start: event.start.format(),
                  end: event.end ? event.end.clone().subtract(1, 'days').format() : event.start.format()
                },
                error: function() {},
                success: function() {
                  // console.log(JSON.stringify(event));
                }
              });
            }
            $this.fullCalendar('removeEvents', event._id);
          });
        }
        // per usare i tooltip sugli eventi
        element.qtip({
          content: event.shiftSlot + ' ' + event.personId,
          show: {
            event: 'click'
          },
          style: {
            classes: 'qtip-bootstrap'
          }
        });
      },
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
    
    if ($this.data('calendar-click')) {
      data['dayClick'] = function(date, jsEvent, view) {
         $('#dialog-form').data('date', date).dialog('open');
        var url = $this.data('calendar-click');
        var shiftType = $('#activity').val();
        $.ajax({
          type: 'POST',
          url: url,
          // aggiungere a data tutti i parametri che si vogliono passare al metodo del controller
          data: {
            'shiftType.id': shiftType,
            date: date.format()
          },
          error: function() {
            revertFunc();
          },
          success: function() {}
        });
        //        chiamata sincrona
        //        if (url.indexOf('?') >= 0) {
        //          url += '&';
        //        } else {
        //          url += '?';
        //        }
        //        url += $.param({
        //          date: date.format()
        //        });
        //        window.open(url, '_self');
      }
    }
    
    if ($this.data('calendar-drop')) {
      data['eventStartEditable'] = true;
      var shiftType = $('#activity').val();
      data['eventDrop'] = function(event, delta, revertFunc) {
        var url = $this.data('calendar-drop');
        $.ajax({
          type: 'POST',
          url: url,
          data: {
        	cancelled: event.cancelled,
            'shiftType.id': shiftType,
            personId: event.personId,
            start: event.start.format(),
            // Restituiamo un giorno in meno di modo che, lato server, siamo in grado di gestire la
            // terminazione dell'evento con la corretta data di fine dell'evento stesso
            end: event.end ? event.end.clone().subtract(1, 'days').format() : event.start.format(),
            originalStart: event.start_orig,
            originalEnd: event.end_orig || event.start_orig
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
          success: function() {
            event.start_orig = event.start.format();
            event.end_orig = event.end ? event.end.clone().subtract(1, 'days').format() : null;
          }
        });
      }
    }
    
    if ($this.data('calendar-resize')) {
      data['eventDurationEditable'] = true;
      data['eventResize'] = function(event, delta, revertFunc) {
        var url = $this.data('calendar-resize');
        var shiftType = $('#activity').val();
        $.ajax({
          type: 'POST',
          url: url,
          // aggiungere a data tutti i parametri che si vogliono passare al metodo del controller
          data: {
        	  cancelled: event.cancelled,
            'shiftType.id': shiftType,
            personId: event.personId,
            start: event.start.format(),
            // Restituiamo un giorno in meno di modo che, lato server, siamo in grado di gestire la
            // terminazione dell'evento con la corretta data di fine dell'evento stesso
            end: event.end ? event.end.clone().subtract(1, 'days').format() : event.start.format(),
            originalStart: event.start_orig,
            // La data di fine, in caso di evento su singolo giorno, è nulla.
            // Pertanto la impostiamo allo stesso valore della data di inizio dell'evento stesso
            originalEnd: event.end_orig || event.start_orig
          },
          error: function() {
            revertFunc();
          },
          success: function() {
            event.start_orig = event.start.format();
            event.end_orig = event.end ? event.end.clone().subtract(1, 'days').format() : null;
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
          success: function() {
            event.shiftSlot = shiftSlot;
            event.start_orig = event.start.format();
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