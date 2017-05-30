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
      viewRender: function (view, element) {
        var data = {
         'activity.id': $('#activity').val(),
         start: view.start.format(),
         end: view.end.subtract(1, 'days').format()
        };
        // Caricamento asincrono delle persone in base al periodo
        $('[data-render-load]').each(function() {
            var url = $(this).data('render-load');
            $(this).load(url,data);
        });
      },
      eventSources: []
    };
    if ($this.data('calendar-date')) {
        data['defaultDate'] = $this.data('calendar-date');
    }
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
    if ($this.data('calendar-event-remove')) {
      data['eventRender'] = function(event, element) {
        if ($.inArray("removable", event.className) != -1) {
          var url = $this.data('calendar-event-remove');
          // Aggiunge l'icona per la rimozione dell'evento nel caso sia impostata la classe removable
          // nell'evento
          element.prepend("<button type='button' class='close' aria-label='Close'><span aria-hidden='true'>&times;</span></button>");
          element.find(".close").click(function() {
            $.ajax({
              type: 'POST',
              url: url,
              data: {
                'psd.id': event.personShiftDayId
              },
              error: function(response) {
                new PNotify({
                  title: "dramma",
                  text: response.responseText,
                  type: "error",
                  remove: true
                });
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
            $this.fullCalendar('removeEvents', event._id);
          });
        }
      }
    }
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
            event.shiftSlot = shiftSlot;
            event.personShiftDayId = response;
          }
        });
      }
    }
    $this.fullCalendar(data);
  });

});

$(document).ajaxComplete(function(){
  $('[data-draggable]').draggable({
    revert: true, // immediately snap back to original position
    revertDuration: 0
  });
  $(this).initepas();
})
function getCurrentViewDate(input) {
  var currentViewDate = $('[data-calendar]').fullCalendar( 'getDate' ).format();
  $(input).val(currentViewDate);
}
