package com.freeswitch.demoCall.Service.Inbound;

import com.freeswitch.demoCall.model.CallCDR;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.freeswitch.esl.client.transport.CommandResponse;
import org.springframework.context.ApplicationContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ESLInboundHandler {

    private final Logger logger = LogManager.getLogger(ESLInboundHandler.class);

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
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall dial user/" + callee);
        inboundClient.sendAsyncApiCommand("conference", "ringmeCall kick last");
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
                    switch (eventName) {
                        case "CHANNEL_ANSWER":
                            handleChannelAnswer(event);
                            break;
                        case "CHANNEL_HANGUP_COMPLETE":
                            handleHangupComplete(event);
                            break;
                        default:
                            System.out.println("Unhandled event: " + eventName + " -> " + event.getEventHeaders().get("variable_sip_call_id"));
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

    private void handleChannelAnswer(EslEvent event) {
        String callUUID = event.getEventHeaders().get("Channel-Call-UUID");
//        System.out.println("Channel answered: " + callUUID);
//        printLog(event);
    }

    private void handleHangupComplete(EslEvent event) {
        printLog(event);
//        String callee = event.getEventHeaders().get("Caller-Destination-Number");
//        if (callee != null) {
//            if (!callee.equals("1000")) {
//                inboundClient.sendAsyncApiCommand("conference", "ringmeCall kick all");
//            }
//        }
    }



    private Map<String, String> getCDRFromESLEvent(EslEvent eslEvent) {
        Map<String, String> eventHeaders = eslEvent.getEventHeaders();
        Map<String, String> map = new HashMap<>();
        if (eventHeaders != null) {

            map.put("type_call", eventHeaders.get("Caller-Context"));
            map.put("ivr", eventHeaders.get("variable_ringme_call_ivr"));
            map.put("call_id", eventHeaders.get("variable_sip_call_id"));
            map.put("state", eventHeaders.get("Channel-Call-State"));

            map.put("sip_invite_failure_status", eventHeaders.get("variable_sip_invite_failure_status"));
            map.put("originate_failed_cause", eventHeaders.get("variable_originate_failed_cause"));

            map.put("answer-state", eventHeaders.get("Answer-State")); // hangup, answered
            map.put("hangup_cause", eventHeaders.get("variable_hangup_cause"));

            map.put("sip_hangup_phrase", eventHeaders.get("variable_sip_hangup_phrase"));
            map.put("status_code", eventHeaders.get("variable_sip_term_status"));

            long duration = (long) Math.ceil(Long.parseLong(eventHeaders.get("variable_billmsec")) / 1000d);
            map.put("duration", String.valueOf(duration));
            map.put("mduration", eventHeaders.get("variable_billmsec"));
            map.put("total-duration", eventHeaders.get("variable_duration"));
            map.put("wait-duration", eventHeaders.get("variable_duration"));
            map.put("time_invite", convertTimestampToStringDate(Long
                            .parseLong(eventHeaders.get("variable_start_epoch")) * 1000));
            map.put("time_answer", convertTimestampToStringDate(Long
                            .parseLong(eventHeaders.get("variable_answer_epoch")) * 1000));
            map.put("time_end", convertTimestampToStringDate(Long
                            .parseLong(eventHeaders.get("variable_end_epoch")) * 1000));

            if (map.get("type_call").equals("ctx_callout")) {

                if (eventHeaders.get("variable_ringme_origin_caller") != null) {

                    map.put("caller", eventHeaders.get("variable_ringme_origin_caller"));
                } else {
                    map.put("caller", eventHeaders.get("Caller-Caller-ID-Number"));
                }
                map.put("callee", eventHeaders.get("Caller-Callee-ID-Number"));
                if (map.get("callee") == null) {
                    String[] arr = eventHeaders.get("Caller-Destination-Number")
                            .replace("no_record_", "")
                            .replace("record_", "")
                            .split("_");
                    if (arr.length > 0) {

                        map.put("callee", arr[0]);
                    }
                }
                map.put("mnpFrom", eventHeaders.get("variable_ringme_telecom"));
                if (eventHeaders.get("variable_sip_req_user") != null) {
                    String[] arr = eventHeaders.get("variable_sip_req_user").split("_");
                    map.put("mnpTo", arr[arr.length - 1]);
                }
                map.put("hotline", eventHeaders.get("Caller-Caller-ID-Number"));

                map.put("bridge_hangup_cause", eventHeaders.get("variable_bridge_hangup_cause"));

                if (map.get("hangup_cause").equals("MEDIA_TIMEOUT")) {
                    logger.info("CALLOUT|" + map.get("caller") + "|" + map.get("callee") + "|" +
                            map.get("call_id") + "|MEDIA_TIMEOUT|" + map.get("duration"));

                    if(map.get("duration") != null && Long.parseLong(map.get("duration")) == 1) {
                        map.put("duration", String.valueOf(0));
                    }
                } else {

                    String timeAccept = null ;
                    if (timeAccept != null) {
                        long mdurationRedis = (Long.parseLong(eventHeaders.get("variable_end_epoch")) * 1000) - Long.parseLong(timeAccept);
                        long durationRedis = (long) Math.ceil(mdurationRedis / 1000d);
                        if (durationRedis > Long.parseLong(map.get("duration"))) {
                            logger.info("CHANGE DURATION FROM REDIS:callId={} | duration old={} | timeAccept old={}",
                                    map.get("call_id"), map.get("duration"), map.get("time_answer"));
//                            map.put("time_answer", Utils.convertTimestampToStringDate(Long.parseLong(timeAccept)));
                            map.put("mduration_redis", String.valueOf(mdurationRedis));
                            map.put("duration", String.valueOf(durationRedis));

                            if (durationRedis == 60) {
                                logger.info("CALLOUT|" + map.get("caller") + "|" + map.get("callee") + "|" + map.get("call_id") + "|DURATION=60|");
                            }
                        } else {
//                            logger.info("NOT CHANGE DURATION REDIS|{}|{}|{}", map.get("call_id"), mdurationRedis, Utils.convertTimestampToStringDate(Long.parseLong(timeAccept)));
                        }
                    }
                }
            } else {
//                map.put("call_id", eventHeaders.get("Unique-ID"));
                map.put("caller", eventHeaders.get("Caller-Orig-Caller-ID-Number"));
                map.put("callee", eventHeaders.get("variable_ringme_dest_callee"));
                map.put("mnpFrom", eventHeaders.get("variable_ringme_telecom"));
                map.put("hotline", eventHeaders.get("Caller-Callee-ID-Number"));

                if (map.get("hotline") == null) {
                    map.put("hotline", eventHeaders.get("Caller-Destination-Number"));
                }
                if (map.get("hotline") != null) {

//                    map.put("mnpTo", getMnpToCallin(map.get("hotline")));
                } else {
//                    printLog(eslEvent);
                    map.put("mnpTo", "variable_ringme_telecom");
                }
                map.put("bridge_hangup_cause", eventHeaders.get("variable_hangup_cause"));
            }

            Map<String, String> customs = convertCustomStatus(map);
            map.putAll(customs);
            if (customs.get("status") != null && customs.get("status").equals("answered")) {
                if (map.get("ivr") != null) {
                    if (eventHeaders.get("variable_execute_on_answer") != null &&
                            eventHeaders.get("variable_execute_on_answer").contains("record_session")) {

                        int startIndex = eventHeaders.get("variable_execute_on_answer").indexOf("/call-record");
                        int endIndex = eventHeaders.get("variable_execute_on_answer").indexOf(".wav");
                        String path = eventHeaders.get("variable_execute_on_answer").substring(startIndex + 12, endIndex + 4);
//                        map.put("domain_record", configuration.getUatDomain());
//                        map.put("link_record", configuration.getApi2Prefix() + path);
                    }
                } else if (eventHeaders.get("variable_last_arg") != null &&
                        (eventHeaders.get("variable_last_arg").contains("record_session") ||
                                eventHeaders.get("variable_last_arg").endsWith(".wav"))) {
                    // record link
//                    map.putAll(getLinkRecord(eventHeaders));
                }
                long waitDuration = Long.parseLong(map.get("total-duration")) - Long.parseLong(map.get("duration"));
                map.put("wait-duration", String.valueOf(waitDuration));
            }
            logger.info(map);
        }
        return map;
    }
    private Map<String, String> convertCustomStatus(Map<String, String> map) {
        Map<String, String> customs = new HashMap<>();
        String status = "", callStatus = "", callStatusCode = "",
                customCallStatus = "", customCallStatusCode = "", closedBy = "";
        boolean isEndCall = map.get("hangup_cause").equals("NORMAL_CLEARING") &&
                (map.get("type_call").equals("ctx_callin") ||
                        (map.get("bridge_hangup_cause") != null && map.get("bridge_hangup_cause").equals("NORMAL_CLEARING")));

        if (map.get("type_call").equals("ctx_callout")) {

            if ((map.get("hangup_cause").equals("NORMAL_UNSPECIFIED") || !isEndCall) &&
                    map.get("duration") != null && Long.parseLong(map.get("duration")) > 0) {
                logger.info("CALLOUT|" + map.get("caller") + "|" + map.get("callee") + "|" + map.get("call_id") +
                        "|CHANGE_OTHER_HANGUP_CAUSE|" + map.get("hangup_cause"));
                isEndCall = true;
            }

            if (isEndCall) {
                status = "answered";
                callStatus = "Callee Hangup";
                callStatusCode = "203";
                closedBy = "callee";

                customCallStatus = "answered";
                customCallStatusCode = "489";
            } else if (map.get("sip_invite_failure_status") != null) {
                status = "no-answered";
                if (map.get("hangup_cause").equals("ORIGINATOR_CANCEL")) {

                    callStatus = "Caller Cancel";
                    callStatusCode = "487";
                    closedBy = "caller";

                    customCallStatus = "noanswers";
                    customCallStatusCode = "487";
                } else if (map.get("sip_invite_failure_status").equals("480")) {
                    if (map.get("bridge_hangup_cause") != null && map.get("bridge_hangup_cause").equals("NO_ANSWER")) {

                        // callee cancel + timeout
                        callStatus = "Callee Cancel"; //chưa định nghĩa Callee Cancel cho vnpost
                        callStatusCode = "488";
                        closedBy = "callee";

                        customCallStatus = "noanswers";
                        customCallStatusCode = "487";
                    } else if (map.get("originate_failed_cause") != null &&
                            map.get("originate_failed_cause").equals("NO_ANSWER") && map.get("bridge_hangup_cause") == null) {

                        // busy
                        status = "busy";
                        callStatus = "Busy";
                        callStatusCode = "480";
                        closedBy = "callee";

                        customCallStatus = "busy";
                        customCallStatusCode = "486";
                    } else {

                        callStatus = "Unavailable";
                        callStatusCode = "491";
                        closedBy = "callee";

                        customCallStatus = "telerror";
                        customCallStatusCode = "491";
                    }
                } else if (map.get("sip_invite_failure_status").equals("403") ||
                        map.get("sip_invite_failure_status").equals("503")) {

                    callStatus = "Unavailable";
                    callStatusCode = "491";
                    closedBy = "callee";

                    customCallStatus = "telerror";
                    customCallStatusCode = "491";
                } else {
                    // callee hangup
                    callStatus = "Busy";
                    callStatusCode = "480";
                    closedBy = "callee";

                    customCallStatus = "busy";
                    customCallStatusCode = "486";
                }
            } else {
                if (map.get("bridge_hangup_cause") != null &&
                        map.get("bridge_hangup_cause").equals("ORIGINATOR_CANCEL")) {
                    // timeout
                    status = "no-answered";
                    callStatus = "Timeout";
                    callStatusCode = "488";
                    closedBy = "caller";

                    customCallStatus = "noanswers";
                    customCallStatusCode = "487";
                } else if (map.get("hangup_cause").equals("MEDIA_TIMEOUT")) {

                    if (map.get("duration") != null && Long.parseLong(map.get("duration")) >= 2) {

                        status = "answered";
                        callStatus = "Callee Hangup";
                        callStatusCode = "203";

                        customCallStatus = "answered";
                        customCallStatusCode = "489";

                        logger.info("CALLOUT|" + map.get("caller") + "|" + map.get("callee") + "|" + map.get("call_id") +
                                "|MEDIA_TIMEOUT|answered|" + map.get("duration"));
                    } else if (map.get("bridge_hangup_cause") != null &&
                            map.get("bridge_hangup_cause").equals("NORMAL_CLEARING")) {
                        status = "no-answered";
                        callStatus = map.get("hangup_cause");
                        callStatusCode = "499";

                        customCallStatus = "error";
                        customCallStatusCode = "494";
                    } else {
                        status = "no-answered";
                        callStatus = "Unavailable";
                        callStatusCode = "491";


                        customCallStatus = "telerror";
                        customCallStatusCode = "491";
                    }
                    closedBy = "callee";
                } else {

                    status = "no-answered";
                    callStatus = "Unavailable";
                    callStatusCode = "491";
                    closedBy = "callee";

                    customCallStatus = "telerror";
                    customCallStatusCode = "491";
                }
            }
        }
//        } else if (map.get("type_call").equals("ctx_callin")) {
//            Integer redisCallinCode = redisService.getRecentCallinCode(map.get("call_id"));
//
//            if (isEndCall) {
//                status = "answered";
//                if (redisCallinCode != null) {
//
//                    callStatus = "Callee Hangup";
//                    callStatusCode = "203";
//                    closedBy = "callee";
//
//                } else {
//
//                    callStatus = "Caller Hangup";
//                    callStatusCode = "204";
//                    closedBy = "caller";
//                }
//
//                customCallStatus = "answered";
//                customCallStatusCode = "602";
//            } else if (map.get("sip_invite_failure_status") != null) {
//                status = "no-answered";
//                if (map.get("hangup_cause").equals("ORIGINATOR_CANCEL")) {
//
//                    callStatus = "Caller Cancel";
//                    callStatusCode = "487";
//                    closedBy = "caller";
//
//                    customCallStatus = "noanswers";
//                    customCallStatusCode = "600";
//                } else if (redisCallinCode != null) {
//                    if (redisCallinCode == 486) {
//                        callStatus = "Busy";
//                        callStatusCode = "480";
//                        closedBy = "callee";
//
//                        customCallStatus = "busy";
//                        customCallStatusCode = "605";
//                    } else if (redisCallinCode == 488) {
//                        callStatus = "Callee Cancel";
//                        callStatusCode = "488";
//                        closedBy = "callee";
//
//                        customCallStatus = "noanswers";
//                        customCallStatusCode = "600";
//                    } else if (redisCallinCode == 489) {
//                        callStatus = "Timeout";
//                        callStatusCode = "489";
//                        closedBy = "callee";
//
//                        customCallStatus = "noanswers";
//                        customCallStatusCode = "600";
//                    }
//
//                } else if (map.get("hangup_cause").equals("USER_BUSY") &&
//                        map.get("sip_invite_failure_status").equals("486")) {
//                    callStatus = "Callee Cancel";
//                    callStatusCode = "488";
//                    closedBy = "callee";
//
//                    customCallStatus = "noanswers";
//                    customCallStatusCode = "600";
//                } else {
//                    callStatus = "Timeout";
//                    callStatusCode = "489";
//                    closedBy = "callee";
//
//                    customCallStatus = "noanswers";
//                    customCallStatusCode = "600";
//                }
//            }
//            else {
//                status = "no-answered";
//                callStatus = map.get("hangup_cause");
//                callStatusCode = "488";
//                closedBy = "callee";
//
//                customCallStatus = "noanswers";
//                customCallStatusCode = "600";
//            }
//        }
        customs.put("status", status);
        customs.put("callStatus", callStatus);
        customs.put("callStatusCode", callStatusCode);
        customs.put("closedBy", closedBy);

        customs.put("customCallStatus", customCallStatus);
        customs.put("customCallStatusCode", customCallStatusCode);
        return customs;
    }

    private void printLog(EslEvent event) {
        logger.info("Received connect response [" + event.getEventName() + "]");
        Map<String, String> eventHeaders = event.getEventHeaders();

        logger.info("=======================  eventHeaders  =============================");
        if (eventHeaders != null) {
            for (String key : eventHeaders.keySet()) {
                logger.info(key + ": " + eventHeaders.get(key));
            }
        }
    }
    public static final ThreadLocal<SimpleDateFormat> formatter2 = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        }
    };
    public static String convertTimestampToStringDate(long stamp) {

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter2.get().format(new Date(stamp));
    }
}
