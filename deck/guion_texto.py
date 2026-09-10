"""
Texto hablado del guion.

Reglas que este archivo debe cumplir (build_guion.py las verifica):
  1. Cero dos puntos, punto y coma y guiones largos. Nada de eso se pronuncia, y
     al leerlo en voz alta corta el discurso.
  2. Cada párrafo abre enlazado con el anterior, y dentro del párrafo las ideas
     se unen con porque, así que, mientras que, de manera que, sin embargo,
     aunque, además. No se yuxtaponen frases sueltas.
  3. Español hablado y adulto. Sin jerga jurídica y sin analogías infantiles.
  4. No se repite lo que ya está escrito en las diapositivas.
"""

BLOQUES = [
    dict(n=1, titulo="Portada",
         pantalla="Título y nombres del grupo.",
         texto=[
             "Buenas tardes. Voy a presentarles el trabajo del Grupo 1 sobre el "
             "**contrato de franquicia**.",

             "Cualquier empresa que quiere crecer se topa tarde o temprano con el "
             "mismo problema, y es que abrir locales nuevos cuesta dinero, "
             "mientras que el dinero propio siempre alcanza hasta cierto punto, "
             "así que lo habitual es endeudarse o crecer más despacio.",

             "Y la franquicia aparece como una tercera salida, porque permite "
             "crecer con el dinero de otros. Alguien más pone la inversión y "
             "opera el local bajo tu marca, de manera que la red se expande sin "
             "que el dueño de la marca tenga que financiarla.",

             "Eso tiene un precio alto, porque hay que entregarle a un tercero lo "
             "único que sostiene el valor de la empresa, que es su nombre y su "
             "forma de trabajar, y confiar en que lo hará igual de bien en una "
             "ciudad donde uno no está para vigilarlo.",

             "Justamente por eso existe el contrato, porque es lo que vuelve "
             "manejable ese riesgo, y es lo que voy a explicarles en estos quince "
             "minutos.",
         ]),

    dict(n=2, titulo="Definición conceptual",
         pantalla="Dos tarjetas: «La franquicia» y «El contrato de franquicia». "
                  "Abajo, tres países.",
         texto=[
             "Cuando una empresa franquicia, lo que vende no es un producto sino "
             "la capacidad de repetir su negocio, y eso se compone de tres cosas "
             "distintas.",

             "La primera y más evidente es la marca, que le ahorra al local nuevo "
             "todos los años que cuesta lograr que la gente confíe en un nombre, "
             "porque el cliente entra ya sabiendo qué va a encontrar.",

             "Después viene el **know-how**, y ahí está el valor de verdad. Es un "
             "término del inglés que significa saber hacer, y se refiere a los "
             "procedimientos que la empresa fue afinando con los años, desde cómo "
             "se prepara cada producto hasta cuánta gente hace falta en cada "
             "turno. Nada de eso se puede "
             "patentar, porque no es un invento sino experiencia acumulada a base "
             "de errores caros. Cuando la empresa franquicia, todo eso se pone "
             "por escrito y se le entrega a alguien de afuera, y por eso las "
             "cláusulas de confidencialidad son tan duras, ya que el franquiciado "
             "lo puede usar pero no divulgar, incluso después de que el contrato "
             "termine.",

             "Y por último está la forma de operar, es decir cómo se ve el local, "
             "cómo se atiende y cómo se hace la publicidad, que es lo que logra "
             "que dos locales con dueños distintos le resulten iguales al "
             "cliente. Las tres cosas juntas son la franquicia.",

             "El contrato, en cambio, es el documento donde queda fijado cuánto "
             "se paga, por cuánto tiempo, en qué zona se opera y qué pasa si "
             "alguien incumple, y tiene dos rasgos particulares.",

             "El primero es que no se cumple y se termina. En una compraventa uno "
             "paga, el otro entrega y ahí queda cerrada la relación, mientras que "
             "acá sigue viva con pagos cada mes, asistencia y supervisión, así "
             "que se parece más a un arriendo que a una compra.",

             "El segundo es menos evidente, y es que el contrato está "
             "**incompleto a propósito**. Ninguno alcanza a prever que en ocho "
             "años la gente compre de otra manera, pero eso no significa que el "
             "abogado lo haya redactado mal, porque si amarrara todas las "
             "variables la red no podría moverse cuando el mercado se mueve.",

             "A esto hay que sumarle que es un **contrato marco**, porque no "
             "regula una operación puntual sino la entrada a una red, así que las "
             "obligaciones no salen solo del texto que firmó sino también de "
             "operar el negocio según ese método.",

             "Y hay una advertencia final que cambia según el país. **Argentina** "
             "incorporó la franquicia a su Código Civil y Comercial, así que si "
             "hay un pleito existen artículos que aplicar, y **Ecuador** la "
             "reconoce en su Código de Comercio. **Colombia y Perú**, en cambio, "
             "la tratan como contrato atípico, o sea que la ley no la menciona, y "
             "eso implica que todo lo que el contrato no diga no lo dice nadie, "
             "de manera que ahí el contrato no respalda el acuerdo sino que es el "
             "acuerdo.",
         ]),

    dict(n=3, titulo="¿Cómo se aplica?",
         pantalla="Cinco etapas numeradas. Abajo, «20 días» y «La fórmula del "
                  "modelo».",
         texto=[
             "Veamos entonces cómo funciona esto en la práctica, porque el "
             "proceso tiene cinco etapas y la primera no es firmar sino elegir.",

             "La marca no le entrega una franquicia a cualquiera que tenga el "
             "dinero, sino que evalúa candidatos con cuidado, porque cada local "
             "que abre va a llevar su nombre en la fachada. De hecho, los "
             "estudios coinciden en que es una de las decisiones que más "
             "determina si la red funciona, porque el riesgo no se reparte por "
             "igual, ya que si el franquiciado opera mal él pierde su inversión "
             "pero la marca pierde prestigio en todo el mercado.",

             "Una vez que hay acuerdo se firma, y ahí no solo se pacta el precio "
             "sino también la salida, es decir cuánto dura, cómo se renueva y en "
             "qué casos se puede cortar antes. Todo eso se define al principio, "
             "mientras las dos partes se están llevando bien, porque después va a "
             "ser mucho más difícil ponerse de acuerdo.",

             "Luego viene la etapa que nadie ve y que es de las más importantes, "
             "que es la capacitación. Se entrena al dueño y al personal, se "
             "entregan los manuales y se acompaña la apertura, así que es acá "
             "donde el know-how cambia de manos de verdad.",

             "Cuando el local abre, el franquiciado lleva el negocio completo, "
             "porque contrata, paga la nómina, atiende y responde por el "
             "arriendo, aunque dentro de reglas que no escribió él.",

             "Y la marca no desaparece después de la firma, sino que vuelve. "
             "Visita el local, revisa los estándares y audita las instalaciones y "
             "a veces los libros contables. Es incómodo, pero está aceptado desde "
             "el primer día porque cuando algo sale mal el cliente no culpa al "
             "dueño del local sino a la marca, de modo que el daño se reparte "
             "entre toda la red.",

             "Ahora, antes de todo este ciclo hay una obligación que para mí es "
             "lo más interesante de la figura, y es que la marca tiene que "
             "entregarle al futuro franquiciado información real del negocio, o "
             "sea quién es, cómo está el sector y cómo está armada la red. Además "
             "debe hacerlo con **veinte días hábiles** de anticipación, para que "
             "alcance a decidir con todo sobre la mesa, y existe porque el que "
             "ofrece conoce el negocio a fondo mientras el que invierte casi no "
             "lo conoce. Y si esa información se esconde o se maquilla hay "
             "responsabilidad por daños **aunque el contrato no llegue a "
             "firmarse**.",
         ]),

    dict(n=4, titulo="Partes involucradas",
         pantalla="Dos tarjetas, franquiciador y franquiciado. Abajo, la franja "
                  "del subfranquiciante.",
         texto=[
             "En esta relación intervienen dos partes, y como los nombres se "
             "parecen tanto suelen confundirse. El **franquiciador**, que también "
             "se llama franquiciante u otorgante, es el dueño de la marca, "
             "mientras que el **franquiciado**, o franquiciatario, es quien abre "
             "y opera el local.",

             "Lo que aporta el dueño de la marca no se puede tocar, porque son el "
             "nombre, el know-how, los manuales y la asistencia técnica. Y para "
             "franquiciar tuvo que haber operado su propio negocio con éxito "
             "durante años, así que no se franquicia una idea sino un historial, "
             "y eso es lo que define cuánto vale.",

             "A cambio cobra de dos maneras que no son lo mismo. El **canon de "
             "entrada** es un pago único al principio por el derecho a entrar en "
             "la red, mientras que las **regalías** se repiten cada mes y "
             "normalmente son un porcentaje de las ventas. Pero además del dinero "
             "recibe control, porque puede exigir que el local se vea igual, que "
             "se opere igual y que el know-how no salga de ahí.",

             "Del otro lado, el franquiciado pone el capital y la gestión, es "
             "decir la inversión inicial, las regalías, el día a día y el equipo, "
             "y aporta además algo que la marca no tiene, porque él sabe cómo "
             "compra la gente de su ciudad.",

             "Lo que recibe a cambio es protección, y la más importante suele ser "
             "la **exclusividad territorial**, es decir el "
             "compromiso de que la marca no va a abrir otro local en su zona "
             "dividiéndole la clientela, aunque si no quedó escrito no existe. Y "
             "entre sus obligaciones está no competir con la red mientras el "
             "contrato esté vigente.",

             "Acá aparece la confusión más común de todas, porque aunque siga "
             "manuales ajenos, aunque lo auditen y aunque no fije libremente sus "
             "precios, el franquiciado **no es un empleado** sino un empresario "
             "independiente, de modo que si el local quiebra la pérdida es suya y "
             "no de la marca.",

             "De esa independencia sale el punto que más se pelea en tribunales, "
             "que es quién responde cuando un cliente sufre un daño en el local. "
             "En principio responde el franquiciado, y que el aviso de la marca "
             "esté en la puerta no alcanza para responsabilizarla. Sin embargo, "
             "los tribunales "
             "españoles hicieron una excepción en clínicas de estética y "
             "odontología, porque si se prueba que el daño vino de una "
             "instrucción concreta de la marca entonces la marca sí responde.",

             "Por último, cuando la red sale al exterior aparece un tercero que "
             "compra los derechos de un país entero y después otorga franquicias "
             "ahí adentro, y aunque no es el dueño de la marca, frente a esos "
             "locales actúa como si lo fuera.",
         ]),

    dict(n=5, titulo="Ventajas y desventajas",
         pantalla="Seis ventajas y seis desventajas.",
         texto=[
             "En la diapositiva están los doce puntos, así que no los voy a leer "
             "uno por uno y prefiero explicarles la lógica de fondo, porque las "
             "dos columnas son la misma decisión vista desde lados opuestos.",

             "Lo que se gana se puede resumir en dos cosas. La primera es tiempo, "
             "porque uno no se pone a averiguar qué funciona sino que entra a "
             "algo que ya funciona, y eso explica que las franquicias sobrevivan "
             "más que los negocios independientes parecidos, ya que los errores "
             "caros los pagó otro antes. La segunda es escala, porque la "
             "publicidad, la capacitación y las compras se hacen para toda la "
             "red, así que un local que negocia cien unidades no consigue el "
             "mismo precio que una red que negocia cien mil.",

             "Lo que se cede son también dos cosas. Se cede margen, y de una "
             "forma que hay que entender bien, porque las regalías se calculan "
             "sobre **las ventas y no sobre la ganancia**, así que si un mes las "
             "ventas caen a la mitad la regalía se paga igual y un mes malo para "
             "el local no es un mes malo para la marca.",

             "Y se cede autonomía, porque el franquiciado puede ver una "
             "oportunidad real en su mercado y no poder aprovecharla si el manual "
             "no lo permite. Además hay una exposición que no controla en "
             "absoluto, ya que si la marca se golpea en su reputación, aunque sea "
             "por algo que pasó en otro país, su local pierde clientes esa semana "
             "aunque él haya trabajado impecable.",

             "Y ahí está el fondo del asunto, porque lo mismo que le da valor a "
             "la red es lo que le quita libertad al franquiciado, de manera que "
             "no hay uno sin el otro y lo único que hace el contrato es definir "
             "dónde se pone esa raya.",
         ]),

    dict(n=6, titulo="Síntesis e intro al video",
         pantalla="El invitado y las cifras de la operación.",
         texto=[
             "Para cerrar quiero dejarles tres conclusiones.",

             "**La primera** es que esto funciona porque ninguna de las dos "
             "partes podría sola, ya que una tiene la marca y el método pero no "
             "el capital ni el conocimiento de cada mercado, mientras que la otra "
             "tiene el capital y conoce su mercado pero no la marca ni el método.",

             "**La segunda**, y es la que más me llamó la atención, es que qué "
             "tan expuestas están las partes depende del país, porque donde la "
             "ley regula la figura hay un mínimo garantizado, pero donde no la "
             "regula, como en Colombia o en Perú, ese mínimo lo pone el contrato "
             "o no lo pone nadie, y por eso un contrato mal hecho ahí no es un "
             "problema de forma sino quedarse sin defensa.",

             "**Y la tercera** es que la independencia entre las partes es real "
             "aunque tiene un límite, porque se sostiene mientras la marca "
             "oriente, y cuando el control se vuelve tan fuerte que reemplaza la "
             "decisión del franquiciado esa independencia deja de existir.",

             "Ahora, todo esto sigue siendo teoría, así que para contrastarlo "
             "entrevistamos a **Gerardo Marcano**, vicepresidente de operaciones "
             "de Grupo David, que lleva veinte años ahí y hoy responde por más de "
             "cuarenta franquicias.",

             "Antes de ver el video quiero aclararles dos términos que él usa. "
             "Cuando habla de **brand awareness** se refiere a qué tanto una "
             "marca es conocida y deseada en un mercado, y para él eso define si "
             "se abre o no un local, mientras que **tropicalización** es adaptar "
             "el producto a cada mercado, o sea cambiar telas, colores y surtido "
             "según el clima y lo que la gente usa. Su ejemplo lo resume bien, "
             "porque mandar ropa de invierno a una ciudad cálida es perder la "
             "temporada.",

             "Presten atención sobre todo a hasta dónde se puede adaptar el "
             "modelo sin romper el estándar, porque lo que acá suena abstracto "
             "para él es una decisión de todos los días.",

             "Con esto cierro. Gracias.",
         ]),
]

CIERRE = [
    "La entrevista deja clara una cosa que en el análisis apenas se asoma, y es "
    "que lo que sostiene una franquicia no es el texto del contrato sino el rigor "
    "con que se repite el estándar y el criterio con que se adapta a cada "
    "mercado. Con gusto respondo sus preguntas.",
]

PREGUNTAS = [
    ("¿Cuánto cuesta una franquicia?",
     "Depende del sector y de qué tan fuerte sea la marca, pero la estructura es "
     "siempre la misma, porque hay un canon de entrada que es único y se paga al "
     "principio, y luego regalías mensuales que normalmente son un porcentaje de "
     "las ventas."),
    ("¿El franquiciado puede vender su local?",
     "Solo con autorización de la marca, y así queda escrito casi siempre, porque "
     "la marca quiere decidir quién entra a su red."),
    ("¿Quién responde si un cliente se lastima en el local?",
     "En principio el franquiciado, porque es un empresario independiente. Los "
     "tribunales españoles han responsabilizado a la marca cuando se prueba que "
     "el daño vino de una instrucción concreta que ella dio, así que tener "
     "estándares de calidad no basta y hay que probar que esa orden causó el "
     "daño."),
    ("¿Cuánto margen tiene el franquiciado para cambiar cosas?",
     "Necesita autorización. Un laudo arbitral colombiano, el caso PANACA, dejó "
     "dicho que el franquiciado debe respetar la filosofía de la marca, aunque "
     "también aclaró que los manuales no se pueden aplicar igual en cualquier "
     "ciudad."),
    ("¿En qué se diferencia de una licencia de marca?",
     "La licencia solo autoriza usar el nombre, mientras que la franquicia "
     "entrega además el know-how, los manuales, la capacitación y la asistencia "
     "continua, y viene con control sobre cómo se opera."),
    ("¿Por qué Colombia no lo ha regulado si es tan común?",
     "Hay una norma técnica voluntaria, la NTC 5813, y el Ministerio de Comercio "
     "redactó en 2021 un proyecto de decreto que incluía ese plazo de veinte "
     "días, pero cuando hicimos la revisión seguía sin expedirse."),
    ("¿Cuánto dura normalmente el contrato?",
     "Lo fija el contrato y cambia según el sector, aunque siempre trae cláusulas "
     "de renovación y de terminación, porque es una relación de largo plazo y por "
     "eso la salida se negocia desde el principio."),
]
