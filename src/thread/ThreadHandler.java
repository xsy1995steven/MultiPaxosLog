package thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class ThreadHandler implements Runnable {

    protected Socket socket;
    protected BufferedReader bufferedReader;
    protected PrintWriter printWriter;

    public ThreadHandler(Socket socket) {
        try {
            this.socket = socket;
            this.printWriter = new PrintWriter(this.socket.getOutputStream(), true);
            this.bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Fail to create thread handler for socket "
                    + socket.getInetAddress().getHostAddress() + ':' + socket.getPort());
        }
    }

}
