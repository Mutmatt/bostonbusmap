/*
    BostonBusMap
 
    Copyright (C) 2009  George Schneeloch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */
package boston.Bus.Map;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

/**
 * This gets the location and sets the mapView to it, then unregisters itself so that this only gets executed once
 * Remember that you need to declare this in the permissions part of AndroidManifest.xml
 *
 */
public class OneTimeLocationListener implements LocationListener {

	private final MapView mapView;
	
	private double latitude;
	private double longitude;

	private final LocationManager locationManager;
	
	public double getLatitude()
	{
		return latitude;
	}
	
	public double getLongitude()
	{
		return longitude;
	}
	
	public OneTimeLocationListener(MapView mapView, LocationManager locationManager)
	{
		this.mapView = mapView;
		this.locationManager = locationManager;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		
		final int e6 = 1000000;
		
		int latAsInt = (int)(latitude * e6);
		int lonAsInt = (int)(longitude * e6);
		
		
		mapView.getController().animateTo(new GeoPoint(latAsInt, lonAsInt));
		
		locationManager.removeUpdates(this);
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}