function descriptor()
    return {
        title = "PlaybackSync",
        version = "0.1",
        author = "Denis Mekhanikov",
        capabilities = {}
    }
end

widgets = {
    dialog = nil,
    host_input = nil,
    port_input = nil,
    connect_button = nil,
    disconnect_button = nil,

    play_button = nil,
    seek_input = nil,
    seek_button = nil
}

socket = nil

function activate()
    widgets.dialog = vlc.dialog("Playback Synchronization")
    widgets.dialog:add_label("Host:", 1, 1)
    widgets.dialog:add_label("Port:", 2, 1)
    widgets.host_input = widgets.dialog:add_text_input("", 1, 2)
    widgets.port_input = widgets.dialog:add_text_input("7773", 2, 2)
    widgets.connect_button = widgets.dialog:add_button("Connect", connect, 3, 2)


    widgets.dialog:add_button("Play", vlc.playlist.play, 1, 3)
    widgets.dialog:add_button("Pause", vlc.playlist.pause, 2, 3)
    widgets.seek_input = widgets.dialog:add_text_input("", 1, 4)
    widgets.seek_button = widgets.dialog:add_button("Seek",
        function() seek(widgets.seek_input:get_text()) end,
        2, 4)
    widgets.dialog:show()
end

function deactivate()
end

function close()
    vlc.deactivate()
end

function connect()
    local host = widgets.host_input:get_text()
    local port = widgets.port_input:get_text()
    -- connect
    vlc.msg.info("Connected to " .. host .. ":" .. port)
    widgets.dialog:del_widget(widgets.connect_button)
    widgets.connect_button = nil
    widgets.disconnect_button = widgets.dialog:add_button("Disconnect", disconnect, 3, 2)
end

function disconnect()
    -- disconnect
    vlc.msg.info("Connection closed")
    widgets.dialog:del_widget(widgets.disconnect_button)
    widgets.disconnect_button = nil
    widgets.connect_button = widgets.dialog:add_button("Connect", connect, 3, 2)
end

function seek(time)
    local input = vlc.object.input()
    vlc.var.set(input, "time", time)
end
