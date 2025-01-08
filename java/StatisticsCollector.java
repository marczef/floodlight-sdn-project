package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.lang.Thread.State;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.statistics.FlowRuleStats;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.util.Pair;

public class StatisticsCollector {

	private static final Logger logger = LoggerFactory
			.getLogger(StatisticsCollector.class);

	private static IThreadPoolService threadPoolService;
	private static IOFSwitchService switchService;

	private IOFSwitch sw;
	private Timer timer;

	public static int Tmin = 1000;
	public static int Tmax = 5000;

	private static final long treshold1 = 625000; // Minimalna zmiana ruchu 5Mbps
	private static final long treshold2 = 2500000; // Duzy ruch 20Mbps

	public static int alfa = 2;
	public static int beta = 4;

	public int pollingInterval = 5000;

	private final Map<Integer, List<Flow>> schedule_table = new ConcurrentHashMap<>();
	private final List<Object> active_flows = Collections
			.synchronizedList(new ArrayList<>());

	public StatisticsCollector(IThreadPoolService threadPoolService) {
		this.threadPoolService = threadPoolService;

		threadPoolService.getScheduledExecutor().scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						processScheduleTable();
					}
				}, Tmin, Tmin, TimeUnit.MILLISECONDS);

		logger.warn("StatisticsCollector initialized with periodic processing thread.");
	}

	public synchronized void addFlow(IOFSwitch sw,
			Map<String, TransportPort> ports) {
		Flow newFlow = new Flow(sw, ports);

		if (!schedule_table.containsKey(Tmin)) {
			schedule_table.put(Tmin, new ArrayList<Flow>());
		}
		schedule_table.get(Tmin).add(newFlow);

	}

	private synchronized void processScheduleTable() {

		logger.info("schedule_table:" + schedule_table.toString());

		for (Map.Entry<Integer, List<Flow>> entry : schedule_table.entrySet()) {
			int interval = entry.getKey();
			List<Flow> flows = entry.getValue();

			for (Flow flow : flows) {
				TransportPort dstPort = flow.getDstPort();
				TransportPort srcPort = flow.getSrcPort();

				// if (dstPort == null && flow.getFuture() != null) {
				// logger.info("powinien flow byc usuniety");
				// removeFlow(flow);
				// }

				processFlow(flow, interval);
			}
		}
	}

	private void processFlow(Flow flow, int interval) {
		List<OFStatsReply> replies = getSwitchStatistics(flow.getSwitch());
		if (replies != null) {
			for (OFStatsReply r : replies) {
				OFFlowStatsReply psr = (OFFlowStatsReply) r;
				for (OFFlowStatsEntry pse : psr.getEntries()) {

					TransportPort sDstTcp = pse.getMatch().get(
							MatchField.TCP_DST);
					TransportPort fDstTcp = flow.getDstPort();
					TransportPort sSrcTcp = pse.getMatch().get(
							MatchField.TCP_SRC);
					TransportPort fSrcTcp = flow.getSrcPort();

					if (sDstTcp != null && fDstTcp != null
							&& sDstTcp.equals(fDstTcp) && sSrcTcp != null
							&& fSrcTcp != null && sSrcTcp.equals(fSrcTcp)) {

						// logger.info("Switch bytecount: "
						// + pse.getByteCount().getValue());
						// logger.info("Flow getlast byte count "
						// + flow.getLastByteCount());

						long diffByteCount = pse.getByteCount().getValue()
								- flow.getLastByteCount();

						long throughput = (long) calculateThroughput(
								diffByteCount, interval);

						logger.info("Throughput in Mbs " + throughput);

						flow.setLastByteCount(pse.getByteCount().getValue());

						int newInterval = calculateNewInterval(interval,
								diffByteCount);
						updateFlowInScheduleTable(flow, newInterval);
					}
				}
			}
		}
	}

	private int calculateNewInterval(int currentInterval, long diffByteCount) {
		if (diffByteCount < treshold1) {
			return Math.min(currentInterval * alfa, Tmax);
		} else if (diffByteCount > treshold2) {
			return Math.max(currentInterval / beta, Tmin);
		} else {
			return currentInterval;
		}
	}

	private synchronized void updateFlowInScheduleTable(Flow flow,
			int newInterval) {
		
		logger.info("polling: " + flow.getInterval());
		logger.info("new interval: " + newInterval);
		
//		int currentInterval = flow.getInterval();
//
//		if (schedule_table.containsKey(currentInterval)) {
//			schedule_table.get(currentInterval).remove(flow);
//		}
//
//		flow.setPollInterval(newInterval);
//
//		if (!schedule_table.containsKey(newInterval)) {
//			schedule_table.put(newInterval, new ArrayList<Flow>());
//		}
//		schedule_table.get(newInterval).add(flow);
	}

	protected void removeFlow(Flow flow) {
		logger.info("removed flow");

		synchronized (active_flows) {
			if (active_flows.remove(flow)) {
				logger.info("Flow usuniety z active_flows: " + flow);
			}
		}

		synchronized (schedule_table) {
			for (Map.Entry<Integer, List<Flow>> entry : schedule_table
					.entrySet()) {
				if (entry.getValue().remove(flow)) {
					logger.info("Flow usuniety z schedule_table dla interwalu: "
							+ entry.getKey());
					break;
				}
			}
		}

		logger.info("Usunieto watek dla flow: " + flow);
	}

	protected List<OFStatsReply> getSwitchStatistics(IOFSwitch sw) {
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;

		Match match;
		if (sw != null) {

			OFStatsRequest<?> req = null;
			match = sw.getOFFactory().buildMatch().build();
			if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_11) >= 0) {
				req = sw.getOFFactory().buildFlowStatsRequest().setMatch(match)
						.setOutPort(OFPort.ANY)
						// tu OUTPORT
						.setOutGroup(OFGroup.ANY).setTableId(TableId.ALL)
						.build();
			}

			try {
				if (req != null) {
					future = sw.writeStatsRequest(req);
					values = (List<OFStatsReply>) future.get(
							pollingInterval * 1000 / 2, TimeUnit.MILLISECONDS);

					// if (!values.isEmpty()) {
					// logger.info("values" + values);
					// } else {
					// logger.info("values EMPTY!");
					// }
				}
			} catch (Exception e) {
				logger.error(
						"Failure retrieving statistics from switch {}. {}", sw,
						e);
			}
		}
		return values;
	}

	public double calculateThroughput(long byteCount, long timeIntervalMs) {

		if (byteCount <= 0) {
			return (double) 0;
		}

		long bitDifference = byteCount * 8;

		double bitsPerSecond = (double) bitDifference
				/ (timeIntervalMs / 1000.0);

		double megabitsPerSecond = bitsPerSecond / 1_000_000.0;

		return megabitsPerSecond;
	}
}
