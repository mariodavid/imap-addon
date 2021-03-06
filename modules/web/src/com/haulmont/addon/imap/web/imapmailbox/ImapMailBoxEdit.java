package com.haulmont.addon.imap.web.imapmailbox;

import com.haulmont.addon.imap.entity.*;
import com.haulmont.addon.imap.service.ImapAPIService;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.addon.imap.entity.ImapAuthenticationMethod;
import com.haulmont.addon.imap.entity.ImapSimpleAuthentication;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.addon.imap.entity.ImapMailBox;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ImapMailBoxEdit extends AbstractEditor<ImapMailBox> {

    private final static Logger LOG = LoggerFactory.getLogger(ImapMailBoxEdit.class);

    @Inject
    private FieldGroup mainParams;

    @Inject
    private FieldGroup pollingParams;

    @Inject
    private FieldGroup proxyParams;

    @Inject
    private Button selectTrashFolderButton;

    @Inject
    private CheckBox useTrashFolderChkBox;

    @Inject
    private CheckBox useProxyChkBox;

    @Inject
    private ImapAPIService service;

    @Inject
    private Metadata metadata;

    @Inject
    private Datasource<ImapMailBox> mailBoxDs;

    @Inject
    private CollectionDatasource<ImapFolder, UUID> foldersDs;

    @Inject
    private DataManager dm;

    public void checkTheConnection() {
        try {
            service.testConnection(getItem());
            showNotification("Connection succeed", NotificationType.HUMANIZED);
        } catch (Exception e) {
            showNotification("Connection failed", NotificationType.ERROR);
        }
    }

    public void selectTrashFolder() {
        ImapMailBox mailBox = getItem();
        Boolean newEntity = mailBox.getNewEntity();
        LOG.debug("Open trash folder window for {} with newFlag {}", mailBox, newEntity);
        AbstractEditor selectFolders = openEditor(
                "imapcomponent$ImapMailBox.trashFolder",
                mailBox,
                WindowManager.OpenType.THIS_TAB,
                ParamsMap.of("mailBox", mailBox),
                mailBoxDs
        );
        selectFolders.addCloseWithCommitListener(() -> getItem().setNewEntity(newEntity));
    }

    public void selectFolders() {
        ImapMailBox mailBox = getItem();
        Boolean newEntity = mailBox.getNewEntity();
        LOG.debug("Open select folders window for {} with newFlag {}", mailBox, newEntity);
        AbstractEditor selectFolders = openEditor(
                "imapcomponent$ImapMailBox.folders",
                mailBox,
                WindowManager.OpenType.THIS_TAB,
                ParamsMap.of("mailBox", mailBox),
                mailBoxDs
        );
        selectFolders.addCloseWithCommitListener(() -> {
            foldersDs.refresh();
            getItem().setNewEntity(newEntity);
        });
    }

    @Override
    protected void initNewItem(ImapMailBox item) {
        item.setAuthenticationMethod(ImapAuthenticationMethod.SIMPLE);
        item.setPollInterval(10 * 60);
        item.setAuthentication(metadata.create(ImapSimpleAuthentication.class));
        item.setNewEntity(true);
    }

    @Override
    protected void postInit() {
        setCertificateVisibility();
        setTrashFolderVisibility();
        setProxyVisibility();

        addCloseWithCommitListener(() -> {
            ImapMailBox mailBox = getItem();

            List<ImapFolder> toCommit = mailBox.getFolders().stream().filter(PersistenceHelper::isNew).collect(Collectors.toList());
            List<ImapFolder> toDelete = dm.loadList(LoadContext.create(ImapFolder.class).setQuery(
                    LoadContext.createQuery(
                            "select f from imapcomponent$ImapFolder f where f.mailBox.id = :boxId"
                    ).setParameter("boxId", mailBox))).stream()
                    .filter(f -> !mailBox.getFolders().contains(f))
                    .collect(Collectors.toList());

            LOG.debug("Populate persistence context for {} (isNew: {}) with folders to save {} and delete {}",
                    mailBox, mailBox.getNewEntity(), toCommit, toDelete);

            if (Boolean.TRUE.equals(mailBox.getNewEntity())) {
                getDsContext().addAfterCommitListener((context, e) -> {
                    context.getCommitInstances().addAll(toCommit);
                    context.getRemoveInstances().addAll(toDelete);
                });
            } else {
                dm.commit(new CommitContext(toCommit, toDelete));
            }
        });
    }

    private void setCertificateVisibility() {
        FieldGroup.FieldConfig mailBoxRootCertificateField = this.mainParams.getFieldNN("mailBoxRootCertificateField");
        mailBoxRootCertificateField.setVisible(getItem().getSecureMode() != null);
        mailBoxDs.addItemPropertyChangeListener(event -> {
            if (Objects.equals("secureMode", event.getProperty())) {
                mailBoxRootCertificateField.setVisible(event.getValue() != null);
            }
        });
    }

    private void setTrashFolderVisibility() {
        FieldGroup.FieldConfig trashFolderNameField = this.pollingParams.getFieldNN("trashFolderNameField");
        boolean visible = getItem().getTrashFolderName() != null;
        LOG.debug("Set visibility of trash folder controls for {} to {}", getItem(), visible);
        trashFolderNameField.setVisible(visible);
        selectTrashFolderButton.setVisible(visible);
        useTrashFolderChkBox.setValue(visible);

        useTrashFolderChkBox.addValueChangeListener(e -> {
            boolean newVisible = Boolean.TRUE.equals(e.getValue());
            LOG.debug("Set visibility of trash folder controls for {} to {}", getItem(), visible);
            trashFolderNameField.setVisible(newVisible);
            selectTrashFolderButton.setVisible(newVisible);

            if (!newVisible) {
                getItem().setTrashFolderName(null);
            }
        });
    }

    private void setProxyVisibility() {
        FieldGroup.FieldConfig proxyHostField = this.proxyParams.getFieldNN("proxyHostField");
        FieldGroup.FieldConfig proxyPortField = this.proxyParams.getFieldNN("proxyPortField");
        FieldGroup.FieldConfig webProxyChkBox = this.proxyParams.getFieldNN("webProxyChkBox");
        boolean visible = getItem().getProxy() != null;
        LOG.debug("Set visibility of proxy controls for {} to {}", getItem(), visible);
        proxyHostField.setVisible(visible);
        proxyHostField.setRequired(visible);
        proxyPortField.setVisible(visible);
        proxyPortField.setRequired(visible);
        webProxyChkBox.setVisible(visible);
        useProxyChkBox.setValue(visible);
        proxyParams.setVisible(visible);
        proxyParams.getParent().setVisible(visible);

        useProxyChkBox.addValueChangeListener(e -> {
            boolean newVisible = Boolean.TRUE.equals(e.getValue());
            LOG.debug("Set visibility of proxy folder controls for {} to {}", getItem(), visible);
            if (!newVisible) {
                getItem().setProxy(null);
            } else {
                getItem().setProxy(metadata.create(ImapProxy.class));
            }
            proxyHostField.setVisible(newVisible);
            proxyHostField.setRequired(newVisible);
            proxyPortField.setVisible(newVisible);
            proxyPortField.setRequired(newVisible);
            webProxyChkBox.setVisible(newVisible);
            proxyParams.setVisible(newVisible);
            proxyParams.getParent().setVisible(newVisible);
        });
    }

}