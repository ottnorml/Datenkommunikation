package edu.hm.dako.chat.audit_log_server;

import edu.hm.dako.chat.common.AuditLogPDU;
import edu.hm.dako.chat.connection.ServerSocketInterface;
import edu.hm.dako.chat.tcp.TcpServerSocket;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;

public class TcpServer extends AbstractServer {
    public TcpServer() {
        super();
    }

    public TcpServer(int serverPort) {
        super(serverPort);
    }

    public TcpServer(int serverPort, AuditLogPduDaoInterface<AuditLogPDU> model) {
        super(serverPort, model);
    }

    public static void main(String[] args) {
        ServerInterface server;
        if (args.length == 0) {
            server = new TcpServer();
        } else {
            String arg = args[0];
            server = new TcpServer(Integer.parseInt(arg));
        }
        server.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void initLog4J() {
        PropertyConfigurator.configureAndWatch("log4j.auditLogServer_tcp.properties");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    synchronized ServerSocketInterface getServerSocket() throws IOException {
        if (serverSocket == null) {
            serverSocket = new TcpServerSocket(
                serverPort,
                AbstractServer.DEFAULT_SENDBUFFER_SIZE,
                AbstractServer.DEFAULT_RECEIVEBUFFER_SIZE
            );
        }
        log.info("Socket wurde auf Port " + serverPort + " initialisiert");

        return serverSocket;
    }
}
