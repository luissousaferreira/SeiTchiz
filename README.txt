Luís Ferreira 49495
Xavier Cordeiro 46365

Testado em Linux

(OBRIGATÓRIO)
Compilar: 
 > sh compile.sh

Executar:

Obrigatorio enviar sempre todos os argumentos

Servidor:
 
 Na pasta bin/server:
 
 > java -jar -Djava.security.manager -Djava.security.policy=server.policy SeiTchizServer.jar <port> <keystore> <keystore-password>
 
Cliente:

 Na pasta bin/client:
 
 > java -jar -Djava.security.manager -Djava.security.policy=client.policy SeiTchiz.jar <serverAddress> <truststore> <keystore> <keystore-password> <clientID>
