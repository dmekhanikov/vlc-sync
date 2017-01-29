-- TODO get host and port from config
host = "localhost"
port = 7773
message_type_size = 1
message_pos_size = 4
sleep_duration = 2500

vlc.msg.info("Sync interface")

function read(fd, length)
    local left = length
    local message = ""
    while left ~= 0 do
        local s = vlc.net.recv(fd, left)
        if s then
            message = message .. s
            left = left - s:len()
        end
        vlc.misc.mwait(vlc.misc.mdate() + sleep_duration)
    end
    return message
end

function seek(time)
    local input = vlc.object.input()
    vlc.var.set(input, "time", time)
end


socket = vlc.net.connect_tcp(host, port)
--[[
    1 byte: code
    01: play
    02: pause
    03: seek position
        4 bytes: position in seconds
]]
while true do
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
