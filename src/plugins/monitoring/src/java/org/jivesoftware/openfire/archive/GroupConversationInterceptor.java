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

package org.jivesoftware.openfire.archive;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.MultiUserChatServer;
import org.picocontainer.Startable;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.Date;

/**
 * Interceptor of MUC events of the local conferencing service. The interceptor is responsible
 * for reacting to users joining and leaving rooms as well as messages being sent to rooms.
 *
 * @author Gaston Dombiak
 */
public class GroupConversationInterceptor implements MUCEventListener, Startable {


    private ConversationManager conversationManager;

    public GroupConversationInterceptor(ConversationManager conversationManager) {
        this.conversationManager = conversationManager;
    }

    public void roomCreated(JID roomJID) {
        //Do nothing
    }

    public void roomDestroyed(JID roomJID) {
        // Process this event in the senior cluster member or local JVM when not in a cluster
        if (ClusterManager.isSeniorClusterMember()) {
            conversationManager.roomConversationEnded(roomJID, new Date());
        }
        else {
            ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
            eventsQueue.addGroupChatEvent(conversationManager.getRoomConversationKey(roomJID),
                    ConversationEvent.roomDestroyed(roomJID, new Date()));
        }
    }

    public void occupantJoined(JID roomJID, JID user, String nickname) {
        // Process this event in the senior cluster member or local JVM when not in a cluster
        if (ClusterManager.isSeniorClusterMember()) {
            conversationManager.joinedGroupConversation(roomJID, user, nickname, new Date());
        }
        else {
            ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
            eventsQueue.addGroupChatEvent(conversationManager.getRoomConversationKey(roomJID),
                    ConversationEvent.occupantJoined(roomJID, user, nickname, new Date()));
        }
    }

    public void occupantLeft(JID roomJID, JID user) {
        // Process this event in the senior cluster member or local JVM when not in a cluster
        if (ClusterManager.isSeniorClusterMember()) {
            conversationManager.leftGroupConversation(roomJID, user, new Date());
            // If there are no more occupants then consider the group conversarion over
            MUCRoom mucRoom = XMPPServer.getInstance().getMultiUserChatServer().getChatRoom(roomJID.getNode());
            if (mucRoom != null &&  mucRoom.getOccupantsCount() == 0) {
                conversationManager.roomConversationEnded(roomJID, new Date());
            }
        }
        else {
            ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
            eventsQueue.addGroupChatEvent(conversationManager.getRoomConversationKey(roomJID),
                    ConversationEvent.occupantLeft(roomJID, user, new Date()));
        }
    }

    public void nicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) {
        // Process this event in the senior cluster member or local JVM when not in a cluster
        if (ClusterManager.isSeniorClusterMember()) {
            occupantLeft(roomJID, user);
            // Sleep 1 millisecond so that there is a delay between logging out and logging in
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Ignore
            }
            occupantJoined(roomJID, user, newNickname);
        }
        else {
            ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
            eventsQueue.addGroupChatEvent(conversationManager.getRoomConversationKey(roomJID),
                    ConversationEvent.nicknameChanged(roomJID, user, newNickname, new Date()));
        }
    }

    public void messageReceived(JID roomJID, JID user, String nickname, Message message) {
        // Process this event in the senior cluster member or local JVM when not in a cluster
        if (ClusterManager.isSeniorClusterMember()) {
            conversationManager.processRoomMessage(roomJID, user, nickname, message.getBody(), new Date());
        }
        else {
            boolean withBody = conversationManager.isRoomArchivingEnabled() && (
                    conversationManager.getRoomsArchived().isEmpty() ||
                            conversationManager.getRoomsArchived().contains(roomJID.getNode()));

            ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
            eventsQueue.addGroupChatEvent(conversationManager.getRoomConversationKey(roomJID),
                    ConversationEvent.roomMessageReceived(roomJID, user, nickname, withBody ? message.getBody() : null, new Date()));
        }
    }

    public void start() {
        XMPPServer.getInstance().getMultiUserChatServer().addListener(this);
    }

    public void stop() {
        MultiUserChatServer chatServer = XMPPServer.getInstance().getMultiUserChatServer();
        if (chatServer != null) {
            chatServer.removeListener(this);
        }
        conversationManager = null;
    }
}
