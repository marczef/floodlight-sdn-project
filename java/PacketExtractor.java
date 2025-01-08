package pl.edu.agh.kt;

import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;

public class PacketExtractor {
	private static final Logger logger = LoggerFactory
			.getLogger(PacketExtractor.class);
	private FloodlightContext cntx;
	protected IFloodlightProviderService floodlightProvider;
	private Ethernet eth;
	private IPv4 ipv4;
	private ARP arp;
	private TCP tcp;
	private UDP udp;
	private OFMessage msg;

	public PacketExtractor(FloodlightContext cntx, OFMessage msg) {
		this.cntx = cntx;
		this.msg = msg;
		logger.info("PacketExtractor: Constructor method called");
	}

	public PacketExtractor() {
		logger.info("PacketExtractor: Constructor method called");
	}

	public Map<String, TransportPort> packetExtract(FloodlightContext cntx) {
		this.cntx = cntx;
		return extractEth();
	}

	public Map<String, TransportPort> extractEth() {
		eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		if (eth.getEtherType() == EthType.ARP) {
			
			logger.info("ARP z jakiegos powodu");
			arp = (ARP) eth.getPayload();
			
			// extractArp();
		}
		if (eth.getEtherType() == EthType.IPv4) {
			ipv4 = (IPv4) eth.getPayload();

			byte[] ipOptions = ipv4.getOptions();
			IPv4Address dstIp = ipv4.getDestinationAddress();

			if (ipv4.getProtocol() == IpProtocol.TCP) {
				TCP tcp = (TCP) ipv4.getPayload();

				TransportPort srcPort = tcp.getSourcePort();
				TransportPort dstPort = tcp.getDestinationPort();
				
				logger.info("Frame: srcPort TCP{}", srcPort);
				logger.info("Frame: DSTPORT!!! TCP{}", dstPort);
				
				Map<String, TransportPort> ports = new HashMap<>();
			    ports.put("srcPort", srcPort);
			    ports.put("dstPort", dstPort);

			    return ports;
				

			}
		}
		
		return null;

	}

	public void extractArp() {
		logger.info("ARP extractor");
	}

	public void extractIp() {
		logger.info("IP extractor");
	}

}
