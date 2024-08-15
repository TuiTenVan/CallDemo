package com.freeswitch.demoCall.Service.Outbound;

import org.freeswitch.esl.client.outbound.AbstractOutboundClientHandler;
import org.freeswitch.esl.client.outbound.AbstractOutboundPipelineFactory;
import org.freeswitch.esl.client.outbound.SocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ESLOutboundListenerService {
    @Autowired
    private ApplicationContext context;

    public void run() {
        final SocketClient outboundServer = new SocketClient(8086, new AbstractOutboundPipelineFactory() {

            @Override
            protected AbstractOutboundClientHandler makeHandler() {
                return new ESLOutboundHandler(context);
            }
        });
        outboundServer.start();
    }
}
