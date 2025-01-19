package pl.edu.agh.kt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	private IRestApiService restApiService;
	protected static Logger logger;
	private static IThreadPoolService threadPoolService;
	private static IOFSwitchService switchService;
	private StatisticsCollector statisticsCollector;

	 public static Vector<Flow> active_flows = new Vector<>();
	 Map<Integer, Flow> schedule_table = new HashMap<Integer, Flow>();

	@Override
	public String getName() {
		return SdnLabListener.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		

		switch (msg.getType()) {
		case PACKET_IN:
						
			logger.info("************* NEW PACKET IN *************");
			
			PacketExtractor extractor = new PacketExtractor();
			extractor.packetExtract(cntx);
			Map<String, TransportPort> ports = extractor.getPorts();
			Map<String, IPv4Address> ips = extractor.getIPs();
			
			if (ports != null && ips != null) {
                statisticsCollector.addFlow(sw, ports, ips);
            }
//			Flows.sendPacketOut(sw);
			
			break;
			
		case FLOW_REMOVED:
			logger.info("************* FLOW REMOVED *************");
			break;

		default:
			logger.error("MSG TYPE {}", msg.getType());
			break;
		}
		
		

		return Command.STOP;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);
		l.add(IThreadPoolService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApiService = context.getServiceImpl(IRestApiService.class);
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		logger = LoggerFactory.getLogger(SdnLabListener.class);
		statisticsCollector = new StatisticsCollector(threadPoolService);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		restApiService.addRestletRoutable(new Rest());

		logger.info("******************* START **************************");
		
//		logger.warn("turururu");
//		flowStatsCollector = threadPoolService.getScheduledExecutor()
//				.scheduleAtFixedRate(new TestClass(), 3,
//						3, TimeUnit.SECONDS);
//		// tentativePortStats.clear();
//		logger.warn("Statistics collection thread(s) started");
		
//		
	}

}
