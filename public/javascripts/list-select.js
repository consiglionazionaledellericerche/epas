$(document).ready(function() {
  $('div.list-group[data-select]').each(function() {
    var $this = $(this);
    var $search = $('<input>').addClass('form-control').attr('type', 'text');
    var searchBlock = $('<div>').addClass('input-group').append($('<span>').addClass(
      'input-group-addon').append($('<i>').addClass('glyphicon glyphicon-search'))).append(
      $search);
    var $select_all = $('<button>').attr('type', 'button').addClass('btn btn-default').text(
      'Select All');
    var $select_none = $('<button>').attr('type', 'button').addClass('btn btn-default').text(
      'Select None');
    var btn_group = $('<div>').addClass('btn-group pull-right').attr('role', 'group');
    btn_group.append($select_all).append($select_none);
    var divRow = $('<div>').addClass('row data-select-bar');
    var divCol = $('<div>').addClass('col-xs-6');
    var divCol2 = $('<div>').addClass('col-xs-6');
    divRow.append(divCol.append(searchBlock)).append(divCol2.append(btn_group));
    divRow.insertBefore($this);
    $this.find('a').each(function() {
      $(this).click(function() {
        if ($(this).hasClass('list-group-item-info')) {
          $(this).unselectElement();
        } else {
          $(this).selectElement();
        }
      });
    });
    $select_all.click(function() {
      $this.find('a').each(function() {
        if (!$(this).hasClass('list-group-item-info')) {
          $(this).selectElement();
        }
      });
    });
    $select_none.click(function() {
      $this.find('a').each(function() {
        if ($(this).hasClass('list-group-item-info')) {
          $(this).unselectElement();
        }
      });
    });
    $.fn.selectElement = function() {
      $(this).addClass('list-group-item-info');
      var input = $('<input>').attr('name', $this.attr('data-select')).attr('type',
        'hidden').attr('value', $(this).attr('data-value'));
      input.insertAfter($(this));
    }
    $.fn.unselectElement = function() {
      $(this).removeClass('list-group-item-info');
      $('input[value=' + $(this).attr('data-value') + ']').remove();
    }
    $(this).addClass('list-group-item-info');
    $search.bind('change keyup', function() {
      var search = $(this).val().trim();
      var regex = new RegExp(search, "gi");
      var founded = 0;
      $this.find('a').each(function() {
        if (!$(this).hasClass('list-group-item-info')) {
          if ($(this).text().match(regex) !== null) {
            founded++;
            $(this).show();
          } else {
            $(this).hide();
          }
        }
      });
      if (founded == 0) {
        $('div.alert').each(function() {
          $(this).remove();
        });
        $('<div>').addClass('alert alert-warning').attr('role', 'alert').append('<p>').addClass(
          'text-center').text('Nessuna occorrenza rilevata').insertAfter($this);
      } else {
        $('div.alert').each(function() {
          $(this).remove();
        });
      }
    });
  });
});
