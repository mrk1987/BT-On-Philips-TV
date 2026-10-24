import struct
import sys

path = sys.argv[1]
data = open(path, "rb").read()
if data[:8] != b"btsnoop\x00":
    sys.exit("not a btsnoop file")

io_names = {0: "DisplayOnly", 1: "DisplayYesNo", 2: "KeyboardOnly", 3: "NoInputNoOutput"}

off = 16
count = 0
while off + 24 <= len(data):
    orig, incl, flags, drops, ts = struct.unpack(">IIIIQ", data[off:off + 24])
    off += 24
    pkt = data[off:off + incl]
    off += incl
    if not pkt:
        continue
    ptype = pkt[0]
    if ptype == 0x04 and len(pkt) >= 3:
        ev = pkt[1]
        plen = pkt[2]
        params = pkt[3:3 + plen]
        if ev == 0x32 and len(params) >= 9:
            count += 1
            print("IO Capability Response: peer_io_cap=%d (%s) oob=%d auth=0x%02x bdaddr=%s" % (
                params[6], io_names.get(params[6], "?"), params[7], params[8],
                ":".join("%02x" % b for b in reversed(params[:6]))))
        elif ev == 0x34:
            count += 1
            print("User Passkey Request: bdaddr=%s" % ":".join("%02x" % b for b in params[:6]))
        elif ev == 0x33:
            count += 1
            print("User Confirmation Request: %s" % params.hex())
        elif ev == 0x3B:
            count += 1
            print("User Passkey Notification: %s" % params.hex())
        elif ev == 0x36:
            count += 1
            print("Simple Pairing Complete: status=0x%02x bdaddr=%s" % (
                params[0] if params else -1,
                ":".join("%02x" % b for b in params[1:7])))
    elif ptype == 0x01 and len(pkt) >= 4:
        opcode = struct.unpack("<H", pkt[1:3])[0]
        plen = pkt[3]
        params = pkt[4:4 + plen]
        if opcode == 0x042B and len(params) >= 9:
            count += 1
            print("IO Capability Request Reply: local_io_cap=%d (%s) oob=%d auth=0x%02x" % (
                params[6], io_names.get(params[6], "?"), params[7], params[8]))
        elif opcode in (0x042C, 0x042D):
            count += 1
            print("User Confirmation Request %sReply: %s" % (
                "Negative " if opcode == 0x042D else "", params.hex()))
        elif opcode == 0x042E:
            count += 1
            print("User Passkey Request Reply: %s" % params.hex())

print("--- %d interesting packets in %d bytes" % (count, len(data)))
