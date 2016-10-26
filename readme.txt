# Los siguientes comandos deben ser ejecutados desde la raíz del proyecto:

# Para compilar, ejecutar:
mvn clean assembly:assembly

# Para compilar salteando los tests, ejecutar:
mvn clean assembly:assembly -Dmaven.test.skip=true

# Los parámetros entre [] son opcionales
# Los parámetros entre <> son obligatorios
# Para correr un nodo, ejecutar:
java [-Xmx2g] [-Djgroups.bind_addr=<ip_a_bindear>] [-Djava.net.preferIPv4Stack=true] -jar target/signal-1.0-jar-with-dependencies.jar <port> <threads>
