package com.haulmont.addon.imap.core;

import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.FetchItem;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.util.MailLogger;

import javax.mail.FetchProfile;
import java.io.IOException;
import java.util.Properties;

class ThreadExtension {

    static final String ITEM_NAME = "X-GM-THRID";
    static final CubaProfileItem THREAD_ID_ITEM = new CubaProfileItem(ITEM_NAME);

    static class CubaIMAPProtocol extends IMAPProtocol {

        public CubaIMAPProtocol(String name, String host, int port, Properties props,
                                boolean isSSL, MailLogger logger) throws IOException, ProtocolException {
            super(name, host, port, props, isSSL, logger);
        }
        @Override
        public FetchItem[] getFetchItems() {
            return new FetchItem[] {
                    new FetchItem(ITEM_NAME, THREAD_ID_ITEM) {
                        @Override
                        public Object parseItem(FetchResponse r) throws ParsingException {
                            return new X_GM_THRID(r);
                        }
                    }
            };
        }

    }

    static class X_GM_THRID implements Item {

        private static final char[] NAME_CHARS = {'X','-','G','M','-','T','H','R','I','D'};
        public int seqnum;

        public long x_gm_thrid;

        public X_GM_THRID(FetchResponse r) throws ParsingException {
            seqnum = r.getNumber();
            r.skipSpaces();
            x_gm_thrid = r.readLong();
        }
    }

    static class CubaProfileItem extends FetchProfile.Item {
        protected CubaProfileItem(String name) {
            super(name);
        }
    }
}
