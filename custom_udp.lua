myprotocol = Proto("custom_udp", "Custom UDP Protocol")

local message_types = {
    [0] = "SYN",
    [1] = "SYN-ACK",
    [2] = "ACK",
    [3] = "DATA",
    [4] = "KEEP-ALIVE",
    [5] = "FIN",
    [6] = "FIN-ACK",
    [7] = "MSG",
    [8] = "DATA"
}

local message_type = ProtoField.uint32("custom_udp.message_type", "Message Type", base.DEC, message_types)
local sequence_number = ProtoField.uint32("custom_udp.sequence_number", "Sequence Number", base.DEC)
local payload_length = ProtoField.uint32("custom_udp.payload_length", "Payload Length", base.DEC)
local checksum = ProtoField.uint64("custom_udp.checksum", "Checksum", base.DEC)
local payload = ProtoField.string("custom_udp.payload", "Payload")

local data_header_id = ProtoField.uint64("custom_udp.data_header_id", "Data Header ID", base.DEC)
local data_header_total_packages = ProtoField.uint32("custom_udp.data_header_total_packages", "Data Header Total Packages", base.DEC)
local data_header_current_package_number = ProtoField.uint32("custom_udp.data_header_current_package_number", "Data Header Current Package Number", base.DEC)

myprotocol.fields = { message_type, sequence_number, payload_length, checksum, data_header_id, data_header_total_packages, data_header_current_package_number, payload}

function myprotocol.dissector(buffer, pinfo, tree)

    if buffer:len() < 20 then return end

    pinfo.cols.protocol = "Custom UDP"

    local subtree = tree:add(myprotocol, buffer(), "Custom UDP Protocol Data")

    subtree:add(message_type, buffer(0, 4))
    subtree:add(sequence_number, buffer(4, 4))
    subtree:add(payload_length, buffer(8, 4))
    subtree:add(checksum, buffer(12, 8))

    local type = buffer(0, 4):uint()

    set_color_filter_slot(5, "custom_udp.message_type == 7 or custom_udp.message_type == 8")

    if type == 7 or type == 8 then
        subtree:add(data_header_id, buffer(20, 8))
        subtree:add(data_header_total_packages, buffer(28, 4))
        subtree:add(data_header_current_package_number, buffer(32, 4))
        subtree:add(payload, buffer(36))
    else
        subtree:add(payload, buffer(20))
    end

end

local custom_udp_port = DissectorTable.get("udp.port")
custom_udp_port:add(8080, myprotocol)