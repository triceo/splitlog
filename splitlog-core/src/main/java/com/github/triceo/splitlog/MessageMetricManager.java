package com.github.triceo.splitlog;

import com.github.triceo.splitlog.api.MessageMeasure;
import com.github.triceo.splitlog.api.MessageMetric;
import com.github.triceo.splitlog.api.MessageMetricProducer;

public class MessageMetricManager implements MessageMetricProducer {

    @Override
    public <T extends Number> MessageMetric<T> measure(final MessageMeasure<T> measure, final String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MessageMetric<? extends Number> getMetric(final String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMetricId(final MessageMetric<? extends Number> measure) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean terminateMeasuring(final String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean terminateMeasuring(final MessageMeasure<? extends Number> measure) {
        // TODO Auto-generated method stub
        return false;
    }

}
