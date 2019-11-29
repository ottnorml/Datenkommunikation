package edu.hm.dako.chat.AuditLogServer;

import edu.hm.dako.chat.common.AuditLogPDU;
import edu.hm.dako.chat.common.AuditLogPduType;
import edu.hm.dako.chat.connection.Connection;
import edu.hm.dako.chat.tcp.TcpServerSocket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;


/**
 * AuditLog Server fuer die Protokollierung von Chat-Nachrichten eines Chat-Servers.
 * Implementierung auf Basis von TCP.
 *
 * @author mandl
 */
public class AuditLogTcpServer extends Application implements AuditLogServerInterface {
    static final String auditLogFile = "ChatAuditLog.dat";
    static final int AUDIT_LOG_SERVER_PORT = 40001;
    static final int DEFAULT_SENDBUFFER_SIZE = 30000;
    static final int DEFAULT_RECEIVEBUFFER_SIZE = 800000;
    private static Logger log = Logger.getLogger(AuditLogTcpServer.class);
    protected long counter = 0;
    private Stage stage;
    private AuditLogGUIController lc;
    private AuditLogModel model = new AuditLogModel();
    private Connection socket;
    private boolean finished = true;
    private TcpServerSocket tcpServerSocket;

    @Override
    public void start(Stage primaryStage) throws Exception {
        PropertyConfigurator.configureAndWatch("log4j.auditLogServer_tcp.properties", 60 * 1000);
        System.out.println("AuditLog-TcpServer gestartet, Port: " + AUDIT_LOG_SERVER_PORT);
        log.info("AuditLog-TcpServer gestartet, Port: " + AUDIT_LOG_SERVER_PORT);
        getModel().serverAddress = "127.0.0.1";

        FXMLLoader loader = new FXMLLoader(AuditLogGUIController.class.getResource("AuditLogGUI.fxml"));
        Parent root = loader.load();
        lc = loader.getController();
        lc.setAppController(this);
        primaryStage.setTitle("AuditLogServerGUI (TCP)");
        root.setStyle("-fx-background-color: cornsilk");
        primaryStage.setOpacity(0.8);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 800, 400));
        stage = primaryStage;
        primaryStage.show();

        getModel().messages.add("AuditLogServer für TCP Kommunikation gestartet. Nachrichten werden geloggt:");

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                tcpServerSocket =
                    new TcpServerSocket(AUDIT_LOG_SERVER_PORT, DEFAULT_SENDBUFFER_SIZE, DEFAULT_RECEIVEBUFFER_SIZE);
                while (!tcpServerSocket.isClosed()) {
                    try {

                        socket = tcpServerSocket.accept();

                        while (!Thread.currentThread().isInterrupted() && finished) {
                            try {
                                AuditLogPDU auditLogPDU = (AuditLogPDU) socket.receive();
                                counter++;
                                System.out.println(auditLogPDU);
                                setMessageLine(auditLogPDU.getUserName(), auditLogPDU.getPduType(),
                                    auditLogPDU.getMessage(), auditLogPDU.getAuditTime());
                            } catch (IOException e) {
                                finished = true;
                            }
                        }
                    } catch (Exception e) {
                        setErrorMessage("AuditLogServer (TCP)",
                            "Beim Empfangen eines PDUs ist ein Fehler aufgetreten.",
                            9);
                        e.printStackTrace();
                    }

                }
                socket.close();
                tcpServerSocket.close();
                return null;
            }
        };

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @Override
    public void stop() {
        try {
            stage.hide();
            String message = "AuditLogServerGUI (TCP) beendet, Gesendete AuditLog-Saetze: " + counter + "\n";
            System.out.println(message);
            writeDataToLogFile(message);

        } catch (Exception e) {
            System.out.println("Fehler beim Schliessen der AuditLogServerGUI (TCP)");
        }
    }

    @Override
    public void setErrorMessage(String sender, String errorMessage, long errorCode) {
        log.debug("errorMessage: " + errorMessage);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Es ist ein Fehler im " + sender + " aufgetreten");
                alert.setHeaderText("Fehlerbehandlung (Fehlercode = " + errorCode + ")");
                alert.setContentText(errorMessage);
                alert.showAndWait();
            }
        });
    }

    @Override
    public void setMessageLine(String user, AuditLogPduType type, String message, Long auditLogTime) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String data = "[TIME] " + new Timestamp(auditLogTime)
                    + " | [USER] " + user
                    + " | [TYPE] " + type
                    + " | [MESSAGE] " + message + "\n";
                getModel().messages.add(data);
                writeDataToLogFile(data);
            }
        });
    }

    @Override
    public void writeDataToLogFile(String data) {
        String path = System.getProperty("user.dir");
        String fileName = auditLogFile;
        String dirName = "/out/TCP-LogFiles/";
        File file = new File(path + dirName + "/" + fileName);
        File dir = new File(path + dirName);

        if (!file.exists()) {
            try {
                if (!dir.exists()) {
                    dir.mkdir();
                }
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                setErrorMessage("AuditLogServer (TCP)",
                    "Ordner ./out/TCP-LogFiles/" + auditLogFile + "konnte nicht angelegt werden.", 99);
            }
        }

        try {
            Files.write(Paths.get(System.getProperty("user.dir") + "/out/TCP-LogFiles/"
                + auditLogFile), data.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            setErrorMessage("AuditLogServer (TCP)",
                "Ein Fehler beim Schreiben in das Log-File ist aufgetreten.", 98);
        }
    }

    public AuditLogModel getModel() {
        return model;
    }
}
