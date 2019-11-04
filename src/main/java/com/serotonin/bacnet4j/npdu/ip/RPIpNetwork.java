package com.serotonin.bacnet4j.npdu.ip;

import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class RPIpNetwork extends IpNetwork {
    static final Logger LOG = LoggerFactory.getLogger(RPIpNetwork.class);

    private final int port;
    private final String localBindAddressStr;
    private final String broadcastAddressStr;
    private final String subnetMaskStr;
    private final boolean reuseAddress;

    // Runtime
    private Thread thread;
    private DatagramSocket socket;
    private OctetString broadcastMAC;
    private InetSocketAddress localBindAddress;
    private byte[] subnetMask;
    private long bytesOut;
    private long bytesIn;
    /**
     * Use an IpNetworkBuilder to create instances.
     *
     * @param port
     * @param localBindAddress
     * @param broadcastAddress
     * @param subnetMask
     * @param localNetworkNumber
     * @param reuseAddress
     */
    RPIpNetwork(int port, String localBindAddress, String broadcastAddress, String subnetMask, int localNetworkNumber, boolean reuseAddress) {
        super(port, localBindAddress, broadcastAddress, subnetMask, localNetworkNumber, reuseAddress);
        this.port = port;
        this.localBindAddressStr = localBindAddress;
        this.broadcastAddressStr = broadcastAddress;
        this.subnetMaskStr = subnetMask;
        this.reuseAddress = reuseAddress;
    }

    @Override
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);
/*
        localBindAddress = InetAddrCache.get(localBindAddressStr, port);

        if (reuseAddress) {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            if (!socket.getReuseAddress())
                LOG.warn("reuseAddress was set, but not supported by the underlying platform");
            socket.bind(localBindAddress);
        } else {
            socket = new DatagramSocket(localBindAddress);
        }
        socket.setBroadcast(true);

        //        broadcastAddress = new Address(broadcastIp, port, new Network(0xffff, new byte[0]));
        broadcastMAC = IpNetworkUtils.toOctetString(broadcastAddressStr, port);
        subnetMask = BACnetUtils.dottedStringToBytes(subnetMaskStr);

        thread = new Thread(this, "BACnet4J IP socket listener for " + transport.getLocalDevice().getId());
        thread.start();
 */
    }

    //
    // For receiving
    @Override
    public void run() {
        final byte[] buffer = new byte[MESSAGE_LENGTH];
        final DatagramPacket p = new DatagramPacket(buffer, buffer.length);

        socket = getSocket();
        while (!socket.isClosed()) {
            try {
                socket.receive(p);

                bytesIn += p.getLength();
                // Create a new byte queue for the message, because the queue will probably be processed in the
                // transport thread.
                final ByteQueue queue = new ByteQueue(p.getData(), 0, p.getLength());
                final OctetString link = IpNetworkUtils.toOctetString(p.getAddress().getAddress(), p.getPort());

                //FIXME write ByteQueue or DatagramPacket to file.
                handleIncomingData(queue, link);

                // Reset the packet.
                p.setData(buffer);
            } catch (@SuppressWarnings("unused") final IOException e) {
                // no op. This happens if the socket gets closed by the destroy method.
            }
        }
    }
}
