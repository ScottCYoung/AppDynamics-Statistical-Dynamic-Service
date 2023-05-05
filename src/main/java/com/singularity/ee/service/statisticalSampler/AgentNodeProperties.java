package com.singularity.ee.service.statisticalSampler;

import com.singularity.ee.agent.appagent.kernel.spi.data.IServiceConfig;
import com.singularity.ee.agent.util.log4j.ADLoggerFactory;
import com.singularity.ee.agent.util.log4j.IADLogger;
import com.singularity.ee.util.string.StringOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

public class AgentNodeProperties extends Observable {
    private static final IADLogger logger = ADLoggerFactory.getLogger((String)"com.singularity.ee.service.statisticalSampler.AgentNodeProperties");
    public static final String[] NODE_PROPERTIES = new String[]{"agent.statisticalSampler.enabled", "agent.statisticalSampler.percentage"};
    private final Map<String, String> properties = new HashMap<>();

    public void initializeConfigs(IServiceConfig serviceConfig) {
        Map configProperties = serviceConfig.getConfigProperties();
        if( configProperties != null ) {
            boolean enabled = StringOperations.safeParseBoolean((String)((String)configProperties.get("agent.statisticalSampler.enabled")), (boolean)true);
            this.properties.put("agent.statisticalSampler.enabled", Boolean.toString(enabled));
            this.properties.put("agent.statisticalSampler.percentage", (String)configProperties.get("agent.statisticalSampler.percentage"));
            logger.info("Initializing the properties " + this);
        } else {
            logger.error("Config properties map is null?!?!");
        }
    }

    public String getProperty( String name ) {
        return this.properties.get(name);
    }

    public void updateProperty( String name, String value ) {
        String existingPropertyValue = this.properties.get(name);
        if( !StringOperations.isEmpty((String)value) && !value.equals(existingPropertyValue)) {
            this.properties.put(name, value);
            logger.info("updated property = " + name + " with value = " + value);
            this.notifyMonitoringService(name);
        } else {
            logger.info("did not update property = " + name + " because it was either unchanged or empty");
        }
    }

    protected void notifyMonitoringService(String name) {
        this.setChanged();
        this.notifyObservers(name);
    }

    public String toString() {
        return "AgentNodeProperties{properties=" + this.properties + '}';
    }

    public boolean isEnabled() {
        return StringOperations.safeParseBoolean((String)this.getProperty("agent.statisticalSampler.enabled"), (boolean)true);
    }


    public Integer getEnabledPercentage() {
        int value = StringOperations.safeParseInteger(this.properties.get("agent.statisticalSampler.percentage"), 10 );
        if( value < 1 ) return 1;
        if( value > 99 ) return 99;
        return value;
    }

}