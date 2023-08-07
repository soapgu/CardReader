package com.shgbit.readcard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {
    private TextView msg;
    private final CompositeDisposable disposables = new CompositeDisposable();
    CardReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reader = new CardReader();
        setContentView(R.layout.activity_main);
        this.msg = findViewById(R.id.tv_msg);
        findViewById(R.id.btn_open).setOnClickListener( v -> {
            try {
                reader.open();
            }catch (Exception exception){
                Logger.e( "open error",exception );
            }
            if( reader.isOpened() ){
                disposables.add( reader.read()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe( card-> {
                            Logger.i("read new card:%s",card);
                            this.msg.setText( card );
                        },
                                throwable -> Logger.e("read error",throwable)
                        , ()-> Logger.i("read complete")));
            }

        } );
    }


}