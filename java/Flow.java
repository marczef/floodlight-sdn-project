package pl.edu.agh.kt;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.projectfloodlight.openflow.types.TransportPort;

import net.floodlightcontroller.core.IOFSwitch;

public class Flow {
	
	public static IOFSwitch sw;
	public static TransportPort dstPort;
	public static TransportPort srcPort;
	public int flowPollingInterval;
	private long lastByteCount;
	
	private final int Tmin = 500;
	
	public Flow(IOFSwitch sw, Map<String, TransportPort> ports) {
		this.sw = sw;
		this.flowPollingInterval = Tmin;
		this.lastByteCount = 0;
		
		if (ports != null) {
			this.srcPort = ports.get("srcPort");
			this.dstPort = ports.get("dstPort");
		}
	}
	
	public void setPollInterval(int newPollingInterval) {
		this.flowPollingInterval = newPollingInterval;
	}
	
	public IOFSwitch getSwitch() { return sw; }
	public TransportPort getDstPort() { return dstPort; }
	public TransportPort getSrcPort() { return srcPort; }
	public int getInterval() { return flowPollingInterval; }
	public long getLastByteCount() { return lastByteCount; }
	public void setLastByteCount(long lastByteCount) { this.lastByteCount = lastByteCount; }
    
    @Override
    public String toString() {
        return "Flow{" + 
               "sw=" + sw +
               ", Destination Port=" + dstPort +
               ", Source Port=" + srcPort +
               ", pollingInterval=" + flowPollingInterval +
               '}';
    }
	

}
