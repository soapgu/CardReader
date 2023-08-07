package com.shgbit.readcard;

import android.serialport.SerialPort;
import android.util.Pair;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;

public class CardReader {
    private SerialPort transport;
    private InputStream inputStream;
    //private OutputStream outputStream;

    private static final String BEGIN = "7f0910000400";
    private boolean opened;

    public synchronized void open()  throws IOException {
        if( !opened ) {
            transport = new SerialPort(new File("/dev/ttyS3"), 9600);
            inputStream = transport.getInputStream();
            //outputStream = transport.getOutputStream();
            opened = true;
            Logger.i("-----Serial port Opened----");
        }
    }

    public Observable<String> read(){
        return Observable.interval( 200, TimeUnit.MILLISECONDS)
                .flatMapMaybe( t-> Maybe.<Pair<byte[],Integer>>create(emitter -> {
                    int size;
                    try {
                        byte[] buffer = new byte[64];
                        size = inputStream.read(buffer);
                        if (size > 0 ) {
                            //onDataReceived(buffer, size);
                            emitter.onSuccess( Pair.create( buffer,size ) );
                        } else {
                            emitter.onComplete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        emitter.onError(e);
                    }
                } ))
                .flatMapMaybe(  t-> onDataReceived(t.first,t.second))
                .distinctUntilChanged()
                .timeout(10,TimeUnit.SECONDS)
                .onErrorComplete( throwable -> throwable instanceof TimeoutException)
                .repeat();
    }

    public boolean isOpened() {
        return opened;
    }

    private Maybe<String> onDataReceived(byte[] buffer, final int size) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (buffer == null || buffer.length == 0) {
            return Maybe.empty();
        }
        for (byte b : buffer) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        String msg = stringBuilder.toString();
        if (msg.startsWith(BEGIN)) {
            String cardNum = msg.substring(12, 20);
            String right_msg = cardNum.substring(6, 8) + cardNum.substring(4, 6) + cardNum.substring(2, 4) + cardNum.substring(0, 2);
            return Maybe.just(right_msg);
        }
        return Maybe.empty();
    }
    

}
