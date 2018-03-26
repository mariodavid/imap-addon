package com.haulmont.components.imap.api.scheduling;

import com.haulmont.components.imap.core.FolderTask;
import com.haulmont.components.imap.core.MsgHeader;
import com.haulmont.components.imap.dto.ImapFolderDto;
import com.haulmont.components.imap.entity.ImapEventType;
import com.haulmont.components.imap.entity.ImapFolder;
import com.haulmont.components.imap.entity.ImapMailBox;
import com.haulmont.components.imap.entity.ImapMessage;
import com.haulmont.components.imap.events.BaseImapEvent;
import com.haulmont.components.imap.events.EmailDeletedImapEvent;
import com.haulmont.components.imap.events.EmailMovedImapEvent;
import com.haulmont.cuba.core.EntityManager;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.search.MessageIDTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MissedMessagesInFolderTask extends ExistingMessagesInFolderTask {

    private Collection<ImapMessage> missedMsgs = new ArrayList<>();
    private final Collection<IMAPFolder> otherFolders;

    MissedMessagesInFolderTask(ImapMailBox mailBox, ImapFolder cubaFolder, IMAPFolder folder, ImapScheduling scheduling, Collection<ImapFolderDto> allFolders) {
        super(mailBox, cubaFolder, folder, scheduling);
        otherFolders = allFolders.stream()
                .flatMap(f -> folderWithChildren(f).stream())
                .filter(f -> Boolean.TRUE.equals(f.getCanHoldMessages()) && !f.getName().equals(cubaFolder.getName()))
                .map(ImapFolderDto::getImapFolder)
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Missed messages task will use folder {} to trigger MOVE event",
                    otherFolders.stream().map(IMAPFolder::getFullName).collect(Collectors.toList())
            );
        }
    }

    @Override
    protected List<BaseImapEvent> handleMessage(EntityManager em, ImapMessage msg, Map<Long, MsgHeader> msgsByUid) {
        MsgHeader newMsgHeader = msgsByUid.get(msg.getMsgUid());
        if (newMsgHeader == null) {
            missedMsgs.add(msg);
            em.remove(msg);
        }
        return Collections.emptyList();
    }

    @Override
    protected String taskDescription() {
        return "moved and deleted messages";
    }

    @Override
    protected Collection<BaseImapEvent> trailEvents() {
        return handleMissedMsgs();
    }

    private List<BaseImapEvent> handleMissedMsgs() {
        if (missedMsgs.isEmpty()) {
            return Collections.emptyList();
        }

        if (!cubaFolder.hasEvent(ImapEventType.EMAIL_MOVED) && !cubaFolder.hasEvent(ImapEventType.EMAIL_DELETED)) {
            return Collections.emptyList();
        }
        log.debug("Handle missed messages {} for folder {}", missedMsgs, cubaFolder);

        List<BaseImapEvent> result = new ArrayList<>(missedMsgs.size());

        missedMsgs.stream()
                .filter(msg -> msg.getMessageId() == null)
                .map(EmailDeletedImapEvent::new)
                .forEach(result::add);
        if (!result.isEmpty()) {
            log.debug("messages {} don't contain Message-ID header, they will be treated as deleted", result);
        }
        Map<String, ImapMessage> messagesByIds = missedMsgs.stream()
                .filter(msg -> msg.getMessageId() != null)
                .collect(Collectors.toMap(ImapMessage::getMessageId, Function.identity()));

        //todo: we need this set to handle move event properly to right folder (in Gmail we have 'All Mails' folder which contains all messages, probably better to add parameter for such folder in mailbox or more general folders param to exclude them from move event flow)
        Set<String> movedIds = new HashSet<>();

        for (IMAPFolder otherFolder : otherFolders) {
            if (messagesByIds.isEmpty()) {
                break;
            }

            Collection<String> ids = scheduling.imapHelper.doWithFolder(
                    mailBox,
                    otherFolder,
                    new FolderTask<>(
                            "fetch messages from folder " + otherFolder.getFullName() + " by message ids " + messagesByIds.keySet(),
                            true,
                            true,
                            f -> {
                                IMAPMessage[] messages = (IMAPMessage[]) f.search(new OrTerm(
                                        messagesByIds.keySet().stream().map(MessageIDTerm::new).toArray(SearchTerm[]::new))
                                );
                                Collection<String> messagesIds = new ArrayList<>(messages.length);
                                for (IMAPMessage message : messages) {
                                    messagesIds.add(message.getMessageID());
                                }

                                return messagesIds;
                            }
                    )
            );

            if (ids.isEmpty()) {
                continue;
            }

            if (otherFolder.getFullName().equals(mailBox.getTrashFolderName())) {
                ids.forEach(id ->
                        result.add(new EmailDeletedImapEvent(messagesByIds.remove(id)))
                );
            } else {
                ids.forEach(id -> {
                    movedIds.add(id);
                    result.add(new EmailMovedImapEvent(messagesByIds.get(id), otherFolder.getFullName()));
                });
            }
        }

        log.debug("Handle missed messages for folder {}. Moved: {}, deleted: {}", cubaFolder, movedIds, messagesByIds);

        movedIds.forEach(messagesByIds::remove);
        result.addAll(messagesByIds.values().stream().map(EmailDeletedImapEvent::new).collect(Collectors.toList()));

        if (!cubaFolder.hasEvent(ImapEventType.EMAIL_MOVED)) {
            result.removeIf(event -> event instanceof EmailMovedImapEvent);
        }
        if (!cubaFolder.hasEvent(ImapEventType.EMAIL_DELETED)) {
            result.removeIf(event -> event instanceof EmailDeletedImapEvent);
        }

        return result;
    }

    private static Collection<ImapFolderDto> folderWithChildren(ImapFolderDto dto) {
        Collection<ImapFolderDto> result = new ArrayList<>();
        result.add(dto);
        result.addAll(dto.getChildren().stream()
                .flatMap(f -> folderWithChildren(f).stream())
                .collect(Collectors.toList())
        );

        return result;
    }


}