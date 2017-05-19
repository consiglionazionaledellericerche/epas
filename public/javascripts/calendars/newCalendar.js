$( document ).ready(function() {
	$('[data-calendar]', this).each(function() {
        var $this = $(this);
        var data = {
          columnFormat: 'dddd',
          fixedWeekCount: false,
          droppable: true,
//          contentHeight: 'auto',
          loading: function(loading) {
            if (loading) {
              $this.addClass('reloading');
              var $spinner = $(
                '<span class="text-primary" style="position:absolute; z-index: 10"><i class="fa fa-spin fa-spinner fa-2x"></i</span>'
              );
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
            	  new PNotify({
            		  title: "dramma",
            		  text: "dramma",
            		  type: "success",
            		  remove: true
            	  });
            if (event.url === undefined) {
              return false;
            }
            window.open(event.url,'_self');
          },
          drop: function( date, jsEvent, ui, resourceId ) {
              alert("Dropped on " + date.format());
          },
          eventRender: function(event, element) {
//              Questa parte volendo si puo' scrivere generica e specificare i campi in un parametro html
//              nel caso si voglia mostrare altri campi rispetto ai default
//              element.find(".fc-content")
//                     .append("<span>"+ event.start.format()+ "</span>");
        	  
//             per usare i tooltip sugli eventi
             element.qtip({
                content: event.shiftSlot + ' ' + event.personId
                
             });
          },
          eventSources: []
        };

        data['eventSources'].push({
            events: function(start, end, timezone, callback) {
                $.ajax({
                    url: $this.data('calendarSource'),
                    type: 'GET',
                    data: {
                        start: start.format(),
                        end: end.subtract(1,'days').format()
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
        if ($this.data('calendarClick')) {
          data['dayClick'] = function(date, jsEvent, view) {

            // $(this) si riferisce al <td> associato a date
            // composizione dell'url con la data
            var url = $this.data('calendarClick');
            if (url.indexOf('?') >= 0) {
              url += '&';
            } else {
              url += '?';
            }
            url += $.param({
              dateTime: date.format()
            });
            window.open(url,'_self');
          }
        }
        if ($this.data('calendar-drop')) {
          data['eventStartEditable'] = true;
          data['eventDrop'] = function(event, delta, revertFunc) {
            var url = $this.data('calendar-drop');
            $.ajax({
                type: 'POST',
                url: url,
                data: { id: event.personId,
                 start: event.start.format(),
                 // Restituiamo un giorno in meno di modo che, lato server, siamo in grado di gestire la
                 // terminazione dell'evento con la corretta data di fine dell'evento stesso
                 end: event.end ? event.end.clone().subtract(1,'days').format() : event.start.format(),
                 originalStart: event.start_orig,
                 originalEnd: event.end_orig || event.start_orig
                 },
                error: function(response){
              	  new PNotify({
            		  title: "dramma",
            		  text: response.responseText,
            		  type: "error",
            		  remove: true
            	  });
                    revertFunc();
                },
                success: function(){
                  event.start_orig = event.start.format();
                  event.end_orig = event.end ? event.end.clone().subtract(1,'days').format() : null;
                    // bootbox.alert('successfully modified');
                    // Si comunica in qualche modo il corretto salvataggio?
                }
                });
          }
        }
        if ($this.data('calendar-resize')) {
          data['eventDurationEditable'] = true;
          data['eventResize'] = function(event, delta, revertFunc) {
            var url = $this.data('calendar-resize');

            $.ajax({
                type: 'POST',
                url: url,
                // aggiungere a data tutti i parametri che si vogliono passare al metodo del controller
                data: { id: event.personId,
                 start: event.start.format(),
                 // Restituiamo un giorno in meno di modo che, lato server, siamo in grado di gestire la
                 // terminazione dell'evento con la corretta data di fine dell'evento stesso
                 end: event.end ? event.end.clone().subtract(1,'days').format() : event.start.format(),
                 originalStart: event.start_orig,
                 // La data di fine, in caso di evento su singolo giorno, è nulla.
                 // Pertanto la impostiamo allo stesso valore della data di inizio dell'evento stesso
                 originalEnd: event.end_orig || event.start_orig
                 },
                error: function(){
                    revertFunc();
                },
                success: function(){
                  event.start_orig = event.start.format();
                  event.end_orig = event.end ? event.end.clone().subtract(1,'days').format() : null;

                    // bootbox.alert('successfully modified');
                    // Si comunica in qualche modo il corretto salvataggio?
                }
                });
          }
        }
        $this.fullCalendar(data);
      });
	
	$('[data-draggable]').draggable({
	    revert: true,      // immediately snap back to original position
	    revertDuration: 0  //
	});

});

