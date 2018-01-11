-- begin MAILCOMPONENT_MAIL_FOLDER
alter table MAILCOMPONENT_MAIL_FOLDER add constraint FK_MAILCOMPONENT_MAIL_FOLDER_MAIL_BOX foreign key (MAIL_BOX_ID) references MAILCOMPONENT_MAIL_BOX(ID)^
create index IDX_MAILCOMPONENT_MAIL_FOLDER_MAIL_BOX on MAILCOMPONENT_MAIL_FOLDER (MAIL_BOX_ID)^
-- end MAILCOMPONENT_MAIL_FOLDER
-- begin MAILCOMPONENT_MAIL_BOX
alter table MAILCOMPONENT_MAIL_BOX add constraint FK_MAILCOMPONENT_MAIL_BOX_ROOT_CERTIFICATE foreign key (ROOT_CERTIFICATE_ID) references SYS_FILE(ID)^
alter table MAILCOMPONENT_MAIL_BOX add constraint FK_MAILCOMPONENT_MAIL_BOX_CLIENT_CERTIFICATE foreign key (CLIENT_CERTIFICATE_ID) references SYS_FILE(ID)^
alter table MAILCOMPONENT_MAIL_BOX add constraint FK_MAILCOMPONENT_MAIL_BOX_AUTHENTICATION foreign key (AUTHENTICATION_ID) references MAILCOMPONENT_MAIL_SIMPLE_AUTHENTICATION(ID)^
-- end MAILCOMPONENT_MAIL_BOX
-- begin MAILCOMPONENT_MAIL_EVENT_TYPE
create unique index IDX_MAILCOMPONENT_MAIL_EVENT_TYPE_UNIQ_NAME on MAILCOMPONENT_MAIL_EVENT_TYPE (NAME) ^
-- end MAILCOMPONENT_MAIL_EVENT_TYPE
-- begin MAILCOMPONENT_MAIL_FOLDER_MAIL_EVENT_TYPE_LINK
alter table MAILCOMPONENT_MAIL_FOLDER_MAIL_EVENT_TYPE_LINK add constraint FK_MAIFOLMAIEVETYP_MAIL_FOLDER foreign key (MAIL_FOLDER_ID) references MAILCOMPONENT_MAIL_FOLDER(ID)^
alter table MAILCOMPONENT_MAIL_FOLDER_MAIL_EVENT_TYPE_LINK add constraint FK_MAIFOLMAIEVETYP_MAIL_EVENT_TYPE foreign key (MAIL_EVENT_TYPE_ID) references MAILCOMPONENT_MAIL_EVENT_TYPE(ID)^
-- end MAILCOMPONENT_MAIL_FOLDER_MAIL_EVENT_TYPE_LINK
