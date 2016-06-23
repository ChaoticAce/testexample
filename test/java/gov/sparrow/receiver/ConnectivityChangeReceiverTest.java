package gov.sparrow.receiver;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowContentResolver;

import static gov.sparrow.contracts.SparrowContract.SPARROW_CONTENT_AUTHORITY;
import static gov.sparrow.receiver.ConnectivityChangeReceiver.ACCOUNT;
import static gov.sparrow.receiver.ConnectivityChangeReceiver.ACCOUNT_TYPE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ConnectivityChangeReceiverTest {

    @Mock ConnectivityManager connectivityManager;
    @Mock Context context;
    @Mock NetworkInfo networkInfo;
    @Mock Intent intent;
    private Account account;
    private ConnectivityChangeReceiver subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        subject = new ConnectivityChangeReceiver();

        when(intent.getAction()).thenReturn("android.net.conn.CONNECTIVITY_CHANGE");

        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);

        when(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)).thenReturn(networkInfo);

        account = new Account(ACCOUNT, ACCOUNT_TYPE);
    }

    @Test
    public void onReceiveConnection_handlesNullNetworkInfo() {
        try {
            when(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET)).thenReturn(null);
            subject.onReceive(context, intent);
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void onReceiveConnectionTypeEthernet_andDisconnected_doNotPerformSync() throws Exception {
        when(networkInfo.getState()).thenReturn(NetworkInfo.State.DISCONNECTED);

        subject.onReceive(context, intent);

        assertThat(ShadowContentResolver.isSyncActive(account, SPARROW_CONTENT_AUTHORITY)).isFalse();
    }

    @Test
    public void onReceiveConnectionTypeEthernet_andConnected_performSync() throws Exception {
        when(networkInfo.getState()).thenReturn(NetworkInfo.State.CONNECTED);

        Bundle expectedBundle =  new Bundle();
        expectedBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        expectedBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        subject.onReceive(context, intent);

        assertThat(ShadowContentResolver.isSyncActive(account, SPARROW_CONTENT_AUTHORITY)).isTrue();

        Bundle syncExtras = ShadowContentResolver.getStatus(account, SPARROW_CONTENT_AUTHORITY).syncExtras;
        assertThat(syncExtras.getBoolean((ContentResolver.SYNC_EXTRAS_EXPEDITED))).isTrue();
        assertThat(syncExtras.getBoolean((ContentResolver.SYNC_EXTRAS_MANUAL))).isTrue();

        onReceiveConnectionTypeEthernet_andConnected_performSyncOnlyOnce();

        onReceiveConnectionTypeEthernet_performSyncAfterReconnecting();
    }

    private void onReceiveConnectionTypeEthernet_andConnected_performSyncOnlyOnce() throws Exception {
        ShadowContentResolver.cancelSync(account, SPARROW_CONTENT_AUTHORITY);
        assertThat(ShadowContentResolver.isSyncActive(account, SPARROW_CONTENT_AUTHORITY)).isFalse();

        subject.onReceive(context, intent);

        assertThat(ShadowContentResolver.isSyncActive(account, SPARROW_CONTENT_AUTHORITY)).isFalse();
    }

    private void onReceiveConnectionTypeEthernet_performSyncAfterReconnecting() throws Exception {
        when(networkInfo.getState()).thenReturn(NetworkInfo.State.DISCONNECTED);
        subject.onReceive(context, intent);

        when(networkInfo.getState()).thenReturn(NetworkInfo.State.CONNECTED);
        subject.onReceive(context, intent);

        assertThat(ShadowContentResolver.isSyncActive(account, SPARROW_CONTENT_AUTHORITY)).isTrue();
    }

}