/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.enumerate;

/**
 * Enumerato per la gestione dei colori degli eventi per i turni.
 *
 * @author Daniele Murgia
 * @since 26/05/17.
 * 
 */
public enum EventColor {

  GREEN("#dff0d8", "#b9e098", "#3c763d"),  
  BLUE("#d9edf7", "#bce8f1", "#31708f"),
  BROWN("#f2ddb8", "#d1b27f", "#7f6943"),
  LIGHTPURPLE("#f4ecf9", "#e7cde8", "#8a518e"),
  ORANGE("#ffe5ad", "#ffcd61", "#966700"),
  LIGHTPINK("#dfcbcd", "#e8ccd7", "#89729e"),
  YELLOW("#fcf8e3", "#f5d89c", "#8a6d3b"), // da usare per smartworking e telelavoro
  RED("#f2dede", "#ebccd1", "#a94442"); // da usare per le assenze e basta


  public final String backgroundColor;
  public final String borderColor;
  public final String textColor;

  EventColor(String backgroundColor, String borderColor, String textColor) {
    this.backgroundColor = backgroundColor;
    this.borderColor = borderColor;
    this.textColor = textColor;
  }
}
