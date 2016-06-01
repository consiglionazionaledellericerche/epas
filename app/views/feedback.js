%{
	categorySelection = '<span>Categoria</span> <select id="feedback-category" name="category">'
	categorySelection += '<option value="">Seleziona una categoria</option>'	
	for(category in categoryMap.entrySet()) {
		selected = !selectedCategory.empty && selectedCategory.equals(category.key) ? 'selected="selected"' : ''
		categorySelection += '<option ' + selected + ' value="'+ category.key + '">' + category.value + '</option>'
	}
	categorySelection += '</select>'
}%

//<span>Categoria</span> <select id="feedback-category" name="category"><option value="">Seleziona una categoria</option><option value="technical">Tecnica</option><option value="assenze">Assenze</option>
$(function() {
    $.feedback({initButtonText: '<i class="fa fa-bug"></i> Segnala un problema',
          ajaxURL: "@{ReportCentre.sendReport()}",
          tpl: {
          description:	'<div id="feedback-welcome"><div class="feedback-logo">Segnalazione</div><p>Con questo puoi segnalare un problema o dare un suggerimento.</p><p>Scrivi una breve descrizione del problema riscontrato:</p><textarea id="feedback-note-tmp"></textarea><p>Successivamente potrai identificare l\'area della pagina correlata al problema.</p><button id="feedback-welcome-next" class="feedback-next-btn feedback-btn-gray">Continua</button><div id="feedback-welcome-error">Descrizione richiesta.</div><div class="feedback-wizard-close"></div></div>',
          highlighter:	'<div id="feedback-highlighter"><div class="feedback-logo">Segnalazione</div><p>Fai click e trascina sulla pagina per aiutarci a capire meglio la tua segnalazione. Se occorresse puoi anche spostare questa finestra di dialogo.</p><button class="feedback-sethighlight feedback-active"><div class="ico"></div><span>Evidenzia</span></button><label>Evidenzia l\'area correlata alla tua segnalazione.</label><button class="feedback-setblackout"><div class="ico"></div><span>Oscura</span></button><label class="lower">Oscura le informazioni personali.</label><div class="feedback-buttons"><button id="feedback-highlighter-next" class="feedback-next-btn feedback-btn-gray">Continua</button><button id="feedback-highlighter-back" class="feedback-back-btn feedback-btn-gray">Indietro</button></div><div class="feedback-wizard-close"></div></div>',
          overview:		
        	'<div id="feedback-overview">' + 
          		'<div class="feedback-logo">Segnalazione</div>' + 
          		'<div id="feedback-overview-description">' +
          			'<div id="feedback-overview-description-text">' + 
          				'<h3>Descrizione</h3>' + 
          				'<h3 class="feedback-additional">Informazioni addizionali</h3>' + 
          				'<div id="feedback-additional-none">${categorySelection}</div>' + 
          				'<div id="feedback-browser-info"><span>Informazioni sul browser</span></div>' + 
          				'<div id="feedback-page-info"><span>Informazioni sulla pagina</span></div>' + 
          				'<div id="feedback-page-structure"><span>Struttura della pagina</span></div></div></div>' +
          				'<div id="feedback-overview-screenshot"><h3>Screenshot</h3></div>' + 
          				'<div class="feedback-buttons">' + 
          					'<button id="feedback-submit" class="feedback-submit-btn feedback-btn-blue">Invia</button>' + 
          					'<button id="feedback-overview-back" class="feedback-back-btn feedback-btn-gray">Indietro</button>' + 
          				'</div>' + 
          				'<div id="feedback-overview-error">Inserire una descrizione e selezionare una tipologia.</div> ' + 
          				'<div class="feedback-wizard-close">' + 
          				'</div>' +
          			'</div>',
          submitSuccess:	'<div id="feedback-submit-success"><div class="feedback-logo">Segnalazione</div><p>La tua segnalazione è stata registrata con successo.</p><button class="feedback-close-btn feedback-btn-blue">OK</button><div class="feedback-wizard-close"></div></div>',
          submitError:	'<div id="feedback-submit-error"><div class="feedback-logo">Segnalazione</div><p>Grazie per la collaborazione, la tua segnalazione è stata registrata con successo.</p><button class="feedback-close-btn feedback-btn-blue">OK</button><div class="feedback-wizard-close"></div></div>'
          },
          html2canvasURL: "@{'/public/javascripts/html2canvas.min.js'}"});
    $('button.feedback-btn').addClass('hidden-xs');
});
