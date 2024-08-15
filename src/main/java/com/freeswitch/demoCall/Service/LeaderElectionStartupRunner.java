package com.freeswitch.demoCall.Service;

import com.freeswitch.demoCall.Service.Inbound.ESLInboundManager;
import com.freeswitch.demoCall.Service.Inbound.LeaderElection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LeaderElectionStartupRunner {

    private final Logger logger = LogManager.getLogger(LeaderElectionStartupRunner.class);

    @Autowired
    private ESLInboundManager eslInboundManager;

    @Async
    public void startLeaderElectionWatcher() throws Exception {

        logger.info("startLeaderElectionWatcher|init");
        try {
            LeaderElection leaderElection  = new LeaderElection();
            leaderElection.setEslInboundManager(eslInboundManager);
            leaderElection.reelectLeader();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}