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
message_type_size = 1
message_pos_size = 4

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
    disconnect()
end

function connect()
    local host = widgets.host_input:get_text()
    local port = widgets.port_input:get_text()
    socket = vlc.net.connect_tcp(host, port)
    communicate()

    widgets.dialog:del_widget(widgets.connect_button)
    widgets.connect_button = nil
    widgets.disconnect_button = widgets.dialog:add_button("Disconnect", disconnect, 3, 2)
end

function disconnect()
    if widgets.disconnect_button then
        widgets.dialog:del_widget(widgets.disconnect_button)
        widgets.disconnect_button = nil
        widgets.connect_button = widgets.dialog:add_button("Connect", connect, 3, 2)
    end
    if socket then
        vlc.net.close(socket)
        socket = nil
    end
    vlc.msg.info("Connection closed")
end

-- TODO: do polling
function communicate()
    while socket do
        read_command()
    end
end

--[[
    1 byte: code
    01: play
    02: pause
    03: seek position
        4 bytes: position in seconds
]]
function read_command()
    local message_type = read(socket, message_type_size):byte(1)
    if message_type == 1 then
        vlc.msg.dbg("Play command received")
        vlc.playlist.play()
    elseif message_type == 2 then
        vlc.msg.dbg("Pause command received")
        vlc.playlist.pause()
    elseif message_type == 3 then
        vlc.msg.dbg("Seek message received")
        -- TODO: read position and seek
    else
        vlc.msg.dbg("Message of unknown type received")
    end
end

function read(fd, length)
    local left = length
    local message = ""
    while left ~= 0 do
        local s = vlc.net.read(fd, left)
        if s then
            message = message .. s
            left = left - s:len()
        end
    end
    return message
end

function seek(time)
    local input = vlc.object.input()
    vlc.var.set(input, "time", time)
end
