#!/bin/bash
echo "dups"
# Konfiguracja
MIN_DURATION=20    # Minimalny czas trwania testu iPerf (w sekundach)
MAX_DURATION=30    # Maksymalny czas trwania testu iPerf (w sekundach)
MIN_BURST=3    # Minimalna przepustowość w 
MAX_BURST=8    # Maksymalna przepustowość w 
MIN_INTERVAL=1     # Minimalny interwał raportowania (w sekundach)
MAX_INTERVAL=5     # Maksymalny interwał raportowania (w sekundach)
NUMBER_OF_SERVERS=9 # Liczba portów na serwerze iPerf
GENERATION_INTERVAL=5 # Czas między kolejnymi testami iPerf
ITERATIONS=1       # Liczba iteracji generowania ruchu
 
 
# Funkcja do uruchamiania klientów iPerf
run_iperf_client() {
    local client=$1
    local server=$2
    local duration=$((RANDOM % (MAX_DURATION - MIN_DURATION + 1) + MIN_DURATION))
    local burst_size=$((RANDOM % (MAX_BURST - MIN_BURST + 1) + MIN_BURST))
    local interval=$((RANDOM % (MAX_INTERVAL - MIN_INTERVAL + 1) + MIN_INTERVAL))
    local port=$((RANDOM % NUMBER_OF_SERVERS + 1))
 
    echo "Uruchamiam klienta $client, który wysyła dane do $server na porcie 100$port przez $duration sekund dla ${burst_size}G"
    xterm -hold -e "iperf3 -c $server -t $duration -w ${burst_size}G -i $interval -p 100$port " &
}
 
# Główna funkcja generująca ruch
generate_random_traffic() {
    run_iperf_servers
 
    for i in $(seq 1 $ITERATIONS); do
        client=$(shuf -n 1 -e h1)
        server=$(shuf -n 1 -e 10.0.0.3)
        run_iperf_client "$client" "$server"
        sleep $GENERATION_INTERVAL
    done
}
 
# Wywołanie głównej funkcji
generate_random_traffic
