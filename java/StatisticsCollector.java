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
import org.projectfloodlight.openflow.types.IPv4Address;
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
	public static int Tmax = 10000;

	private static final long treshold1 = 5; // Minimalny ruch5Mbps
	private static final long treshold2 = 12; // Duzy ruch 20Mbps

	public static int alfa = 2;
	public static int beta = 4;

	public int pollingInterval = 5000;

	private final Map<Integer, List<Flow>> schedule_table = new ConcurrentHashMap<>();
	private final Map<Integer, ScheduledFuture<?>> intervalThreadPools = new ConcurrentHashMap<>();
	private final List<Flow> flows = new ArrayList<Flow>();
	
	private final TopologyManager topManager = TopologyManager.getInstance();

	public StatisticsCollector(IThreadPoolService threadPoolService) {
		this.threadPoolService = threadPoolService;

		ScheduledFuture<?> future = threadPoolService.getScheduledExecutor()
				.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						processScheduleTable(Tmin);
					}
				}, Tmin, Tmin, TimeUnit.MILLISECONDS);

		intervalThreadPools.put(Tmin, future);
		logger.info("intervalThreadPools" + intervalThreadPools);

		logger.warn("StatisticsCollector initialized with periodic processing thread.");
	}

	public synchronized void addFlow(IOFSwitch sw,
			Map<String, TransportPort> ports, Map<String, IPv4Address> ips) {
		Flow newFlow = new Flow(sw, ports, ips);

		synchronized (schedule_table) {

			if (!schedule_table.containsKey(Tmin)) {
				schedule_table.put(Tmin, new ArrayList<Flow>());
			}
			schedule_table.get(Tmin).add(newFlow);
		}

	}

	private synchronized void processScheduleTable(int currInterval) {

		synchronized (schedule_table) {

			logger.info("schedule_table:" + schedule_table.toString());
			logger.info("currInterval:" + currInterval);
			logger.info("futures list" + intervalThreadPools.keySet());

			for (Map.Entry<Integer, List<Flow>> entry : schedule_table
					.entrySet()) {
				int interval = entry.getKey();
				List<Flow> flows = entry.getValue();

				for (Flow flow : flows) {
					processFlow(flow, interval);
				}

				processThreads();
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

					IPv4Address sDstIP = pse.getMatch()
							.get(MatchField.IPV4_DST);
					IPv4Address fDstIP = flow.getDstIP();
					IPv4Address sSrcIP = pse.getMatch()
							.get(MatchField.IPV4_SRC);
					IPv4Address fSrcIP = flow.getSrcIP();
					
					if (sDstTcp != null && fDstTcp != null
							&& sDstTcp.equals(fDstTcp) && sSrcTcp != null
							&& fSrcTcp != null && sSrcTcp.equals(fSrcTcp)
							&& sDstIP != null && fDstIP != null
							&& fDstIP.equals(sDstIP) && sSrcIP != null
							&& fSrcIP != null && fSrcIP.equals(sSrcIP)) {

						long diffByteCount = pse.getByteCount().getValue()
								- flow.getLastByteCount();
						
						long throughput = (long) calculateThroughput(
								diffByteCount, interval);

						logger.info("Throughput in Mbs " + throughput);
						
						this.topManager.addLink(fSrcIP.toString(), sDstIP.toString(), throughput);

						flow.setLastByteCount(pse.getByteCount().getValue());

						int newInterval = calculateNewInterval(interval,
								throughput);

						if (interval != newInterval) {
							updateFlowInScheduleTable(flow, newInterval);
						}
					}
					// else { TODO FIX LOGIC HERE
					// removeFlow(flow);
					// }
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

	private void updateFlowInScheduleTable(Flow flow, final int newInterval) {

		int currentInterval = flow.getInterval();

		synchronized (schedule_table) {
			try {
				if (schedule_table.containsKey(currentInterval)) {
					List<Flow> flows = new ArrayList<>(
							schedule_table.get(currentInterval));
					if (flows != null && flows.contains(flow)) {

						flows.remove(flow);
						schedule_table.put(currentInterval, flows);

						if (flows.isEmpty()) {
							schedule_table.remove(currentInterval);
						}
					}
				}

				flow.setPollInterval(newInterval);

				if (!schedule_table.containsKey(newInterval)) {
					schedule_table.put(newInterval, new ArrayList<Flow>());
					schedule_table.get(newInterval).add(flow);

					synchronized (intervalThreadPools) {
						if (!intervalThreadPools.containsKey(newInterval)) {
							newThread(newInterval);
						}
					}

				} else {
					schedule_table.get(newInterval).add(flow);
				}

			} catch (Exception e) {
				logger.error(
						"An error occurred while removing flow from schedule_table: ",
						e);
			}

		}
	}

	private void processThreads() {

		synchronized (schedule_table) {
			synchronized (intervalThreadPools) {
				for (Integer interval : new ArrayList<>(
						intervalThreadPools.keySet())) {
					if (!schedule_table.containsKey(interval)) {
						ScheduledFuture<?> future = intervalThreadPools
								.remove(interval);
						if (future != null && !future.isCancelled()
								&& !future.isDone()) {
							future.cancel(false);
							logger.info("Removed thread pool for interval: "
									+ interval);
						}
					}
				}
			}
		}
	}

	protected synchronized void newThread(final int newInterval) {

		synchronized (intervalThreadPools) {

			if (!intervalThreadPools.containsKey(newInterval)) {

				ScheduledFuture<?> future = threadPoolService
						.getScheduledExecutor().scheduleAtFixedRate(
								new Runnable() {
									@Override
									public void run() {
										processScheduleTable(newInterval);
									}
								}, newInterval, newInterval,
								TimeUnit.MILLISECONDS);

				logger.info("Created threath for interval " + newInterval);

				intervalThreadPools.put(newInterval, future);
			}

		}
	}

	private synchronized void removeFlow(Flow flow) {
		synchronized (schedule_table) {
			try {
				int currentInterval = flow.getInterval();

				if (schedule_table.containsKey(currentInterval)) {
					List<Flow> flows = schedule_table.get(currentInterval);

					if (flows != null && flows.contains(flow)) {
						flows.remove(flow);

					} else {
						logger.warn("Flow not found in schedule_table for interval: "
								+ currentInterval);
					}
				} else {
					logger.warn("No schedule_table entry for interval: "
							+ currentInterval);
				}
			} catch (Exception e) {
				logger.error("An error occurred while removing flow: ", e);
			}
		}
	}

	protected synchronized List<OFStatsReply> getSwitchStatistics(IOFSwitch sw) {
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;

		if (sw != null) {
			try {
				Match match = sw.getOFFactory().buildMatch().build();
				OFStatsRequest<?> req = sw.getOFFactory()
						.buildFlowStatsRequest().setMatch(match)
						.setOutPort(OFPort.ANY).setOutGroup(OFGroup.ANY) //TODO OFPort.of(poort)
						.setTableId(TableId.ALL).build();

				future = sw.writeStatsRequest(req);

				try {
					values = (List<OFStatsReply>) future.get(
							pollingInterval * 1000 / 2, TimeUnit.MILLISECONDS);
					logger.info("VALUES: " + values);
				} catch (TimeoutException e) {
					logger.warn("Timeout retrieving statistics from switch {}",
							sw.getId());
				} catch (ExecutionException e) {
					logger.error(
							"Execution exception retrieving statistics from switch {}. Error: {}",
							sw.getId(), e);
				}

			} catch (Exception e) {
				logger.error(
						"Failed to build or send statistics request to switch {}. Error: {}",
						sw, e);
			}
		} else {
			logger.warn("Switch is null, cannot retrieve statistics.");
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
