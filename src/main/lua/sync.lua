-- TODO get host and port from config
host = "localhost"
port = 7773
message_type_size = 1
message_pos_size = 4
sleep_duration = 2500
play_message = string.char(1)
pause_message = string.char(2)

status = {
    play_status = false
}

vlc.msg.info("Sync interface")

socket = vlc.net.connect_tcp(host, port)

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
        process_changes()
    end
    return message
end

function write(fd, data)
    vlc.net.send(fd, data)
end

function seek(time)
    local input = vlc.object.input()
    vlc.var.set(input, "time", time)
end

function process_changes()
    local play_status = vlc.playlist.status()
    if status.play_status ~= play_status then
        if play_status == "playing" then
            handle_play()
        else
            handle_pause()
        end
        update_status()
    end
end

function handle_play()
    vlc.msg.dbg("Sending play message")
    write(socket, play_message)
end

function handle_pause()
    vlc.msg.dbg("Sending pause message")
    write(socket, pause_message)
end

function update_status()
    local play_status = vlc.playlist.status()
    status.play_status = play_status
end

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
    update_status()
end
