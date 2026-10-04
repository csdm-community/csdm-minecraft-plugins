# Tipografía Bedrock (0.3.12)

La prueba en el cliente Bedrock de CSDM mostró que `ARCHIVO CSDM - Texto de prueba`
conserva la fuente habitual, mientras que `✦ ARCHIVO CSDM • Texto de prueba`
se renderiza con una fuente más pequeña. No depende de la escala de los jugadores.

CSDMVisuals adapta la presentación **por destinatario Floodgate**, sin cambiar
configuraciones de rangos, nombres de cuenta ni mensajes almacenados:

| Símbolo original | Presentación Bedrock |
| --- | --- |
| `✦` | `*` |
| `•` | `\|` |

Los acentos, ñ, colores, degradados y estilos Adventure se conservan. Java y la
consola conservan los componentes originales. Otros símbolos no se eliminan:
no se pretende garantizar compatibilidad de cualquier carácter Unicode.

## Alcance

- Chat: envuelve el renderer de CSDMCommunity, ya registrado por orden de
  `softdepend`. Adapta la presentación para cada observador, sin modificar el
  cuerpo firmado ni cancelar/retransmitir mensajes de jugadores.
- Mensajes de sistema (incluidos entrada, salida, verificación y action bar):
  adapta los dos símbolos en el paquete saliente a Bedrock. También afecta a
  mensajes de otros plugins y `/tellraw` que contengan esos símbolos.
- Etiquetas: adapta únicamente equipos cuyo ID empieza por `csdm_t_`.
- Lista de jugadores: adapta los nombres visibles, nunca el perfil ni UUID.

La adaptación es visual; si un jugador escribe uno de esos dos símbolos, su
representación también cambia para los destinatarios Bedrock. Los originales
permanecen intactos. No requiere un paquete de recursos.

## Activación y actualización

Actualizar **CSDMVisuals.jar** a 0.3.12 con el servidor detenido y respaldando el
JAR anterior. No hace falta actualizar los otros cuatro plugins ni Geyser.
El arranque normal garantiza el orden de registro de los listeners; no usar
gestores de recarga en caliente. Floodgate debe estar instalado en Paper,
incluido cuando Geyser corre en Standalone.

La opción `bedrock-text.enabled` se considera `true` si no existe en una
configuración anterior. Es independiente de `enabled` (escala del lobby) y de
`bedrock-viewers-enabled` (escala vista desde Bedrock). No cambia ninguno de ellos.

Para desactivar la adaptación, añadir al config.yml de CSDMVisuals:

```yaml
bedrock-text:
  enabled: false
```

Ejecutar `/csdmvisuals` para recargar. Volver a entrar para renovar las etiquetas
y lista de jugadores ya enviadas. El historial de chat no se reescribe.

## Prueba en servidor

1. Entrar simultáneamente desde Java y Bedrock al museo.
2. Comparar un mensaje nuevo de entrada y un mensaje de chat con rango.
3. Ver la etiqueta de Dirección desde ambos clientes: Java conserva `•`,
   Bedrock recibe `|`; ambos conservan el acento y los colores.
4. Confirmar que la identidad `.sepultacion1` sigue vinculada y que el tamaño
   visual del lobby conserva su configuración anterior.

Las pruebas automatizadas comprueban componentes anidados, estilos, caracteres
españoles e independencia de los objetos de equipos/lista compartidos con Java.
La apariencia final y la traducción por Geyser requieren esta prueba con clientes.
