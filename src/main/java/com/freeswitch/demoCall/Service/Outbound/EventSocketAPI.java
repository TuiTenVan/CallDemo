package com.freeswitch.demoCall.Service.Outbound;

import lombok.extern.log4j.Log4j2;
import org.freeswitch.esl.client.internal.AbstractEslClientHandler;
import org.freeswitch.esl.client.outbound.AbstractOutboundClientHandler;
import org.freeswitch.esl.client.transport.SendMsg;
import org.freeswitch.esl.client.transport.message.EslHeaders;
import org.freeswitch.esl.client.transport.message.EslHeaders.Name;
import org.freeswitch.esl.client.transport.message.EslMessage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

@Log4j2
public class EventSocketAPI {

    public static void hangupCall(ChannelHandlerContext ctx) {
        SendMsg hangupMsg = new SendMsg();
        hangupMsg.addCallCommand("execute");
        hangupMsg.addExecuteAppName("hangup");
        EslMessage response = ((AbstractEslClientHandler) ctx.getHandler()).sendSyncMultiLineCommand(ctx.getChannel(),
                hangupMsg.getMsgLines());

        boolean isResult = response.getHeaderValue(Name.REPLY_TEXT).startsWith("+OK");
        if (!isResult) {
            System.out.println("Call hangup failed:" + response.getHeaderValue(Name.REPLY_TEXT));
        }
    }

    public static void callToConference(ChannelHandlerContext ctx, String callee) {
        AbstractOutboundClientHandler handler = (AbstractOutboundClientHandler) ctx.getHandler();
        Channel channel = ctx.getChannel();
        SendMsg bindMsg = new SendMsg();
        bindMsg.addCallCommand("execute");
        bindMsg.addExecuteAppName("conference");
        bindMsg.addExecuteAppArg("bridge:ringmeCall:user/" + callee);
        EslMessage response = handler.sendSyncMultiLineCommand(channel, bindMsg.getMsgLines());
        String replyText = response.getHeaderValue(EslHeaders.Name.REPLY_TEXT);
        if (replyText.startsWith("+OK")) {
            System.out.println("Conference action successful with callers: ");
        } else {
            System.out.println("Conference action failed: " + replyText);
        }
    }
    public static void addFlags(ChannelHandlerContext ctx, String conferenceName) {
        AbstractOutboundClientHandler handler = (AbstractOutboundClientHandler) ctx.getHandler();
        Channel channel = ctx.getChannel();
        SendMsg kickMsg = new SendMsg();
        kickMsg.addCallCommand("execute");
        kickMsg.addExecuteAppName("conference");
        kickMsg.addExecuteAppArg(conferenceName + "+flags{}");
        kickMsg.addEventLock();
        try {
            EslMessage response = handler.sendSyncMultiLineCommand(channel, kickMsg.getMsgLines());
            String replyText = response.getHeaderValue(EslHeaders.Name.REPLY_TEXT);
            if (replyText.startsWith("+OK")) {
                System.out.println("Successfully");
            } else {
                System.out.println("Failed" + replyText);
            }
        } catch (Exception e) {
            System.err.println("An error occurred " + e.getMessage());
        }
    }

    public static void addMemberToConference(ChannelHandlerContext ctx, String conferenceName, String callee) {
        AbstractOutboundClientHandler handler = (AbstractOutboundClientHandler) ctx.getHandler();
        Channel channel = ctx.getChannel();
        SendMsg addMsg = new SendMsg();
        addMsg.addCallCommand("execute");
        addMsg.addExecuteAppName("conference_set_auto_outcall");
        addMsg.addExecuteAppArg(callee);
        addMsg.addEventLock();

        EslMessage response = handler.sendSyncMultiLineCommand(channel, addMsg.getMsgLines());
        String replyText = response.getHeaderValue(EslHeaders.Name.REPLY_TEXT);

        if (replyText.startsWith("+OK")) {
            System.out.println("Successfully added " + callee + " to conference " + conferenceName);
        } else {
            System.out.println("Failed to add member: " + replyText);
        }
    }


    public static void runCommand(ChannelHandlerContext ctx, String appName, String appArg, boolean addEventLock) {
        AbstractOutboundClientHandler handler = (AbstractOutboundClientHandler) ctx.getHandler();
        Channel channel = ctx.getChannel();
        SendMsg cmd = new SendMsg();
        cmd.addCallCommand("execute");
        cmd.addExecuteAppName(appName);
        if ( appArg != null) {
            cmd.addExecuteAppArg(appArg);
        }
        if (addEventLock) {
            cmd.addEventLock();
        }
        EslMessage response = handler.sendSyncMultiLineCommand(channel, cmd.getMsgLines());
        response.getHeaderValue(Name.REPLY_TEXT);
    }
}
