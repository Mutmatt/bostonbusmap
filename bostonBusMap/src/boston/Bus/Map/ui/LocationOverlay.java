package boston.Bus.Map.ui;

import android.content.Context;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.Toast;
import boston.Bus.Map.R;
import boston.Bus.Map.main.UpdateHandler;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class LocationOverlay extends MyLocationOverlay {
	private final Context context; 
	private UpdateHandler handler;
	private Runnable runnable;
	private final MapView mapView;
	
	public LocationOverlay(Context context, MapView mapView) {
		super(context, mapView);
		
		this.context = context;
		this.mapView = mapView;
		this.runnable = new Runnable() {
			
			@Override
			public void run() {
				//do nothing; later on this will be replaced with a useful Runnable
			}
		};
	}
	
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		switch (arg1)
		{
		case LocationProvider.AVAILABLE:
			break;
		case LocationProvider.OUT_OF_SERVICE:
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Toast.makeText(context, context.getString(R.string.locationUnavailable), Toast.LENGTH_LONG).show();
			break;
		}
	}

	public void setUpdateable(UpdateHandler handler) {
		this.handler = handler;
		
		runnable = new Runnable() {
			
			@Override
			public void run() {
				
    			mapView.getController().animateTo(getMyLocation());
    			
    			LocationOverlay.this.handler.triggerUpdate(1500);
			}
		};
		
	}

	public void updateMapViewPosition() {
		runOnFirstFix(runnable);
	}


	
}