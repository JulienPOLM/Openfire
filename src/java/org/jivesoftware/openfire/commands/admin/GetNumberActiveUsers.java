/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2008 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution, or a commercial license
 * agreement with Jive.
 */

package org.jivesoftware.openfire.commands.admin;

import org.dom4j.Element;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.commands.AdHocCommand;
import org.jivesoftware.openfire.commands.SessionData;
import org.jivesoftware.openfire.session.ClientSession;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command that allows to retrieve the number of online users who are active at any one moment.
 * Active users are those users that have sent an available presence.
 *
 * @author Gaston Dombiak
 */
public class GetNumberActiveUsers extends AdHocCommand {

    protected void addStageInformation(SessionData data, Element command) {
        //Do nothing since there are no stages
    }

    public void execute(SessionData data, Element command) {
        DataForm form = new DataForm(DataForm.Type.result);

        FormField field = form.addField();
        field.setType(FormField.Type.hidden);
        field.setVariable("FORM_TYPE");
        field.addValue("http://jabber.org/protocol/admin");

        field = form.addField();
        field.setLabel(getLabel());
        field.setVariable("activeusersnum");
        // Make sure that we are only counting based on bareJIDs and not fullJIDs
        Collection<ClientSession> sessions = SessionManager.getInstance().getSessions();
        Set<String> users = new HashSet<String>(sessions.size());
        for (ClientSession session : sessions) {
            if (session.getPresence().isAvailable()) {
                users.add(session.getAddress().toBareJID());
            }
        }
        field.addValue(users.size());

        command.add(form.getElement());
    }

    protected List<Action> getActions(SessionData data) {
        //Do nothing since there are no stages
        return null;
    }

    public String getCode() {
        return "http://jabber.org/protocol/admin#get-active-users-num";
    }

    public String getDefaultLabel() {
        // TODO Use i18n
        return "Number of Active Users";
    }

    protected Action getExecuteAction(SessionData data) {
        //Do nothing since there are no stages
        return null;
    }

    public int getMaxStages(SessionData data) {
        return 0;
    }
}
