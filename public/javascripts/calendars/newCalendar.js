$(document).ready(function() {
  $('[data-calendar]', this).each(function() {
    var $this = $(this);
    var data = {
      height: 'auto',
      columnFormat: 'dddd',
      fixedWeekCount: false,
      droppable: true,
      eventOrder: "editable, title",
      loading: function(loading) {
        if (loading) {
          $this.addClass('reloading');
          var $spinner = $('<span class="text-primary" style="position:absolute; z-index: 10"><i class="fa fa-spin fa-spinner fa-3x"></i</span>');
          $this.before($spinner);
          var pos = $this.offset();
          var centerX = pos.left + $this.width() / 2;
          $spinner.offset({
            top: pos.top + 180,
            left: centerX
          });
        } else {
          $this.removeClass('reloading');
          $this.prev('span.text-primary').remove();
        }
      },
      viewRender: function(view, element) {
        var data = {
          'activity.id': $('#activity').val(),
          start: view.start.format(),
          end: view.end.subtract(1, 'days').format(),
          intervalStart: view.intervalStart.format()
        };
        // Caricamento asincrono delle persone in base al periodo
        $('[data-render-load]').each(function() {
          var url = $(this).data('render-load');
          $(this).load(url, data);
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
    // Festività dal calendario Google
    data['eventSources'].push({
      googleCalendarApiKey: 'AIzaSyDxn95GcuRQ8VqsDiu72LlebkplabI1ppM', // egovernment.cnr@gmail.com
      googleCalendarId: 'it.italian#holiday@group.v.calendar.google.com',
      rendering: 'background',
      className: 'holiday',
    });
    if ($this.data('calendar-event-remove')) {
      data['eventRender'] = function(event, element) {
        // Per visualizzare il titolo anche sugli eventi renderizzati come background (festività)
        if (event.source && event.source.rendering === 'background') {
          element.append("<em>" + event.title + "</em>");
        }
        if ($.inArray("removable", event.className) != -1) {
          var url = $this.data('calendar-event-remove');
          // Aggiunge l'icona per la rimozione dell'evento nel caso sia impostata la classe removable
          // nell'evento
          var button = $("<button></button>", {
            "type": "button",
            "class": "close",
            "data-tooltip": "",
            "title": "Rimuovi Turno",
            "aria-label": "Close"
          });
          button.append("<span aria-hidden='true'>&times;</span>");
          element.prepend(button);
          button.click(function() {
            $.confirm({
              title: 'Eliminare questo turno?' ,
              content: event.start.format() + ' - ' + event.title,
              backgroundDismiss: true,
              buttons: {
                confirm: {
                  text: 'Elimina <i class="fa fa-trash-o" aria-hidden="true"></i>',
                  btnClass: 'btn-red',
                  action: function() {
                    $.ajax({
                      type: 'POST',
                      url: url,
                      data: {
                        'psd.id': event.personShiftDayId
                      },
                      error: function(response) {
                        new PNotify(response.responseJSON);
                      },
                      success: function(response) {
                        new PNotify(response);
                        $this.fullCalendar('removeEvents', event._id);
                      }
                    });
                  }
                },
                cancel: {
                  text: 'Annulla'
                }
              }
            });
          });
        }
        // Rendering dei problemi sui turni tramite un popover
        if (event.troubles) {
          var div = $("<div></div>", {
            "class": "webui-popover-content"
          });
          event.troubles.forEach(function(item) {
            div.append(item + '<br>');
          });
          var icon = $("<i></i>", {
            "class": "fa fa-exclamation-triangle",
            "aria-hidden": true,
            "data-title": "Errori sul turno"
          });
          icon.webuiPopover({
            placement: 'auto',
            trigger: 'hover',
            type: 'html',
            style: 'alert',
            animation: 'pop',
            dismissible: true,
            delay: {
              show: null,
              hide: null
            }
          });
          element.prepend(div);
          element.prepend(icon);
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
            // Passare un JSON serializzato a partire da un PNotifyObject definito lato Java
            new PNotify(response.responseJSON);
            revertFunc();
          },
          success: function(response) {
            new PNotify(response);
            $this.fullCalendar('refetchEvents');
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
          error: function(response) {
            // Passare un JSON serializzato a partire da un PNotifyObject definito lato Java
            new PNotify(response.responseJSON);
            // Non essendoci la revertFunc() eliminiamo il nuovo evento in caso di 'errore' (turno non inseribile)
            $this.fullCalendar('removeEvents', event._id);
          },
          success: function(response) {
            new PNotify(response);
            $this.fullCalendar('refetchEvents');
          }
        });
      }
    }
    $this.fullCalendar(data);
  });
});
$(document).ajaxComplete(function() {
  $('[data-draggable]').draggable({
    revert: true, // immediately snap back to original position
    revertDuration: 0
  });
  $(this).initepas();
});

function getCurrentViewDate(input) {
  var currentViewDate = $('[data-calendar]').fullCalendar('getDate').format();
  $(input).val(currentViewDate);
}