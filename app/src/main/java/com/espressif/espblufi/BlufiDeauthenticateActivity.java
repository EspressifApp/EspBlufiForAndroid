package com.espressif.espblufi;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.espressif.espblufi.communication.BlufiCommunicator;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiDeauthenticateActivity extends BlufiAbsActivity {
    private static final int MENU_ID_DEAUTHENTICATE = 1;

    private BlufiCommunicator mCommunicator;

    private ProgressBar mProgressBar;

    private List<Connection> mConnectionList;
    private RecyclerView mRecyclerView;

    private MenuItem mMenuItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_deauthenticate_activity);

        mCommunicator = BlufiBridge.sCommunicator;

        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        mConnectionList = new ArrayList<>();
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(new Adapter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCommunicator = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuItem = menu.add(Menu.NONE, MENU_ID_DEAUTHENTICATE, 0, R.string.deauthenticate_menu_item);
        mMenuItem.setIcon(android.R.drawable.ic_menu_delete);
        mMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_DEAUTHENTICATE:
                deauthenticate();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deauthenticate() {
        List<String> bssidList = new ArrayList<>();
        for (Connection conn: mConnectionList) {
            if (conn.checked) {
                bssidList.add(conn.BSSID);
            }
        }

        if (bssidList.isEmpty()) {
            return;
        }

        showProgress(true);
        mMenuItem.setEnabled(false);

        Intent data = new Intent();
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    mCommunicator.deauthenticate(bssidList);
                    subscriber.onNext(true);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        String[] bssidArray = new String[bssidList.size()];
                        for (int i = 0; i < bssidArray.length; i++) {
                            bssidArray[i] = bssidList.get(i);
                        }
                        data.putExtra(BlufiBridge.KEY_DEAUTHENTICATE_DATA, bssidArray);
                        setResult(RESULT_OK, data);
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        setResult(RESULT_EXCEPTION);
                        finish();
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                    }
                });

    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mProgressBar.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasCheckedConnection() {
        for (Connection conn : mConnectionList) {
            if (conn.checked) {
                return true;
            }
        }

        return false;
    }

    private class Connection {
        String SSID;
        String BSSID;
        boolean checked;
    }

    private class Holder extends RecyclerView.ViewHolder {
        Connection connection;

        TextView text1;
        TextView text2;
        CheckBox checkBox;

        Holder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            checkBox = (CheckBox) itemView.findViewById(R.id.check);
            checkBox.setOnClickListener(v -> {
                connection.checked = checkBox.isChecked();
                if (connection.checked) {
                    mMenuItem.setEnabled(true);
                } else {
                    mMenuItem.setEnabled(hasCheckedConnection());
                }
            });
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        LayoutInflater mInflater;

        Adapter() {
            mInflater = getLayoutInflater();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.deauthenticate_connection_item, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.connection = mConnectionList.get(position);

            holder.text1.setText(holder.connection.SSID);
            holder.text2.setText(holder.connection.BSSID);
            holder.checkBox.setChecked(holder.connection.checked);
        }

        @Override
        public int getItemCount() {
            return mConnectionList.size();
        }
    }
}
