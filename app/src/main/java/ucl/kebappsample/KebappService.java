package ucl.kebappsample;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KebappService extends Service implements
        ConnectionInfoListener,PeerListListener {

    private Channel channel;
    private BroadcastReceiver receiver = null;
    private List peers = new ArrayList();

    int devices = 0;

    public static final String TAG = "KebappService";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_kebapptest";
    public static final String SERVICE_REG_TYPE = "_kebapp._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    //public MyListFragment peersList;

    private final IntentFilter intentFilter = new IntentFilter();
    private TextView statusTxtView;

    private WiFiP2pService service;

    private boolean isRunning  = false;

    private Face mFace;
    private String mAddress = "";
    private KeyChain keyChain;

    @Override
    public void onCreate() {
        Log.i(TAG, "Service onCreate");
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //statusTxtView = (TextView) findViewById(R.id.status_text);
        //super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, (KebappApplication)getApplication());
        registerReceiver(receiver, intentFilter);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");

        //Creating new thread for my service
        //Always write your long running tasks in a separate thread, to avoid ANR
        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.i(TAG, "Service onStartCommand thread");

                intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

                manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                channel = manager.initialize(KebappService.this, getMainLooper(), null);
                startRegistrationAndDiscovery();

            }
        }).start();

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {

        isRunning = false;
        stopSelf();
        disconnect();
        unregisterReceiver(receiver);
        Log.i(TAG, "Service onDestroy");
    }

    public void disconnect() {
        if (manager != null && channel != null) {
            manager.requestGroupInfo(channel, new GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && manager != null && channel != null
                            && group.isGroupOwner()) {
                        manager.removeGroup(channel, new ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }
    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG,"Failed to add a service");
            }
        });

        discoverService();

    }

    private void discoverService() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
                        Log.d(TAG, "onBonjourServiceAvailable "
                                + instanceName + " " + registrationType + " " + srcDevice);
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            devices++;

                            // update the UI and add the item the discovered
                            // device.
                         /*   WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2pService service = new WiFiP2pService();
                                service.device = srcDevice;
                                service.instanceName = instanceName;
                                service.serviceRegistrationType = registrationType;
                                adapter.add(service);
                                adapter.notifyDataSetChanged();*/
                            service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            connectP2p(service);
                            Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);
                            //  }
                        }

                    }
                }, new DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE) + " " + devices);
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(TAG,"Failed adding service discovery request");
                    }
                });
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG,"Service discovery failed");

            }
        });
    }

    public void connectP2p(WiFiP2pService service) {
        Log.d(TAG, "connectP2p ");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 15; // I want this device to become the owner
        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Connecting to service");


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Log.i(TAG, "Start produce service thread");

                            KeyChain keyChain = buildTestKeyChain();
                            mFace = new Face("localhost");
                            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                            Log.i(TAG, "My Address is: " + mAddress);

                            // Register the prefix with the device's address
                            mFace.registerPrefix(new Name("/kebapp/maps"), new OnInterest() {
                                @Override
                                public void onInterest(Name name, Interest interest, Transport transport, long l) {
                                    //      try {
                                    int size = interest.getName().size();
                                    Name requestName = interest.getName();
                                    Log.i(TAG, "Size: " + size);
                                    Log.i(TAG, "Interest Name: " + interest.getName().toUri());
                                    String source = requestName.get(2).toEscapedString();
                                    String dest = requestName.get(3).toEscapedString();
                                    Log.i(TAG, "Interest source: " + source);
                                    Log.i(TAG, "Interest dest: " + dest);

                                    String urlString = new String("http://maps.googleapis.com/maps/api/directions/json?origin="+source+"&destination="+dest);
                                    //Toast.makeText(getApplicationContext(), "Interest Received: " + interest.getName().toUri(), Toast.LENGTH_LONG).show();
                                    try{
                                        JSONObject jsonObject = getJSONObjectFromURL(urlString);
                                        JSONArray routes = jsonObject.getJSONArray("routes");
                                        JSONObject legs = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance");

                                        String content = legs.toString();

                                        Data data = new Data(requestName);
                                        data.setContent(new Blob(content));

                                        mFace.putData(data);
                                        Log.i(TAG, "The device info has been send");
                                        Log.i(TAG, "The content is: " + content);
                                        // Parse your json here

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }, new OnRegisterFailed() {
                                @Override
                                public void onRegisterFailed(Name name) {
                                    Log.e(TAG, "Failed to register the data");
                                }
                            });

                            while(true) {
                               // Log.i(TAG, "Service is running");
                                mFace.processEvents();
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }

                }).start();
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG,"Failed connecting to service");
            }
        });
        Log.d(TAG, "connectedP2p ");

    }


    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        //MyListFragment fragment = (MyListFragment) getFragmentManager()
        //        .findFragmentByTag("services");

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner "+ p2pInfo.groupOwnerAddress.getHostAddress());
          /*  try {
                handler = new GroupOwnerSocketHandler(
                        ((MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }*/
        } else {
            Log.d(TAG, "Connected as peer " + p2pInfo.groupOwnerAddress.getHostAddress());
           /*  handler = new ClientSocketHandler(
                    ((MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();*/
        }
        // chatFragment = new WiFiChatFragment();
        //getFragmentManager().beginTransaction()
        //        .replace(R.id.container_root, chatFragment).commit();
        //peersList.addItems(p2pInfo);
        //statusTxtView.setVisibility(View.GONE);

    }
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {

        // Out with the old, in with the new.
     /* peers.clear();
        peers.addAll(peerList.getDeviceList());

        // If an AdapterView is backed by this data, notify it
        // of the change.  For instance, if you have a ListView of available
        // peers, trigger an update.
        // ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

        peersList.clearItems();
        for(int i= 0;i<peers.size();i++)
        {
            WifiP2pDevice dev = (WifiP2pDevice)peers.get(0);
            Log.d(MainActivity.TAG, "Device "+dev);
            if(dev.status==0)
                peersList.addItems(dev.deviceName+" "+ service.instanceName);

        }
        Log.d(MainActivity.TAG, "Peers "+peers.size());
        if (peers.size() == 0) {
            Log.d(MainActivity.TAG, "No devices found");
            return;
        }
        */
    }

    // AsyncTask for register the routes on NFD
    private class RegisterNFD extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "Register NFD Task";
        private boolean shouldStop = false;
        private Face mFace;
        //private KebappApplication app;
        //private Context context;

        @Override
        protected Void doInBackground(Void... params) {

            try {

                Nfdc ndfc = new Nfdc();
                int faceID = ndfc.faceCreate("udp://127.0.0.1");
                ndfc.ribRegisterPrefix(new Name("/kebapp"), faceID, 10, true, false);
                ndfc.shutdown();

            } catch (SecurityException e) {
                Log.e(RegisterNFD.TAG, "Register Failed");
            } catch(Exception e) {
                Log.e(RegisterNFD.TAG, e.toString());
            }
            return null;
        }
    }


    public static KeyChain buildTestKeyChain() throws net.named_data.jndn.security.SecurityException {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        KeyChain keyChain = new KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (net.named_data.jndn.security.SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;
    }

    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

        //char[] buffer = new char[1024];

        String jsonString;

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        jsonString = sb.toString();
        //Log.d(TAG, "JSON: " + jsonString);

        return new JSONObject(jsonString);
    }
}