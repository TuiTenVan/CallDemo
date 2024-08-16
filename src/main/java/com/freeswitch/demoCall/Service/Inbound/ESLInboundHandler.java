package com.freeswitch.demoCall.Service.Inbound;

import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class ESLInboundHandler {
    private Client inboundClient;

    public ESLInboundHandler(String ip, ApplicationContext context) {
        try {
            inboundClient = new Client();
            startEslInboundListener(inboundClient);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean isConnected() {
        return inboundClient.canSend();
    }

    public void reconnect() {
        try {
            inboundClient = new Client();
            startEslInboundListener(inboundClient);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void transfer(String callee) {
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall kick last");
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall dial user/" + callee);
    }

    public void takeOver() {
        // Đá nhân viên hiện tại ra khỏi cuộc gọi
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall kick last");
        // Quản lý tự động tham gia vào cuộc gọi
        String managerExtension = "1003";
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall dial user/" + managerExtension);
    }

    public void disconnect() {
        CommandResponse commandResponse = inboundClient.close();
        System.out.println(commandResponse);
    }

    private void startEslInboundListener(Client inboundClient) {
        try {
            inboundClient.connect("localhost", 8021, "ClueCon", 10);
            inboundClient.addEventFilter("Event-Name", "CHANNEL_HANGUP_COMPLETE");
            inboundClient.addEventFilter("Event-Name", "CHANNEL_ANSWER");
            inboundClient.addEventListener(new IEslEventListener() {
                @Override
                public void eventReceived(EslEvent event) {
                    String eventName = event.getEventName();
                    System.out.println(eventName);
                    switch (eventName) {
                        case "CHANNEL_ANSWER":
                            handleChannelAnswer(event);
                            break;
                        case "CHANNEL_HANGUP_COMPLETE":
                            handleHangupComplete(event);
                            break;
                        default:
                            System.out.println("Unhandled event: " + eventName);
                            break;
                    }
                }

                @Override
                public void backgroundJobResultReceived(EslEvent event) {
                    String jobUuid = event.getEventHeaders().get("Job-UUID");
                    System.out.println("Background job result received: " + jobUuid);
                }
            });
            inboundClient.setEventSubscriptions("plain", "all");
        } catch (Exception t) {
            System.out.println(t.getMessage());
        }
    }

    private void handleChannelAnswer(EslEvent event) {
        String callerId = event.getEventHeaders().get("Caller-Caller-ID-Number");

        if ("1003".equals(callerId)) {
            System.out.println("User 1003 is joining the conference. Kicking out user 1000...");
            kickUserFromConference("ringmeCall", "1000");
        }
    }

    public void kickUserFromConference(String conferenceName, String userId) {
        EslMessage response = inboundClient.sendSyncApiCommand("conference", conferenceName + " list");

        List<String> lines = response.getBodyLines();
        String memberId = null;

        for (String line : lines) {
            if (line.contains(userId)) {
                // Extract the member ID from the line, assuming it's the first field
                memberId = line.split(";")[0];
                break;
            }
        }

        if (memberId != null) {
            // Now kick the user with the extracted memberId
            inboundClient.sendAsyncApiCommand("conference", conferenceName + " kick " + memberId);
            System.out.println("Kicked user with ID: " + memberId);
        } else {
            System.out.println("User with ID " + userId + " not found in conference " + conferenceName);
        }
    }

    private void handleHangupComplete(EslEvent event) {
        String callee = event.getEventHeaders().get("Caller-Destination-Number");
        System.out.println(callee);
        if (callee != null) {
            if (!callee.equals("1000")) {
                inboundClient.sendAsyncApiCommand("conference", "ringmeCall kick all");
            }
        }
    }
}
