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

package models.dto;

import lombok.Builder;
import lombok.Data;

/**
 * PNotifyObject definition.
 *
 * @author Daniele Murgia
 * @since 31/05/17
 */
@Data
@Builder
public class PNotifyObject {
  
  //The notice's title.
  private String title;
  
  //Whether to escape the content of the title. (Not allow HTML.)
  private String title_escape; 
  
  //The notice's text.
  private String text;
  
  //Whether to escape the content of the text. (Not allow HTML.)
  private String text_escape;

  //Can be either "brighttheme", "bootstrap3", "fontawesome"
  private String styling;
  
  private String addclass; // Additional classes to be added to the notice. (For custom styling.)
  
  //Class to be added to the notice for corner styling.
  private String cornerclass; 
  
  //Display the notice when it is created. Turn this off to add notifications to the history 
  //without displaying them.
  private Boolean auto_display;
  
  //Width of the notice.
  private String width;
  
  //Minimum height of the notice. It will expand to fit content.
  private String min_height;

  //Type of the notice. "notice", "info", "success", or "error".
  private String type; 
  
  //Set icon to true to use the default icon for the selected style/type, false for no icon, 
  //or a string for your own icon class.
  private Boolean icon;
  
  //The animation to use when displaying and hiding the notice. "none" and "fade" are supported 
  //through CSS. Others are supported through the Animate module and Animate.css.
  private String animation;
  
  //"slow", "normal", or "fast". Respectively, 400ms, 250ms, 100ms.
  private String animate_speed;
  
  //Display a drop shadow.
  private Boolean shadow; 
  
  //After a delay, remove the notice.
  private Boolean hide; 
  
  //Delay in milliseconds before the notice is removed.
  private Integer delay; 
  
  //Reset the hide timer if the mouse moves over the notice.
  private Boolean mouse_reset;
  
  private Boolean remove; // Remove the notice's elements from the DOM after it is removed.
  
  //Change new lines to br tags.
  private Boolean insert_brs;
  
  //Whether to remove the notice from the global array when it is closed.
  private Boolean destroy; 
}
