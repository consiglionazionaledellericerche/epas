package models;

import models.base.MutableModel;
import models.enumerate.AttachmentType;

import org.hibernate.envers.Audited;

import play.data.validation.Required;
import play.db.jpa.Blob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 * @author daniele
 * @since 06/10/16.
 */

@Audited
@Entity
@Table(name = "attachments")
public class Attachment extends MutableModel {

  @NotNull
  @Required
  public String filename;

  public String description;

  @NotNull
  @Enumerated(EnumType.STRING)
  public AttachmentType type;

  @NotNull
  @Column(nullable = false)
  public Blob file;

  @ManyToOne(optional = true)
  @JoinColumn(name = "office_id")
  public Office office;

  @Transient
  public long getLength() {
    return file == null ? 0 : file.length();
  }

  @PreRemove
  private void onDelete() {
    this.file.getFile().delete();
  }
}
