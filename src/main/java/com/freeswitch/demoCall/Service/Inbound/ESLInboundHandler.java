package com.freeswitch.demoCall.Service.Inbound;

import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.springframework.context.ApplicationContext;

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
        inboundClient.sendAsyncApiCommand("conference", " ringmeCall dial user/" + callee);
    }

    public void disconnect() {
        CommandResponse commandResponse = inboundClient.close();
        System.out.println(commandResponse);
    }
    private void startEslInboundListener(Client inboudClient) {
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
            inboudClient.setEventSubscriptions("plain", "all");
        } catch (Exception t) {
            System.out.println(t.getMessage());
        }
    }

    private static void handleChannelAnswer(EslEvent event) {
        String callUUID = event.getEventHeaders().get("Channel-Call-UUID");
//        System.out.println("Channel answered: " + callUUID);
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
