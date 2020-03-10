package cz.utb;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cz.utb.kryonet.MyServer;
import cz.utb.kryonet.Network;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

public class KryoServer {
    Server server;

    SpeedTestSocket speedTestSocket;
    TokenGenerator tokenGenerator = new TokenGenerator();

    public KryoServer() throws IOException {
        MyServer myServer = new MyServer();
        server = myServer.server;
        new Console();
    }

    public class Console {
        public Console() {
            speedTestSocket = new SpeedTestSocket();
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(SpeedTestReport report) {
                    // called when download/upload is complete
                    BigDecimal divisor = new BigDecimal("1000000");
                    System.out.print("[completed] "
                            + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                            report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \n");
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(float percent, SpeedTestReport report) {
                    // called to notify download/upload progress
                    BigDecimal divisor = new BigDecimal("1000000");
                    StringBuilder sb = new StringBuilder("[");
                    int round = (int) percent;
                    if (round < 10) {
                        sb.append("..........");
                    } else {
                        int firstDigit = Integer.parseInt(Integer.toString(round).substring(0, 1));
                        for (int i = 0; i < firstDigit - 1; i++) {
                            sb.append("=");
                        }
                        sb.append(">");
                        for (int i = 0; i < 10 - firstDigit; i++) {
                            sb.append(".");
                        }
                    }
                    sb.append("] "
                            + report.getTransferRateOctet().divide(divisor).round(new MathContext(3)) + " MB/s  " +
                            report.getTransferRateBit().divide(divisor).round(new MathContext(3)) + " mbps \r");
                    System.out.print(sb.toString());
                }
            });
            start();
        }

        public void start() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String ipnut = scanner.next().trim();
                switch (ipnut) {
                    case "download":
                        speedTestSocket.startDownload("ftp://speedtest.tele2.net/5MB.zip");
                        break;
                    case "upload":
                        speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 1000000, 1000);
                        break;
                }

            }
        }
    }

    public static void main(String[] args) throws IOException {
        Log.set(Log.LEVEL_INFO);
        System.out.println("Creating server...");

        new KryoServer();
    }
}
