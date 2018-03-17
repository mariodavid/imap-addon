package com.haulmont.components.imap.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import com.haulmont.cuba.core.entity.StandardEntity;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.chile.core.annotations.NamePattern;
import java.util.Date;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@NamePattern("№%s: %s|orderNumber,name")
@Table(name = "IMAPCOMPONENT_IMAP_MESSAGE_ATTACHMENT_REF")
@Entity(name = "imapcomponent$ImapMessageAttachmentRef")
public class ImapMessageAttachmentRef extends BaseUuidEntity {
    private static final long serialVersionUID = -1046407519479636529L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IMAP_MESSAGE_REF_ID")
    protected ImapMessageRef imapMessageRef;

    @Temporal(TemporalType.TIME)
    @NotNull
    @Column(name = "CREATED_TS", nullable = false)
    protected Date createdTs;

    @NotNull
    @Column(name = "ORDER_NUMBER", nullable = false)
    protected Integer orderNumber;

    @NotNull
    @Column(name = "NAME", nullable = false)
    protected String name;

    @NotNull
    @Column(name = "FILE_SIZE", nullable = false)
    protected Long fileSize;

    public void setCreatedTs(Date createdTs) {
        this.createdTs = createdTs;
    }

    public Date getCreatedTs() {
        return createdTs;
    }


    public void setImapMessageRef(ImapMessageRef imapMessageRef) {
        this.imapMessageRef = imapMessageRef;
    }

    public ImapMessageRef getImapMessageRef() {
        return imapMessageRef;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileSize() {
        return fileSize;
    }


}