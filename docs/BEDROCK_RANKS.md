# Rangos legibles en Bedrock

CSDMVisuals 0.3.15 adapta las etiquetas CSDM para los receptores detectados por
Floodgate cuando `bedrock-text.enabled` está activado. Usa colores uniformes y
conserva la negrita configurada en los rangos y elimina la cursiva de las
etiquetas, evitando los saltos del degradado. El nombre de cuenta en chat y
lista de jugadores mantiene la base sin negrita ni cursiva; los separadores no
heredan la negrita de las etiquetas. Los mensajes de entrada conservan la
negrita de su plantilla original.

| Rango | Color Bedrock |
| --- | --- |
| Usuario | Gris |
| Personalidad | Dorado |
| Mod | Azul |
| Coordinación, Dirección, Élite | Aqua |
| Leyenda | Púrpura claro |
| Inmortal | Dorado |

Se aplica al nombre mostrado en chat, a los prefijos/sufijos de los equipos
`csdm_t_` y a los nombres de la lista de jugadores. También simplifica los
mensajes de entrada con la plantilla `✦ RANGO · ...`. Conserva el contenido,
acentos, eventos interactivos, identidad de jugador, colisiones y visibilidad.
No cambia los componentes enviados a Java. Los rangos personalizados que no
coincidan con las etiquetas conocidas mantienen su formato.

El renderer de chat adapta exclusivamente el nombre mostrado antes de pasar el
mensaje al renderer anterior; no recolorea las palabras escritas por el jugador.
Las sustituciones de símbolos de 0.3.12 continúan aplicándose aparte.

Instalar con `bash scripts/actualizar-visuals.sh SHA_COMPLETO` y volver a entrar
en Bedrock y Java. Comprobar Dirección, Personalidad, un rango de prestigio,
chat y entrada/salida. La comprobación visual real requiere el despliegue;
las pruebas automatizadas cubren degradados MiniMessage, preservación de
identidad/estilos Java y conservación de la regla de colisiones.
