package org.opensource.proxy.config;

public class RouterConfig {
    private String routingName;
    private int enterPort;
    private String routingDestination;
    private int routingPort;
    private String description;

    public RouterConfig(String routingName, int enterPort, String routingDestination, int routingPort, String description) {
        this.routingName = routingName;
        this.enterPort = enterPort;
        this.routingDestination = routingDestination;
        this.routingPort = routingPort;
        this.description = description;
    }

    public String getRoutingName() {
        return routingName;
    }

    public int getEnterPort() {
        return enterPort;
    }

    public String getRoutingDestination() {
        return routingDestination;
    }

    public int getRoutingPort() {
        return routingPort;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + enterPort;
        result = prime * result + ((routingDestination == null) ? 0 : routingDestination.hashCode());
        result = prime * result + routingPort;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RouterConfig other = (RouterConfig) obj;
        if (enterPort != other.enterPort)
            return false;
        if (routingDestination == null) {
            if (other.routingDestination != null)
                return false;
        } else if (!routingDestination.equals(other.routingDestination))
            return false;
        if (routingPort != other.routingPort)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RouterConfig{" +
                "routingName='" + routingName + '\'' +
                ", enterPort=" + enterPort +
                ", routingDestination='" + routingDestination + '\'' +
                ", routingPort=" + routingPort +
                ", description='" + description + '\'' +
                '}';
    }
}