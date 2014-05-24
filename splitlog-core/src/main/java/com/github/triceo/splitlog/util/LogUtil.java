package com.github.triceo.splitlog.util;

import org.slf4j.Logger;

import com.github.triceo.splitlog.api.Message;
import com.github.triceo.splitlog.api.MessageDeliveryStatus;
import com.github.triceo.splitlog.api.MessageListener;
import com.github.triceo.splitlog.api.MessageProducer;

public class LogUtil {

    public static enum Level {
        DEBUG, ERROR, INFO, TRACE, WARNING;
    }

    private static final Object[] NOARGS = new Object[0];
    private static final String STATUS_TEMPLATE_NOPRODUCER = " {}\n\tMessage: {}\n\tWhere: {}";
    private static final String STATUS_TEMPLATE_PRODUCER = LogUtil.STATUS_TEMPLATE_NOPRODUCER + "\n\tFrom: {}";

    public static void newMessage(final Logger logger, final Level level, final String note, final Message msg,
        final MessageDeliveryStatus status, final MessageProducer<?> producer, final MessageListener<?> consumer) {
        LogUtil.newMessage(logger, level, note, msg, status, producer, consumer, LogUtil.NOARGS);
    }

    public static void newMessage(final Logger logger, final Level level, final String note, final Message msg,
        final MessageDeliveryStatus status, final MessageProducer<?> producer, final MessageListener<?> consumer,
        final Object... objects) {
        final String result = note + LogUtil.STATUS_TEMPLATE_PRODUCER;
        LogUtil.notify(logger, level, result, status, msg, consumer, producer, objects);
    }

    public static void notify(final Logger logger, final Level level, final String note, final Object... objects) {
        switch (level) {
            case DEBUG:
                logger.debug(note, objects);
                break;
            case ERROR:
                logger.error(note, objects);
                break;
            case INFO:
                logger.info(note, objects);
                break;
            case TRACE:
                logger.trace(note, objects);
                break;
            case WARNING:
                logger.warn(note, objects);
                break;
        }
    }
}
