package com.freeswitch.demoCall.Service.Outbound;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeswitch.esl.client.outbound.AbstractOutboundClientHandler;
import org.freeswitch.esl.client.transport.event.EslEvent;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.springframework.context.ApplicationContext;

@Data
@NoArgsConstructor
public class ESLOutboundHandler extends AbstractOutboundClientHandler {
    private final Logger logger = LogManager.getLogger(ESLOutboundHandler.class);
    public ESLOutboundHandler(ApplicationContext context) {}
    @Override
    protected void handleConnectResponse(ChannelHandlerContext ctx, EslEvent event) {
        System.out.println(event.getEventHeaders().get("Answer-State"));
        if (event.getEventName().equalsIgnoreCase("CHANNEL_DATA")) {
            if ("ringing".equalsIgnoreCase(event.getEventHeaders().get("Answer-State"))) {
                callTransfer(ctx,event);
            }
        } else {
            logger.warn("Unexpected event after connect: [" + event.getEventName() + ']');
        }
    }

    @Override
    protected void handleEslEvent(ChannelHandlerContext ctx, EslEvent event) {
        System.out.println("Received event: " + event);
        String eventName = event.getEventName();
        System.out.println(eventName);
    }

    @Override
    protected void handleDisconnectionNotice() {
        super.handleDisconnectionNotice();
        System.out.println("Received disconnection notice");
    }

    public void callTransfer(ChannelHandlerContext ctx, EslEvent event) {
        EventSocketAPI.callToConference(ctx, "1000");
        EventSocketAPI.addFlags(ctx, "ringmeCall");
    }
}
