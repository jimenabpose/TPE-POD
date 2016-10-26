Preprocesamiento de señales
---------------------------

Construccion
------------

Ejecutar: mvn package
En la carpeta target se creara el jar signal-1.0.jar con todas las clases.

El proyecto no tiene dependencias externas.


Clases y paquetes destacables
-----------------------------

Toda la interfaz publica se encuentra en el paquete ar.edu.itba.pod.api
Las dos interfases más importantes son SignalProcesor y SPNode.

En ar.edu.itba.pod.impl se encuentra una implementación de referencia del API en un
solo hilo de ejecucion.

En los directorios de test (src/test/java) se encuentran, ademas de algunos casos de prueba 
unitarios:
SampleClient y SampleServer, que muestran el uso de las interfases a través de RMI.
SideBySideTester, que permite comparar una implementacion contra la de referencia para
verificar que la funcionalidad permanece intacta. Ver un ejemplo rudimentario

