package com.freeswitch.demoCall.Controller;

import com.freeswitch.demoCall.Service.Outbound.ESLOutboundListenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CallController {

    private final ESLOutboundListenerService eslOutboundListenerService;

//    @PostMapping("/conference/kick-and-add")
//    public String kickAndAddToConference(@RequestParam String conferenceName, @RequestParam String userToKick, @RequestParam String userToAdd, ChannelHandlerContext ctx) {
//        EventSocketAPI.kickAndAddToConference(ctx, conferenceName, userToKick, userToAdd);
//        return "Kick and add request sent";
//    }

}
