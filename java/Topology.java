package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.List;

public class Topology {

    private List<Node> nodes;
    private List<Link> links;

    public Topology() {
        this.nodes = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public static class Node {
        private String name;

        public Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Node{name='" + name + "'}";
        }
    }

    public static class Link {
        private String node1;
        private String node2;
        private String label;
        private long timestamp;

        public Link(String node1, String node2, String label, long timestamp) {
            this.node1 = node1;
            this.node2 = node2;
            this.label = label;
            this.timestamp = timestamp;
        }

        public String getNode1() {
            return node1;
        }

        public void setNode1(String node1) {
            this.node1 = node1;
        }

        public String getNode2() {
            return node2;
        }

        public void setNode2(String node2) {
            this.node2 = node2;
        }
        
        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
        
        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "Link{node1=" + node1 + ", node2=" + node2 + ", label=" + label+ '}';
        }
    }

    @Override
    public String toString() {
        return "Topology{nodes=" + nodes + ", links=" + links + '}';
    }
}
