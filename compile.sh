mkdir bin && mkdir bin/client && mkdir bin/server
javac -d bin/client/ -cp . client/*.java client/services/*.java
cp -r client/PubKeys bin/client/
cp client/*.ks bin/client
javac -d bin/server/ -cp . server/*.java server/persistence/*.java server/services/*.java
cp -r server/PubKeys bin/server/
cp server/*.ks bin/server
cp client.policy bin/client
cp server.policy bin/server
cp CLIENT_MANIFEST.MF bin/client/
cp SERVER_MANIFEST.MF bin/server/
cd bin/client
jar -cvfm SeiTchiz.jar CLIENT_MANIFEST.MF *
cd ..
cd server
jar -cvfm SeiTchizServer.jar SERVER_MANIFEST.MF *
