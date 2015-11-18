$(document).ready(function() {
  //    Attivazione di filtro,e pulsanti seleziona tutti/nessuno sulle list-group
  $('select.list-group').each(function() {
    var $this = $(this);
    var $ul = $this.prev('ul.list-group');
    var $options = [];
    $ul.find('a').each(function() {
      $options.push({
        value: $(this).attr('data-value'),
        text: $(this).text()
      });
    });
    var $search = $('<input>').addClass('form-control').attr('type', 'text');
    var $searchBlock = $('<div>').addClass('input-group').append($('<span>').addClass(
      'input-group-addon').append($('<i>').addClass('glyphicon glyphicon-search'))).append(
      $search);
    var $select_all = $('<button>').attr('type', 'button').addClass('btn btn-success').text(
      'Select All');
    var $select_none = $('<button>').attr('type', 'button').addClass('btn btn-warning').text(
      'Select None');
    var $btn_group = $('<div>').addClass('btn-group').attr('role', 'group');
    $btn_group.append($select_all).append($select_none);
    $searchBlock.insertBefore($ul);
    $btn_group.insertBefore($ul);
    $select_all.click(function() {
      $($ul).find('a').each(function() {
        if (!$(this).hasClass('active')) {
          $(this).click();
        }
      });
    });
    $select_none.click(function() {
      $($ul).find('a').each(function() {
        if ($(this).hasClass('active')) {
          $(this).click();
        }
      });
    });
    $search.bind('change keyup', function() {
      var options = $ul.empty().data('options');
      var search = $(this).val().trim();
      var regex = new RegExp(search, "gi");
      $.each($options, function(i) {
        var option = $options[i];
        if (option.text.match(regex) !== null) {
          $ul.append($('<a>').addClass('list-group-item').prop("href", "#").text(
            option.text).attr('data-value', option.value));
          $ul.trigger("change");
        }
      });
    });
  });
});