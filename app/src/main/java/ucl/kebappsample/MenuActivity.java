package ucl.kebappsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MenuActivity extends ActionBarActivity {
    private Button btnFindDevices;
    private Button btnGetPhotos;
    private Button btnSharePhotos;
    public static boolean wifiDirectConnected = false;

    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        //btnGetPhotos = (Button) this.findViewById(R.id.btnGetPhotos);
        btnSharePhotos = (Button) this.findViewById(R.id.btnSharePhotos);
        btnFindDevices = (Button) this.findViewById(R.id.btnFindDevices);
        //intent = new Intent(this, DeviceListActivity.class);
      //  btnGetPhotos.setOnClickListener(new View.OnClickListener() {
      //      @Override
      //      public void onClick(View v) {

                //get visible devices first, needs to be updated
                //do something on NFD !!!!!
                //get and update device list

                //app.addDevice()
              /*String oAddress = ((KebappApplication)getApplication()).getOwnerAddress();
                String mAddress = ((KebappApplication)getApplication()).getMyAddress();
                if(!oAddress.equals(mAddress)) {
                    RequestDeviceListTask task = new RequestDeviceListTask((KebappApplication)getApplication(), getApplicationContext());
                    // String oAddress = ((PhotoSharingApplication) getApplication()).getOwnerAddress();
                    Log.i("Request Deivce List", "Start to request device list from " + oAddress);
                    task.execute(oAddress);
                }
                else {
                    RegisterNFD task = new RegisterNFD((KebappApplication)getApplication());
                    Log.i("Menu Activity", "Register on NFD");
                    ArrayList<DeviceInfo> deviceInfos = ((KebappApplication)getApplication()).getDeviceList();
                    Log.i("Menu Activity", "The device list " + deviceInfos.toString());
                    task.execute(deviceInfos);

                    // Intent intent = new Intent(MenuActivity.this, DeviceListActivity.class);
                    // ArrayList<DeviceInfo> deviceInfos = ((PhotoSharingApplication)getApplication()).getDeviceList();
                    intent.putParcelableArrayListExtra("devices", deviceInfos);

                    startActivity(intent);
                }*/

//                Intent intent = new Intent(MenuActivity.this, DeviceListActivity.class);
//                ArrayList<DeviceInfo> deviceInfos = ((PhotoSharingApplication)getApplication()).getDeviceList();
//                intent.putParcelableArrayListExtra("devices", deviceInfos);
//                //intent.putExtra("devices", devices);
//
//                // Register the routes
//                // ArrayList<String> ips = new ArrayList<String>();
//                // for(DeviceInfo info : deviceInfos) {
//                    // ips.add(info.ipAddress);
//                // }
//                // RegisterNFD rTask = new RegisterNFD();
//                // rTask.execute(ips);
//
//                startActivity(intent);
      //      }
     //   });
        btnSharePhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, InterestActivity.class);
                startActivity(intent);
                //in order to send data
                //intent.putExtra(key,value);
                //in order to get result
                //startActivityForResult(intent, requestcode);
            }
        });
        btnFindDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //send to wifi direct activity
                Intent intent = new Intent(MenuActivity.this, DiscoverActivity.class);
                //startActivityForResult(intent);
                startActivity(intent);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
      //  if (id == R.id.action_settings) {
      //      return true;
      //  }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //wifiDirectConnected = true; // for test purpose, enable the two buttons on main menu page
        if (wifiDirectConnected){
//            btnGetPhotos.setEnabled(true);
//            btnGetPhotos.setBackground(getResources().getDrawable(R.drawable.btnviewothersenable));
            btnSharePhotos.setEnabled(true);
           // btnSharePhotos.setBackground(getResources().getDrawable(R.drawable.btnsharemyphotosenable));
        }
        else{
            Toast.makeText(getApplicationContext(), "To get started, you may have to connect to other devices first", Toast.LENGTH_LONG).show();
//            btnGetPhotos.setEnabled(false);
//            btnGetPhotos.setBackground(getResources().getDrawable(R.drawable.btnviewothersdisable));
            btnSharePhotos.setEnabled(false);
         //   btnSharePhotos.setBackground(getResources().getDrawable(R.drawable.btnsharemyphotosdisable));
        }
    }


}
