function descriptor()
    return { title = "PlaybackSync" ;
        version = "0.1" ;
        author = "Denis Mekhanikov" ;
        capabilities = {} }
end

function activate()
    dialog = vlc.dialog("Playback Synchronization")
    dialog:add_label("Host:", 1, 1)
    dialog:add_label("Port:", 2, 1)
    host_input = dialog:add_text_input("", 1, 2)
    port_input = dialog:add_text_input("7773", 2, 2)
    connect_button = dialog:add_button("Connect", connect, 3, 2)
    dialog:show()
end

function connect()
    local host = host_input:get_text()
    local port = port_input:get_text()
end

function deactivate()
end

function close()
    vlc.deactivate()
end
