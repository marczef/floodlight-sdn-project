package pl.edu.agh.kt;

import java.util.function.Predicate;

import pl.edu.agh.kt.Topology.Node;

class TopologyManager {

    private static TopologyManager instance;
    private Topology topology;

    private TopologyManager() {
        this.topology = new Topology();
    }

    public static synchronized TopologyManager getInstance() {
        if (instance == null) {
            instance = new TopologyManager();
        }
        return instance;
    }

    public Topology getTopology() {
        return topology;
    }
    
    public Topology getTopologyWithDiffTimestamp() {
    	Topology t = new Topology();
    	t.setNodes(topology.getNodes());
    	for(Topology.Link link: topology.getLinks()) {
    		t.getLinks().add(new Topology.Link(link.getNode1(), link.getNode2(), link.getLabel(), System.currentTimeMillis() - link.getTimestamp()));
    	}
    	return t;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }
    
    public void addLink(String name1, String name2, long throughput) {
    	if (!this.isNodeInTopology(name1)) this.addNode(name1);
    	if (!this.isNodeInTopology(name2)) this.addNode(name2);

    	String thrLabel = String.valueOf(throughput) + " Mbit/s";
    	
    	if(!updateLinkIfPresent(name1, name2, thrLabel))
    		this.topology.getLinks().add(new Topology.Link(name1, name2, thrLabel, System.currentTimeMillis()));
    }
    
    private void addNode(String name1) {
    	this.topology.getNodes().add(new Topology.Node(name1));
    }
    
    private boolean isNodeInTopology(String name) {
    	for(Topology.Node node: this.topology.getNodes()) {
    		if (node.getName().equals(name)) return true;
    	}
    	return false;
    }
    
    private boolean updateLinkIfPresent(String name1, String name2, String label) {
    	for(Topology.Link link: this.topology.getLinks()) {
    		if(link.getNode1().equals(name1) && link.getNode2().equals(name2)){
    			link.setLabel(label);
    			link.setTimestamp(System.currentTimeMillis());
    			return true;
    		}
    	}
    	return false;
    }
}
