package com.freeswitch.demoCall.Controller;

import com.freeswitch.demoCall.Service.Inbound.ESLInboundManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CallController {
    private final ESLInboundManager eslInboundManager;

    @PostMapping("/transfer")
    public String kickAndAddToConference(@RequestParam String callee) {
        eslInboundManager.transfer(callee);
        return "Kick and add request sent";
    }

//    @PostMapping("/take-over")
//    public String takeOverCall() {
//        eslInboundManager.takeOver();
//        return "Take-over request sent";
//    }
}
