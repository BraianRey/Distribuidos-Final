(function () {
    const DEFAULT_WS_URL = 'http://localhost:5000/ws';
    const CONFIG = window.__REACTIONS_CONFIG__ || {};
    const WS_ENDPOINT = CONFIG.wsUrl || DEFAULT_WS_URL;

    const DESTINATIONS = {
        enviarPrivado: '/apiChat/enviarMensajePrivado/',
        avisarPlay: '/apiChat/avisarPlay/',
        playTopic: (idCancion) => `/avisarPlay/${idCancion}`,
        reactionTopic: (idCancion) => `/chatPrivado/${idCancion}`
    };

    const EMOJIS_REACCION = {
        like: '👍',
        love: '❤️',
        angry: '😡',
        fun: '😄'
    };

    let clienteChat = null;
    let nicknamePropio = '';
    let reproduciendoCancion = false;
    let idCancionActual = '';
    let suscripcionesActivas = [];

    document.addEventListener('DOMContentLoaded', () => {
        inicializarUI();
    });

    function inicializarUI() {
        const playButton = document.getElementById('btnPlay');
        if (playButton) {
            playButton.addEventListener('click', toggleReproduccion);
        }

        document.querySelectorAll('.reaction-btn').forEach((boton) => {
            boton.addEventListener('click', () => enviarReaccionPrivada(boton.dataset.reaction));
        });

        const nicknameInput = document.getElementById('nicknameOrigen');
        if (nicknameInput) {
            nicknameInput.addEventListener('input', actualizarEstadoControles);
        }

        actualizarEstadoControles();
    }

    function actualizarEstadoControles() {
        const conectado = clienteChat && clienteChat.connected;
        const btnPlay = document.getElementById('btnPlay');
        const nicknameInput = document.getElementById('nicknameOrigen');
        const idCancionInput = document.getElementById('idCancion');

        if (nicknameInput) {
            nicknameInput.disabled = conectado;
        }
        if (idCancionInput) {
            idCancionInput.disabled = reproduciendoCancion;
        }

        if (btnPlay) {
            btnPlay.textContent = reproduciendoCancion ? '⏸' : '▶';
            btnPlay.disabled = !nicknameInput || !nicknameInput.value.trim();
        }

        document.querySelectorAll('.reaction-btn').forEach((boton) => {
            boton.disabled = !reproduciendoCancion;
        });
    }

    function obtenerNickname() {
        const nicknameInput = document.getElementById('nicknameOrigen');
        return nicknameInput ? nicknameInput.value.trim() : '';
    }

    function obtenerIdCancion() {
        const idInput = document.getElementById('idCancion');
        if (!idInput) {
            return '';
        }
        const valor = idInput.value.trim();
        if (!valor) {
            return '';
        }
        const numero = Number(valor);
        if (!Number.isInteger(numero) || numero <= 0) {
            alert('El ID de canción debe ser un número entero positivo.');
            return '';
        }
        return String(numero);
    }

    function conectarY(callback) {
        if (clienteChat && clienteChat.connected) {
            callback();
            return;
        }

        const nickname = obtenerNickname();
        if (!nickname) {
            alert('Ingresa un nickname para conectarte.');
            return;
        }

        nicknamePropio = nickname;
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            alert('Las librerías SockJS/STOMP no se cargaron correctamente. Verifica las etiquetas de script.');
            return;
        }

        const socket = new SockJS(WS_ENDPOINT);
        clienteChat = Stomp.over(socket);

        clienteChat.connect({ nickname }, () => {
            actualizarEstadoControles();
            callback();
        }, (error) => {
            console.error('Error al conectar:', error);
            alert('No se pudo establecer la conexión con el servidor de reacciones.');
        });
    }

    function suscribirseACanales(idCancion) {
        liberarSuscripciones();

        if (!clienteChat || !clienteChat.connected) {
            return;
        }

        suscripcionesActivas.push(
            clienteChat.subscribe(DESTINATIONS.playTopic(idCancion), manejarEstadoReproduccion),
            clienteChat.subscribe(DESTINATIONS.reactionTopic(idCancion), manejarReaccion)
        );
    }

    function liberarSuscripciones() {
        suscripcionesActivas.forEach((sub) => sub.unsubscribe());
        suscripcionesActivas = [];
    }

    function toggleReproduccion() {
        const idCancion = obtenerIdCancion();
        if (!idCancion) {
            return;
        }

        conectarY(() => {
            if (idCancionActual !== idCancion) {
                idCancionActual = idCancion;
                suscribirseACanales(idCancionActual);
            }

            if (reproduciendoCancion) {
                detenerReproduccion();
            } else {
                iniciarReproduccion();
            }
        });
    }

    function iniciarReproduccion() {
        if (reproduciendoCancion || !clienteChat || !clienteChat.connected) {
            return;
        }

        reproduciendoCancion = true;
        actualizarEstadoControles();
        avisarPlay(true);
        mostrarBurbuja('usuariosReproduciendo', `${nicknamePropio} inició la canción`, 'start');
    }

    function detenerReproduccion(silencioso = false) {
        if (!reproduciendoCancion) {
            return;
        }

        if (!silencioso) {
            avisarPlay(false);
        }

        reproduciendoCancion = false;
        actualizarEstadoControles();
        mostrarBurbuja('usuariosDetenidos', `${nicknamePropio} detuvo la canción`, 'stop');
    }

    function avisarPlay(estado) {
        if (!clienteChat || !clienteChat.connected || !idCancionActual) {
            return;
        }

        const payload = {
            nickname: nicknamePropio,
            idCancion: Number(idCancionActual),
            reproduciendo: estado
        };

        clienteChat.send(DESTINATIONS.avisarPlay, {}, JSON.stringify(payload));
    }

    function enviarReaccionPrivada(tipo) {
        if (!clienteChat || !clienteChat.connected || !reproduciendoCancion) {
            alert('Debes estar conectado y reproduciendo una canción para enviar reacciones.');
            return;
        }

        const emoji = EMOJIS_REACCION[tipo] || '🎵';
        const nicknameDestinoInput = document.getElementById('nicknameDestino');
        const nicknameDestino = nicknameDestinoInput ? nicknameDestinoInput.value.trim() : '';

        const payload = {
            nicknameOrigen: nicknamePropio,
            idCancion: Number(idCancionActual),
            reaction: tipo,
            contenido: emoji
        };

        if (nicknameDestino) {
            payload.nicknameDestino = nicknameDestino;
        }

        clienteChat.send(DESTINATIONS.enviarPrivado, {}, JSON.stringify(payload));
        const destinoTexto = nicknameDestino ? ` → ${nicknameDestino}` : '';
        mostrarBurbuja('reaccionesUsuarios', `${nicknamePropio}${destinoTexto}: ${emoji}`, 'reaction');
    }

    function manejarEstadoReproduccion(message) {
        try {
            const evento = JSON.parse(message.body);
            if (evento.nickname === nicknamePropio) {
                return;
            }

            if (evento.reproduciendo === true) {
                mostrarBurbuja('usuariosReproduciendo', `${evento.nickname} inició la canción`, 'start');
            } else if (evento.reproduciendo === false) {
                mostrarBurbuja('usuariosDetenidos', `${evento.nickname} detuvo la canción`, 'stop');
            }
        } catch (error) {
            console.error('Error procesando evento de reproducción:', error);
        }
    }

    function manejarReaccion(message) {
        try {
            const evento = JSON.parse(message.body);
            if (evento.nicknameDestino && evento.nicknameDestino !== nicknamePropio) {
                return;
            }

            const emoji = EMOJIS_REACCION[evento.reaction] || evento.contenido || '🎵';
            const destinoTexto = evento.nicknameDestino ? ` → ${evento.nicknameDestino}` : '';
            mostrarBurbuja('reaccionesUsuarios', `${evento.nicknameOrigen}${destinoTexto}: ${emoji}`, 'reaction');
        } catch (error) {
            console.error('Error procesando reacción:', error);
        }
    }

    function mostrarBurbuja(contenedorId, texto, tipo) {
        const contenedor = document.getElementById(contenedorId);
        if (!contenedor) {
            return;
        }

        const burbuja = document.createElement('span');
        burbuja.className = `bubble bubble--${tipo}`;
        burbuja.textContent = texto;
        contenedor.appendChild(burbuja);

        burbuja.addEventListener('animationend', () => {
            burbuja.remove();
        });
    }
})();
