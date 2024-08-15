package com.freeswitch.demoCall.Service;

import com.freeswitch.demoCall.Service.Outbound.ESLOutboundListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class AppStartupRunner implements ApplicationRunner {
    @Autowired
    private ESLOutboundListenerService eslOutboundListenerService;

    @Autowired
    private LeaderElectionStartupRunner leaderElectionStartupRunner;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("======================>" + "AppStartupRunner");
        eslOutboundListenerService.run();
        leaderElectionStartupRunner.startLeaderElectionWatcher();
    }

}
