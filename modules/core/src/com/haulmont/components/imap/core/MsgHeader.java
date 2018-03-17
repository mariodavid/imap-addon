package com.haulmont.components.imap.core;

import javax.mail.Flags;

public class MsgHeader {
    private Long uid;
    private Flags flags;
    private String caption;
    private String refId;
    private Long threadId;

    public MsgHeader() {
    }

    public MsgHeader(Long uid, Flags flags, String caption) {
        this.uid = uid;
        this.flags = flags;
        this.caption = caption;
    }

    public MsgHeader(Long uid, Flags flags, String caption, String refId, Long threadId) {
        this.uid = uid;
        this.flags = flags;
        this.caption = caption;
        this.refId = refId;
        this.threadId = threadId;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Flags getFlags() {
        return flags;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }
}
