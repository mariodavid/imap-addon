package com.haulmont.addon.imap.api.scheduling;

import com.haulmont.addon.imap.core.FolderTask;
import com.haulmont.addon.imap.core.MsgHeader;
import com.haulmont.addon.imap.entity.ImapFolder;
import com.haulmont.addon.imap.entity.ImapMailBox;
import com.haulmont.addon.imap.entity.ImapMessage;
import com.haulmont.addon.imap.events.BaseImapEvent;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Transaction;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.MessagingException;
import java.util.*;
import java.util.stream.Collectors;

abstract class ExistingMessagesInFolderTask extends AbstractFolderTask {

    ExistingMessagesInFolderTask(ImapMailBox mailBox, ImapFolder cubaFolder, IMAPFolder folder, ImapScheduling scheduling) {
        super(mailBox, cubaFolder, folder, scheduling);
    }

    @Override
    List<BaseImapEvent> makeEvents() {
        int batchSize = scheduling.config.getUpdateBatchSize();
        long windowSize = Math.min(
                getCount(),
                (mailBox.getUpdateSliceSize() != null) ? Math.max(mailBox.getUpdateSliceSize(), batchSize) : batchSize
        );
        LOG.debug("[{} for {}]handle events for existing messages using windowSize {} and batchSize {}",
                taskDescription(), cubaFolder, windowSize, batchSize);
        return scheduling.imapHelper.doWithFolder(
                mailBox,
                folder,
                new FolderTask<>(
                        taskDescription(),
                        true,
                        true,
                        f -> {
                            List<BaseImapEvent> modificationEvents = new ArrayList<>((int) windowSize);
                            for (int i = 0; i < windowSize; i += batchSize) {
                                int thisBatchSize = (int) Math.min(batchSize, windowSize - i);
                                LOG.trace("[{} for {}]handle batch#{} with size {}",
                                        taskDescription(), cubaFolder, i, thisBatchSize);
                                modificationEvents.addAll(handleBatch(batchSize));
                            }

                            modificationEvents.addAll(trailEvents());

                            return modificationEvents;
                        }
                )
        );
    }

    private long getCount() {
        scheduling.authentication.begin();
        try (Transaction tx = scheduling.persistence.createTransaction()) {
            EntityManager em = scheduling.persistence.getEntityManager();
            return ((Number) em.createQuery("select count(m.id) from imapcomponent$ImapMessage m where m.folder.id = :mailFolderId")
                    .setParameter("mailFolderId", cubaFolder)
                    .getSingleResult()).longValue();
        } finally {
            scheduling.authentication.end();
        }
    }

    private List<BaseImapEvent> handleBatch(int count) throws MessagingException {
        scheduling.authentication.begin();
        try (Transaction tx = scheduling.persistence.createTransaction()) {
            EntityManager em = scheduling.persistence.getEntityManager();

            List<ImapMessage> messages = em.createQuery(
                    "select m from imapcomponent$ImapMessage m where m.folder.id = :mailFolderId order by m.updateTs asc nulls first",
                    ImapMessage.class
            )
                    .setParameter("mailFolderId", cubaFolder)
                    .setMaxResults(count)
                    .setViewName("imap-msg-full")
                    .getResultList();

            List<MsgHeader> imapMessages = scheduling.imapHelper.getAllByUids(
                    folder, messages.stream().mapToLong(ImapMessage::getMsgUid).toArray()
            );
            LOG.trace("[{} for {}]batch messages from db: {}, from IMAP server: {}",
                    taskDescription(), cubaFolder, messages, imapMessages);

            Map<Long, MsgHeader> headersByUid = new HashMap<>(imapMessages.size());
            for (MsgHeader msg : imapMessages) {
                headersByUid.put(msg.getUid(), msg);
            }

            List<BaseImapEvent> events = messages.stream()
                    .flatMap(msg -> handleMessage(em, msg, headersByUid).stream())
                    .collect(Collectors.toList());
            tx.commit();

            return events;

        } finally {
            scheduling.authentication.end();
        }

    }

    protected abstract List<BaseImapEvent> handleMessage(EntityManager em, ImapMessage msg, Map<Long, MsgHeader> msgsByUid);

    protected abstract String taskDescription();

    protected Collection<BaseImapEvent> trailEvents() {
        return Collections.emptyList();
    }
}
