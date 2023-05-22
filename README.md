# Public-Ledger-for-Auctions

Aplicação de leilões que integra uma rede peer-to-peer e sistema de Blockchain para guardar dados.

Rede p2p segue o algoritmo Kademlia com algumas alterações que visam o problema em mão.
A BlockChain usa Proof-of-Work como método de consenso e resolve divergências.

Ler SSD.pdf para mais detalhes.


## Test Instructions

1. sudo chmod +x runBoot.sh
2. sudo chmod +x run.sh

### Run the bootstrap node:
3. ./runBoot.sh <port>

### Run the clients: 
4. ./run.sh <ip> <port>

