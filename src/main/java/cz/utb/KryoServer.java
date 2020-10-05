package cz.utb;

import com.esotericsoftware.kryonet.Server;

import com.esotericsoftware.minlog.Log;
import cz.utb.kryonet.MyServer;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KryoServer {
    Server server;

    SpeedTestSocket speedTestSocket;
    TokenGenerator tokenGenerator = new TokenGenerator();
    java.sql.Connection conn = null;
    SQL sqlHelper;


    public KryoServer() throws IOException {
        sqlHelper = new SQL();
        sqlHelper.connectToDatabase();
        MyServer myServer = new MyServer(sqlHelper);
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
                    String name = report.getSpeedTestMode().name();
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
                    case "debug":
                        Log.set(Log.LEVEL_DEBUG);
                        break;
                    case "info":
                        Log.set(Log.LEVEL_INFO);
                        break;
                    case "download":
                        speedTestSocket.startDownload("ftp://speedtest.tele2.net/5MB.zip");
                        break;
                    case "upload":
                        speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 500000, 1000);
                        break;
                    case "time":
                        long millis = System.currentTimeMillis();
                        System.out.println(String.format("%02d min, %02d sec",
                                TimeUnit.MILLISECONDS.toMinutes(millis) -
                                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                                TimeUnit.MILLISECONDS.toSeconds(millis) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                        ));
                        long a = millis -
                                TimeUnit.DAYS.toMillis(TimeUnit.MILLISECONDS.toDays(millis));
                        System.out.println(a);
                        String sb1 = Long.toString(a);
                        sb1 = sb1.substring(1);
                        float b = Float.valueOf(sb1);
                        System.out.println(b);
                        break;
                }

            }
        }
    }

    public static void main(String[] args) throws IOException {
        Log.set(Log.LEVEL_INFO);
        Log.info("Creating server...");

        new KryoServer();
    }

}
