package com.sunlights.core.actor;

import akka.actor.UntypedActor;
import com.sunlights.core.models.SmsMessage;

/**
 * <p>Project: tradingsystem</p>
 * <p>Title: SmsSendActor.java</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2014 Sunlights.cc</p>
 * <p>All Rights Reserved.</p>
 *
 * @author <a href="mailto:jiaming.wang@sunlights.cc">wangJiaMing</a>
 */
public class SmsSendActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof SmsMessage) {
            SmsMessage sm = (SmsMessage) message;
            // TODO: send message.
//            SmsMessageClient.sendSms(sm);
        } else {
            unhandled(message);
        }
    }
}