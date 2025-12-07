(function () {
    const DEFAULT_WS_URL = 'http://localhost:8081/ws';
    const CONFIG = window.__REACTIONS_CONFIG__ || {};
    const WS_ENDPOINT = CONFIG.wsUrl || DEFAULT_WS_URL;
    const KNOWN_TITLE_TO_ID = Object.freeze({
        'cancion1.mp3': 1,
        'cancion2.mp3': 2
    });

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
    let idCancionActual = null;
    let suscripcionesActivas = [];
    let isConnecting = false;
    const pendingConnectionCallbacks = [];

    document.addEventListener('DOMContentLoaded', () => {
        inicializarUI();
    });

    function inicializarUI() {
        document.querySelectorAll('.reaction-btn').forEach((boton) => {
            boton.addEventListener('click', () => enviarReaccionPrivada(boton.dataset.reaction));
        });

        const nicknameInput = document.getElementById('nicknameOrigen');
        if (nicknameInput) {
            nicknameInput.addEventListener('input', actualizarEstadoControles);
        }

        const audioPlayer = document.getElementById('audio-player');
        if (audioPlayer) {
            audioPlayer.addEventListener('play', handleAudioPlay);
            audioPlayer.addEventListener('pause', handleAudioPause);
            audioPlayer.addEventListener('ended', handleAudioPause);
        }

        actualizarEstadoControles();
    }

    function actualizarEstadoControles() {
        const conectado = clienteChat && clienteChat.connected;
        const nicknameInput = document.getElementById('nicknameOrigen');

        if (nicknameInput) {
            nicknameInput.disabled = conectado;
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
        if (idCancionActual !== null) {
            return idCancionActual;
        }
        return null;
    }

    function conectarY(callback) {
        if (typeof callback === 'function') {
            if (clienteChat && clienteChat.connected) {
                callback();
                return;
            }
            pendingConnectionCallbacks.push(callback);
        }

        if (clienteChat && clienteChat.connected) {
            while (pendingConnectionCallbacks.length) {
                const cb = pendingConnectionCallbacks.shift();
                try { cb(); } catch (err) { console.error(err); }
            }
            return;
        }

        if (isConnecting) {
            return;
        }

        const nickname = obtenerNickname();
        if (!nickname) {
            alert('Ingresa un nickname para conectarte.');
            pendingConnectionCallbacks.length = 0;
            return;
        }

        nicknamePropio = nickname;
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            alert('Las librerías SockJS/STOMP no se cargaron correctamente. Verifica las etiquetas de script.');
            pendingConnectionCallbacks.length = 0;
            return;
        }

        const socket = new SockJS(WS_ENDPOINT);
        clienteChat = Stomp.over(socket);
        isConnecting = true;

        clienteChat.connect({ nickname }, () => {
            isConnecting = false;
            actualizarEstadoControles();
            while (pendingConnectionCallbacks.length) {
                const cb = pendingConnectionCallbacks.shift();
                try { cb(); } catch (err) { console.error(err); }
            }
        }, (error) => {
            isConnecting = false;
            pendingConnectionCallbacks.length = 0;
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

    function conectarYSuscribirse(idCancion, callback, force = false) {
        if (idCancion === null) {
            return;
        }
        conectarY(() => {
            const debeResuscribirse = force || idCancionActual !== idCancion || suscripcionesActivas.length === 0;
            idCancionActual = idCancion;
            if (debeResuscribirse) {
                suscribirseACanales(idCancion);
            }
            if (typeof callback === 'function') {
                callback();
            }
        });
    }

    function iniciarReproduccionAuto(idCancion) {
        conectarYSuscribirse(idCancion, () => {
            if (!reproduciendoCancion) {
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
        if (!clienteChat || !clienteChat.connected || idCancionActual === null) {
            return;
        }

        const payload = {
            nickname: nicknamePropio,
            idCancion: idCancionActual,
            reproduciendo: estado
        };

        clienteChat.send(DESTINATIONS.avisarPlay, {}, JSON.stringify(payload));
    }

    function enviarReaccionPrivada(tipo) {
        if (!clienteChat || !clienteChat.connected || !reproduciendoCancion || idCancionActual === null) {
            alert('Debes estar conectado y reproduciendo una canción para enviar reacciones.');
            return;
        }

        const emoji = EMOJIS_REACCION[tipo] || '🎵';
        const nicknameDestinoInput = document.getElementById('nicknameDestino');
        const nicknameDestino = nicknameDestinoInput ? nicknameDestinoInput.value.trim() : '';

        const payload = {
            nicknameOrigen: nicknamePropio,
            idCancion: idCancionActual,
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

    function inferirIdDesdeTitulo(titulo) {
        if (!titulo) {
            return null;
        }
        const normalizado = titulo.trim().toLowerCase();
        if (Object.prototype.hasOwnProperty.call(KNOWN_TITLE_TO_ID, normalizado)) {
            return KNOWN_TITLE_TO_ID[normalizado];
        }
        const match = normalizado.match(/(\d+)/);
        return match ? parseInt(match[1], 10) : null;
    }

    function setCancionDesdeTitulo(titulo) {
        const id = inferirIdDesdeTitulo(titulo);
        if (!id) {
            console.warn('No se pudo inferir el id de la canción para reacciones.', titulo);
            return null;
        }
        conectarYSuscribirse(id, undefined, true);
        return id;
    }

    function handleAudioPlay() {
        if (idCancionActual === null) {
            console.warn('No hay id de canción para sincronizar reproducciones.');
            return;
        }

        iniciarReproduccionAuto(idCancionActual);
    }

    function handleAudioPause() {
        if (reproduciendoCancion) {
            detenerReproduccion();
        }
    }

    function notifyPlay() {
        if (idCancionActual === null) {
            console.warn('No hay id de canción para notificar play.');
            return;
        }
        iniciarReproduccionAuto(idCancionActual);
    }

    function notifyPause() {
        if (reproduciendoCancion) {
            detenerReproduccion();
        }
    }

    function startPlaybackFlow(titulo) {
        let id = null;
        if (titulo) {
            id = setCancionDesdeTitulo(titulo);
        }
        if (id === null) {
            id = idCancionActual ?? obtenerIdCancion();
        }
        if (id === null) {
            console.warn('No fue posible iniciar la reproducción automática: falta id de canción.');
            return;
        }
        // No llamar a iniciarReproduccionAuto aquí, solo preparar
        // El usuario debe presionar play en el reproductor
    }

    if (typeof window !== 'undefined') {
        window.__reaccionesIntegration = {
            setCancionDesdeTitulo,
            startPlaybackFlow,
            notifyPlay,
            notifyPause
        };
    }
})();
