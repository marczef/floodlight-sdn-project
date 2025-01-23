# -*- coding: utf-8 -*-
import socket
import time
import threading
import random
from time import sleep
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.util import dumpNodeConnections
from mininet.cli import CLI

# Konfiguracja
MIN_DURATION = 10  # Minimalny czas trwania testu iPerf (w sekundach)
MAX_DURATION = 30  # Maksymalny czas trwania testu iPerf (w sekundach)
MIN_BURST = 1000  # Minimalna przepustowość w Kbps
MAX_BURST = 10000  # Maksymalna przepustowość w Kbps
MIN_INTERVAL = 1  # Minimalny interwał raportowania (w sekundach)
MAX_INTERVAL = 5  # Maksymalny interwał raportowania (w sekundach)
NUMBER_OF_SERVERS = 9  # Liczba portów na serwerze iPerf
GENERATION_INTERVAL = 5  # Czas między kolejnymi testami iPerf

class CustomTopo(Topo):
    def __init__(self):
        print("Inicjalizacja CustomTopo...")
        super(CustomTopo, self).__init__()
        self.build()
    def build(self):
    	# Add hosts and switches
      kolobrzeg = self.addSwitch('s1')
      gdansk = self.addSwitch('s2')
      szczecin = self.addSwitch('s3')
      poznan = self.addSwitch('s4')
      print("Dodano przelaczniki:")
      kolobrzeg_host = self.addHost('h1')
      gdansk_host = self.addHost('h2')
      szczecin_host = self.addHost('h3')
      poznan_host = self.addHost('h4')
      print("Dodano hosty:")
        # Add links
      self.addLink(kolobrzeg, kolobrzeg_host)
      self.addLink(gdansk, gdansk_host)
      self.addLink(szczecin, szczecin_host)
      self.addLink(poznan, poznan_host)
      self.addLink(szczecin, kolobrzeg)
      self.addLink(szczecin, poznan)
      self.addLink(kolobrzeg, gdansk)
      self.addLink(poznan, gdansk)

def run_iperf_servers(net):
    """Uruchom serwery iPerf na h3 i h4"""
    for i in range(3, 5):  # h3 i h4
        for j in range(1, NUMBER_OF_SERVERS + 1):
            host = net.get('h{}'.format(i))
            host.cmd('iperf3 -s -i 1  -p 100{} &'.format(j))
            print('Iperf3 server started on port 100{}'.format(j))
        print('Iperf3 server started on h{}'.format(i))

def run_iperf_client(client, server):
    """Uruchom klienta iPerf, aby generować ruch."""
    duration = random.randint(MIN_DURATION, MAX_DURATION)
    burst_size = random.randint(MIN_BURST, MAX_BURST)
    interval = random.randint(MIN_INTERVAL, MAX_INTERVAL)
    port_nr = random.randint(1, NUMBER_OF_SERVERS)
    timestamp = int(time.time() * 1e9)
    print('{} iperf3 -c {} -t {} -b {}K -i {} -p 100{} --logfile iperf_{}_to_{}_{}.log &'.format(
        client,server.IP(), duration, burst_size, interval, port_nr, client.name, server.name, timestamp))
    client.cmd('iperf3 -c {} -t {} -b {}K -i {} -p 100{} --logfile iperf_{}_to_{}_{}.log &'.format(
        server.IP(), duration, burst_size, interval, port_nr, client.name, server.name, timestamp))

def generate_random_traffic(net):
    """Generuj losowy ruch między klientami a serwerami."""
    src_hosts = [net.get('h1'), net.get('h2')]  # Klienci
    dst_hosts = [net.get('h3'), net.get('h4')]  # Serwery
    run_iperf_servers(net)
    i=0
#    while i < 5 :
#    client = random.choice(src_hosts)
#    server = random.choice(dst_hosts)
    print("Wywaloanie 1 ruchu dla src_hosts[0]")
    src_hosts[0].cmd('xterm -e ./client4.sh &')
    print("Wywaloanie 2 ruchu dla src_hosts[1]")
    src_hosts[1].cmd('xterm -e ./client5.sh &')
#    run_iperf_client(client, server)
    sleep(GENERATION_INTERVAL)
    i+=1
    CLI(net)
if __name__ == "__main__":


    # Tworzenie sieci
    topo = CustomTopo()
    net = Mininet(topo=topo, controller=lambda name: RemoteController(name, ip='127.0.0.1', port=6653))
    net.start()
    dumpNodeConnections(net.hosts)

    try:
        generate_random_traffic(net)
    except KeyboardInterrupt:
        print("\nZatrzymano generowanie ruchu.")
        CLI(net)
    finally:
        net.stop()
