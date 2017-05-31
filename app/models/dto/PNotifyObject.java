package models.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author daniele
 * @since 31/05/17.
 */
@Data
@Builder
public class PNotifyObject {

  private String title; // The notice's title.
  private String title_escape; // Whether to escape the content of the title. (Not allow HTML.)
  private String text; // The notice's text.
  private String text_escape; // Whether to escape the content of the text. (Not allow HTML.)
  private String styling; // Can be either "brighttheme", "bootstrap3", "fontawesome"
  private String addclass; // Additional classes to be added to the notice. (For custom styling.)
  private String cornerclass; // Class to be added to the notice for corner styling.
  private Boolean auto_display; // Display the notice when it is created. Turn this off to add notifications to the history without displaying them.
  private String width; // Width of the notice.
  private String min_height; // Minimum height of the notice. It will expand to fit content.
  private String type; // Type of the notice. "notice", "info", "success", or "error".
  private Boolean icon; // Set icon to true to use the default icon for the selected style/type, false for no icon, or a string for your own icon class.
  private String animation; // The animation to use when displaying and hiding the notice. "none" and "fade" are supported through CSS. Others are supported through the Animate module and Animate.css.
  private String animate_speed; // "slow", "normal", or "fast". Respectively, 400ms, 250ms, 100ms.
  private Boolean shadow; // Display a drop shadow.
  private Boolean hide; // After a delay, remove the notice.
  private int delay = 8000; // Delay in milliseconds before the notice is removed.
  private Boolean mouse_reset; // Reset the hide timer if the mouse moves over the notice.
  private Boolean remove; // Remove the notice's elements from the DOM after it is removed.
  private Boolean insert_brs; // Change new lines to br tags.
  private Boolean destroy; // Whether to remove the notice from the global array when it is closed.
}
