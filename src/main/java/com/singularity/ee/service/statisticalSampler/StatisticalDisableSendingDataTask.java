package com.singularity.ee.service.statisticalSampler;

import com.singularity.ee.agent.appagent.kernel.ServiceComponent;
import com.singularity.ee.agent.appagent.kernel.spi.IDynamicService;
import com.singularity.ee.agent.appagent.kernel.spi.IServiceContext;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.util.javaspecific.threads.IAgentRunnable;

import java.util.Map;

public class StatisticalDisableSendingDataTask implements IAgentRunnable {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.dynamicservice.statisticalSampler.StatisticalDisableSendingDataTask");
    private IDynamicService agentService;
    private AgentNodeProperties agentNodeProperties;
    private ServiceComponent serviceComponent;
    private IServiceContext serviceContext;

    public StatisticalDisableSendingDataTask(IDynamicService agentService, AgentNodeProperties agentNodeProperties, ServiceComponent serviceComponent, IServiceContext iServiceContext) {
        this.agentNodeProperties=agentNodeProperties;
        this.agentService=agentService;
        this.serviceComponent=serviceComponent;
        this.serviceContext=iServiceContext;
        agentNodeProperties.setHoldMaxEvents( ReflectionHelper.getMaxEvents(serviceComponent.getEventHandler().getEventService()) );
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if(!agentNodeProperties.isEnabled()) {
            logger.info("Service " + agentService.getName() + " is not enabled. To enable it enable the node property agent.statisticalSampler.enabled");
            return;
        }
        logger.info("Running the task to check if this node will be sending metrics or disabling that functionality");
        //1. get configured percentage of nodes that are enabled to send data
        Integer percentageOfNodesSendingData = agentNodeProperties.getEnabledPercentage();
        int r = (int) (Math.random() *100);
        if( r > percentageOfNodesSendingData ) { //if r > 10% (the large number
            sendInfoEvent("This Agent WILL NOT be sending data, it is randomly selected to reduce metrics and events to the controller r="+r);
            serviceComponent.getMetricHandler().getMetricService().hotDisable(); //disable all metrics
            agentNodeProperties.setMetricThrottled(true);
            if( agentNodeProperties.isMaxEventsSet() ) {
                int newMaxEvents = agentNodeProperties.getMaxEvents();
                if( newMaxEvents > 0 ) {
                    ReflectionHelper.setMaxEvents(serviceComponent.getEventHandler().getEventService(), newMaxEvents);
                } else { //just turn it off
                    serviceComponent.getEventHandler().getEventService().hotDisable(); //disable all events
                }
                agentNodeProperties.setEventThrottled(true);
            } else {
                sendInfoEvent("max events is not being adjusted because node property agent.statisticalSampler.maxEvents is not set [0-100]");
            }
        } else {//else r <= 10%; so enable everything
            sendInfoEvent("This Agent WILL be sending data, it is randomly selected to enable sending metrics and events to the controller r=" + r);
            serviceComponent.getMetricHandler().getMetricService().hotEnable(); //enable all metrics again :)
            serviceComponent.getEventHandler().getEventService().hotEnable(); //enable all events again :)
            ReflectionHelper.setMaxEvents( serviceComponent.getEventHandler().getEventService(), agentNodeProperties.getHoldMaxEvents() );
            agentNodeProperties.setEventThrottled(false);
            agentNodeProperties.setMetricThrottled(false);
        }
    }

    private void sendInfoEvent(String message) {
        sendInfoEvent(message, MetaData.getAsMap());
    }

    private void sendInfoEvent(String message, Map map) {
        logger.info("Sending Custom INFO Event with message: "+ message);
        if( !map.containsKey("statisticalSampler-version") ) map.putAll(MetaData.getAsMap());
        serviceComponent.getEventHandler().publishInfoEvent(message, map);
    }
    
}
