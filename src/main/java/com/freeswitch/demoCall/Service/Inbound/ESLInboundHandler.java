package com.freeswitch.demoCall.Service.Inbound;

import com.freeswitch.demoCall.Service.Outbound.EventSocketAPI;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.springframework.context.ApplicationContext;


public class ESLInboundHandler {
    private static boolean hasHungUp = false;
    private static Client inboundClient;

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

    public void disconnect() {
        CommandResponse commandResponse = inboundClient.close();
        System.out.println(commandResponse);
    }
    private void startEslInboundListener(Client inboudClient) {
        try {
            inboundClient.connect("localhost", 8021, "ClueCon", 10);
            System.out.println("Connect successful");
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
        System.out.println("Channel answered: " + callUUID);
    }

    private static void handleHangupComplete(EslEvent event) {
        String response = event.getEventHeaders().get("variable_current_application_response");
        String callee = event.getEventHeaders().get("variable_dialed_user");
        String caller = event.getEventHeaders().get("variable_effective_caller_id_number");
//        System.out.println(event.getEventHeaders());
        String hangupCause = event.getEventHeaders().get("Hangup-Cause");
        System.out.println("Call hangup complete, Response: " + response + ", Hangup Cause: " + hangupCause);
        if(caller == null){
            if (!hasHungUp && callee.equals("1000")) {
                hasHungUp = true;
                inboundClient.sendAsyncApiCommand("conference ringmeCall dial user/1002", "");
                System.out.println("Successfully hung up");
            }
            hasHungUp = false;
        }
    }

}
