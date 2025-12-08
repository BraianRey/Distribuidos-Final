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
    
    // Set para mantener los usuarios activos (escuchando)
    const usuariosActivos = new Set();

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

    function actualizarListaUsuariosActivos() {
        const contenedor = document.getElementById('listaUsuariosActivos');
        if (!contenedor) return;

        contenedor.innerHTML = '';

        if (usuariosActivos.size === 0) {
            const noUsersText = document.createElement('p');
            noUsersText.className = 'no-users-text';
            noUsersText.textContent = 'No hay usuarios escuchando esta canción';
            contenedor.appendChild(noUsersText);
        } else {
            usuariosActivos.forEach((nickname) => {
                const badge = document.createElement('div');
                badge.className = 'user-badge';
                
                const icon = document.createElement('span');
                icon.className = 'user-badge-icon';
                icon.textContent = '🎧';
                
                const name = document.createElement('span');
                name.textContent = nickname;
                
                badge.appendChild(icon);
                badge.appendChild(name);
                contenedor.appendChild(badge);
            });
        }
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
        
        // Agregar usuario a la lista de activos
        usuariosActivos.add(nicknamePropio);
        actualizarListaUsuariosActivos();
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
        
        // Desuscribirse de todos los canales al pausar
        liberarSuscripciones();
        console.log('Desuscrito de reacciones y notificaciones al pausar.');
        
        // Limpiar toda la lista de usuarios activos cuando se pausa
        usuariosActivos.clear();
        actualizarListaUsuariosActivos();
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
                // Verificar si es un usuario nuevo o uno que ya estaba
                const esUsuarioNuevo = !usuariosActivos.has(evento.nickname);
                
                // Agregar a la lista de usuarios activos
                usuariosActivos.add(evento.nickname);
                actualizarListaUsuariosActivos();
                
                // Solo mostrar burbuja si estamos reproduciendo Y es un usuario nuevo
                if (reproduciendoCancion && esUsuarioNuevo) {
                    mostrarBurbuja('usuariosReproduciendo', `${evento.nickname} inició la canción`, 'start');
                }
                
                // Si estamos reproduciendo, responder con nuestro estado para que el nuevo usuario nos vea
                // Solo responder si detectamos que es un usuario nuevo (para evitar loops)
                if (reproduciendoCancion && esUsuarioNuevo && clienteChat && clienteChat.connected) {
                    setTimeout(() => {
                        avisarPlay(true);
                    }, 100);
                }
            } else if (evento.reproduciendo === false) {
                // Remover de la lista de usuarios activos
                usuariosActivos.delete(evento.nickname);
                actualizarListaUsuariosActivos();
                
                // Solo mostrar burbuja si estamos reproduciendo
                if (reproduciendoCancion) {
                    mostrarBurbuja('usuariosDetenidos', `${evento.nickname} detuvo la canción`, 'stop');
                }
            }
        } catch (error) {
            console.error('Error procesando evento de reproducción:', error);
        }
    }

    function manejarReaccion(message) {
        // No procesar reacciones si no estamos reproduciendo
        if (!reproduciendoCancion) {
            return;
        }
        
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

        // Volver a suscribirse al reanudar la reproducción
        conectarYSuscribirse(idCancionActual, () => {
            iniciarReproduccionAuto(idCancionActual);
        }, true);
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
