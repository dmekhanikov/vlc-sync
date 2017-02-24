host = config.host or "localhost"
port = config.port or 7773
message_type_size = 1
message_time_size = 4
time_eps = 2
playing = "playing"

play_message_code = 1
pause_message_code = 2
seek_message_code = 3
ping_message_code = 127
play_message = string.char(play_message_code)
pause_message = string.char(pause_message_code)
seek_message = string.char(seek_message_code)
ping_message = string.char(ping_message_code)

status = {
    play_status = false,
    time = 0
}

vlc.msg.info("Sync interface")

socket = vlc.net.connect_tcp(host, port)

function read(fd, length)
    local left = length
    local message = ""
    while left ~= 0 do
        local pollfds = {}
        -- vlc.msg.dbg("poll")
        pollfds[fd] = vlc.net.POLLIN
        local ret = vlc.net.poll(pollfds)
        if ret > 0 then
            local s = vlc.net.recv(fd, left)
            if s then
                message = message .. s
                left = left - s:len()
            end
        end
        process_changes()
    end
    return message
end

function write(fd, data)
    vlc.net.send(fd, data)
end

function process_changes()
    local input = vlc.object.input()
    if input then
        local play_status = vlc.playlist.status()
        if status.play_status ~= play_status then
            if play_status == playing then
                handle_play()
            else
                handle_pause()
            end
        else
            local time = vlc.var.get(input, "time")
            if math.abs(time - status.time) > time_eps then
                if vlc.playlist.status() == playing then
                    handle_play()
                else
                    handle_seek()
                end
            end
        end
        update_status()
    end
end

function handle_play()
    vlc.msg.dbg("Sending play message")
    local mstime = get_time_ms()
    write(socket, play_message .. encode_time(mstime))
end

function handle_pause()
    vlc.msg.dbg("Sending pause message")
    write(socket, pause_message)
end

function handle_seek()
    vlc.msg.dbg("Sending seek message")
    local mstime = get_time_ms()
    write(socket, seek_message .. encode_time(mstime))
end

function update_status()
    local input = vlc.object.input()
    if input then
        local play_status = vlc.playlist.status()
        local time = vlc.var.get(input, "time")
        status.play_status = play_status
        status.time = time
    end
end

function get_time_ms()
    local time = vlc.var.get(vlc.object.input(), "time")
    local mstime = math.floor(time * 1000)
    vlc.msg.dbg("time: " .. mstime)
    return mstime
end

time_radix = 256

function encode_time(time)
    local s = ""
    while time ~= 0 do
        local x = time % time_radix
        s = s .. string.char(x)
        time = math.floor(time / time_radix)
    end
    while (s:len() ~= 4) do
        s = s .. string.char(0)
    end
    return s
end

function decode_time(s)
    local time = 0
    for i = s:len(), 1, -1 do
        time = time * time_radix + s:byte(i)
    end
    return time
end

function seek(time)
    local input = vlc.object.input()
    if input then
        vlc.var.set(input, "time", time)
    end
end

--[[
    1 byte - code
    01: play
        4 bytes - position in miliseconds
    02: pause
    03: seek position
        4 bytes - position in miliseconds
    127: ping
]]
while true do
    local message_type = read(socket, message_type_size):byte(1)
    if message_type == play_message_code then
        vlc.msg.dbg("Play command received")
        local mstime = decode_time(read(socket, message_time_size))
        seek(mstime / 1000)
        vlc.playlist.play()
    elseif message_type == pause_message_code then
        vlc.msg.dbg("Pause command received")
        vlc.playlist.pause()
    elseif message_type == seek_message_code then
        vlc.msg.dbg("Seek message received")
        local mstime = decode_time(read(socket, message_time_size))
        seek(mstime / 1000)
    elseif message_type == ping_message_code then
        --vlc.msg.dbg("Ping message received")
        write(socket, ping_message)
    else
        vlc.msg.dbg("Message of unknown type received: " .. message_type)
    end
    update_status()
end
