#!/usr/bin/env python3
"""Команда серверу Minecraft по RCON: rcon.py <server.properties> <команда...>. Только для localhost."""
import socket
import struct
import sys


def props(path):
    out = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            if "=" in line and not line.startswith("#"):
                k, v = line.rstrip("\n").split("=", 1)
                out[k] = v
    return out


def packet(req_id, kind, body):
    data = struct.pack("<ii", req_id, kind) + body.encode("utf-8") + b"\0\0"
    return struct.pack("<i", len(data)) + data


def read(sock):
    head = b""
    while len(head) < 4:
        chunk = sock.recv(4 - len(head))
        if not chunk:
            raise ConnectionError("сервер закрыл соединение")
        head += chunk
    (length,) = struct.unpack("<i", head)
    data = b""
    while len(data) < length:
        data += sock.recv(length - len(data))
    req_id, kind = struct.unpack("<ii", data[:8])
    return req_id, data[8:-2].decode("utf-8", "replace")


def main():
    if len(sys.argv) < 3:
        sys.exit("использование: rcon.py <server.properties> <команда>")
    p = props(sys.argv[1])
    command = " ".join(sys.argv[2:])
    with socket.create_connection(("127.0.0.1", int(p.get("rcon.port", "25575"))), timeout=10) as s:
        s.sendall(packet(1, 3, p["rcon.password"]))
        if read(s)[0] == -1:
            sys.exit("rcon: неверный пароль")
        s.sendall(packet(2, 2, command))
        print(read(s)[1])


if __name__ == "__main__":
    main()
