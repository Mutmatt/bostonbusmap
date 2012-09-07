package boston.Bus.Map.provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import boston.Bus.Map.data.MyHashMap;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.main.UpdateAsyncTask;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class DatabaseContentProvider extends ContentProvider {
	private static final String AUTHORITY = "com.bostonbusmap.databaseprovider";

	private static final String FAVORITES_TYPE = "vnd.android.cursor.dir/vnd.bostonbusmap.favorite";
	private static final Uri FAVORITES_URI = Uri.parse("content://" + AUTHORITY + "/favorites");
	private static final UriMatcher uriMatcher;

	private static final int FAVORITES = 1;

	private final static String dbName = "bostonBusMap";

	private final static String verboseRoutes = "routes";
	private final static String verboseStops = "stops";
	private final static String stopsRoutesMap = "stopmapping";
	private final static String stopsRoutesMapIndexTag = "IDX_stopmapping";
	private final static String stopsRoutesMapIndexRoute = "IDX_routemapping";
	private final static String directionsStopsMapIndexStop = "IDX_directionsstop_stop";
	private final static String directionsStopsMapIndexDirTag = "IDX_directionsstop_dirtag";


	private final static String subwaySpecificTable = "subway";


	private final static String directionsTable = "directions";
	private final static String directionsStopsTable = "directionsStops";
	private final static String stopsTable = "stops";
	private final static String routesTable = "routes";
	private final static String pathsTable = "paths";
	private final static String blobsTable = "blobs";
	private final static String oldFavoritesTable = "favs";
	private final static String newFavoritesTable = "favs2";

	private final static String verboseFavorites = "favorites";

	private final static String routeKey = "route";
	private final static String routeTitleKey = "routetitle";
	private final static String newFavoritesTagKey = "tag";
	private final static String latitudeKey = "lat";
	private final static String longitudeKey = "lon";

	private final static String colorKey = "color";
	private final static String oppositeColorKey = "oppositecolor";
	private final static String pathsBlobKey = "pathblob";
	private final static String stopTagKey = "tag";
	private final static String branchKey = "branch";
	private final static String stopTitleKey = "title";
	private final static String platformOrderKey = "platformorder";


	private static final String dirTagKey = "dirTag";
	private static final String dirNameKey = "dirNameKey";
	private static final String dirTitleKey = "dirTitleKey";
	private static final String dirRouteKey = "dirRouteKey";
	private static final String dirUseAsUIKey = "useAsUI";

	/**
	 * The first version where we serialize as bytes, not necessarily the first db version
	 */
	public final static int FIRST_DB_VERSION = 5;
	public final static int ADDED_FAVORITE_DB_VERSION = 6;
	public final static int NEW_ROUTES_DB_VERSION = 7;	
	public final static int ROUTE_POOL_DB_VERSION = 8;
	public final static int STOP_LOCATIONS_STORE_ROUTE_STRINGS = 9;
	public final static int STOP_LOCATIONS_ADD_DIRECTIONS = 10;
	public final static int SUBWAY_VERSION = 11;
	public final static int ADDED_PLATFORM_ORDER = 12;
	public final static int VERBOSE_DB = 13;
	public final static int VERBOSE_DB_2 = 14;
	public final static int VERBOSE_DB_3 = 15;
	public final static int VERBOSE_DB_4 = 16;
	public final static int VERBOSE_DB_5 = 17;
	public final static int VERBOSE_DB_6 = 18;
	public final static int VERBOSE_DB_7 = 19;

	public final static int VERBOSE_DB_8 = 20;
	public final static int VERBOSE_DB_9 = 21;
	public final static int VERBOSE_DB_10 = 22;
	public final static int VERBOSE_DB_11 = 23;

	public final static int VERBOSE_DBV2_1 = 24;
	public final static int VERBOSE_DBV2_2 = 26;
	public final static int VERBOSE_DBV2_3 = 27;
	public final static int VERBOSE_DBV2_4 = 28;

	public final static int WITH_STOPS_FOR_DIR = 36;

	public final static int CURRENT_DB_VERSION = WITH_STOPS_FOR_DIR;

	public static final int ALWAYS_POPULATE = 3;
	public static final int POPULATE_IF_UPGRADE = 2;
	public static final int MAYBE = 1;

	public static final int INT_TRUE = 1;
	public static final int INT_FALSE = 0;

	/**
	 * Handles the database which stores route information
	 * 
	 * @author schneg
	 *
	 */
	public static class DatabaseHelper extends SQLiteOpenHelper
	{


		public DatabaseHelper(Context context) {
			super(context, dbName, null, CURRENT_DB_VERSION);

		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			/*db.execSQL("CREATE TABLE IF NOT EXISTS " + blobsTable + " (" + routeKey + " STRING PRIMARY KEY, " + blobKey + " BLOB)");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + routePoolTable + " (" + routeKey + " STRING PRIMARY KEY)");*/
			db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseFavorites + " (" + stopTagKey + " STRING PRIMARY KEY)");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + directionsTable + " (" + dirTagKey + " STRING PRIMARY KEY, " + 
					dirNameKey + " STRING, " + dirTitleKey + " STRING, " + dirRouteKey + " STRING, " + 
					dirUseAsUIKey + " INTEGER)");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseRoutes + " (" + routeKey + " STRING PRIMARY KEY, " + colorKey + 
					" INTEGER, " + oppositeColorKey + " INTEGER, " + pathsBlobKey + " BLOB, " + routeTitleKey + " STRING)");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseStops + " (" + stopTagKey + " STRING PRIMARY KEY, " + 
					latitudeKey + " FLOAT, " + longitudeKey + " FLOAT, " + stopTitleKey + " STRING)");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + stopsRoutesMap + " (" + routeKey + " STRING, " + stopTagKey + " STRING, " +
					dirTagKey + " STRING, PRIMARY KEY (" + routeKey + ", " + stopTagKey + "))");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + subwaySpecificTable + " (" + stopTagKey + " STRING PRIMARY KEY, " +
					platformOrderKey + " INTEGER, " + 
					branchKey + " STRING)");

			db.execSQL("CREATE TABLE IF NOT EXISTS " + directionsStopsTable +
					"(" + dirTagKey + " STRING, " + stopTagKey + " STRING)");

			db.execSQL("CREATE INDEX IF NOT EXISTS " + stopsRoutesMapIndexRoute + " ON " + stopsRoutesMap + " (" + routeKey + ")");
			db.execSQL("CREATE INDEX IF NOT EXISTS " + stopsRoutesMapIndexTag + " ON " + stopsRoutesMap + " (" + stopTagKey + ")");
			db.execSQL("CREATE INDEX IF NOT EXISTS " + directionsStopsMapIndexStop + " ON " + directionsStopsTable + " (" + stopTagKey + ")");
			db.execSQL("CREATE INDEX IF NOT EXISTS " + directionsStopsMapIndexDirTag + " ON " + directionsStopsTable + " (" + dirTagKey + ")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.v("BostonBusMap", "upgrading database from " + oldVersion + " to " + newVersion);
			HashSet<String> favorites = null;

			db.beginTransaction();
			/*if (oldVersion > STOP_LOCATIONS_STORE_ROUTE_STRINGS && oldVersion < VERBOSE_DB)
			{
				favorites = readOldFavorites(db);
			}
			else if (oldVersion >= VERBOSE_DB)
			{
				favorites = new HashSet<String>();
				populateFavorites(favorites, false, db);
			}*/

			if (oldVersion < CURRENT_DB_VERSION)
			{
				db.execSQL("DROP TABLE IF EXISTS " + directionsTable);
				db.execSQL("DROP TABLE IF EXISTS " + directionsStopsTable);
				db.execSQL("DROP TABLE IF EXISTS " + stopsTable);
				db.execSQL("DROP TABLE IF EXISTS " + routesTable);
				db.execSQL("DROP TABLE IF EXISTS " + pathsTable);
				db.execSQL("DROP TABLE IF EXISTS " + blobsTable);
				db.execSQL("DROP TABLE IF EXISTS " + verboseRoutes);
				db.execSQL("DROP TABLE IF EXISTS " + verboseStops);
				db.execSQL("DROP TABLE IF EXISTS " + stopsRoutesMap);
			}

			if (oldVersion < VERBOSE_DBV2_1)
			{
				db.execSQL("DROP TABLE IF EXISTS " + oldFavoritesTable);
				db.execSQL("DROP TABLE IF EXISTS " + newFavoritesTable);
			}

			//if it's verboseFavorites, we want to save it since it's user specified data

			onCreate(db);

			/*if (favorites != null)
			{
				writeVerboseFavorites(db, favorites);
			}*/

			db.setTransactionSuccessful();
			db.endTransaction();

		}
	}


	public static class DatabaseAgent {
		/**
		 * Fill the given HashSet with all stop tags that are favorites
		 * @param favorites
		 */
		public static void populateFavorites(ContentResolver contentResolver, 
				HashSet<String> favorites)
		{
			Cursor cursor = contentResolver.query(FAVORITES_URI, new String[]{stopTagKey},
					null, null, null);
			
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false)
			{
				String favoriteStopKey = cursor.getString(0);

				favorites.add(favoriteStopKey);

				cursor.moveToNext();
			}
		}

		public static void saveMapping(ContentResolver contentResolver, 
				MyHashMap<String, RouteConfig> mapping,
				boolean wipe, HashSet<String> sharedStops, UpdateAsyncTask task)
						throws IOException
						{
			ContentProviderOperation.newInsert(uri);
			contentResolver.applyBatch(authority, operations);
			if (wipe)
			{
				//database.delete(stopsTable, null, null);
				//database.delete(directionsTable, null, null);
				//database.delete(pathsTable, null, null);
				//database.delete(blobsTable, null, null);

				database.delete(verboseStops, null, null);
				database.delete(verboseRoutes, null, null);
			}

			int total = mapping.keySet().size();
			task.publish(new ProgressMessage(ProgressMessage.SET_MAX, total));

			int count = 0;
			for (String route : mapping.keySet())
			{
				RouteConfig routeConfig = mapping.get(route);
				if (routeConfig != null)
				{
					String routeTitle = routeConfig.getRouteTitle();
					saveMappingKernel(database, route, routeTitle, routeConfig, sharedStops);
				}

				count++;
				task.publish(count);
			}

			database.setTransactionSuccessful();
			database.endTransaction();
				}

		/**
		 * 
		 * @param database
		 * @param route
		 * @param routeConfig
		 * @param useInsert insert all rows, don't replace them. I assume this is faster since there's no lookup involved
		 * @throws IOException 
		 */
		private void saveMappingKernel(SQLiteDatabase database, String route, String routeTitle, RouteConfig routeConfig,
				HashSet<String> sharedStops) throws IOException
				{
			Box serializedPath = new Box(null, CURRENT_DB_VERSION);

			routeConfig.serializePath(serializedPath);

			byte[] serializedPathBlob = serializedPath.getBlob();

			{
				ContentValues values = new ContentValues();
				values.put(routeKey, route);
				values.put(routeTitleKey, routeTitle);
				values.put(pathsBlobKey, serializedPathBlob);
				values.put(colorKey, routeConfig.getColor());
				values.put(oppositeColorKey, routeConfig.getOppositeColor());

				database.replace(verboseRoutes, null, values);
			}

			//add all stops associated with the route, if they don't already exist


			database.delete(stopsRoutesMap, routeKey + "=?", new String[]{route});


			for (StopLocation stop : routeConfig.getStops())
			{
				/*"CREATE TABLE IF NOT EXISTS " + verboseStops + " (" + stopTagKey + " STRING PRIMARY KEY, " + 
				latitudeKey + " FLOAT, " + longitudeKey + " FLOAT, " + stopTitleKey + " STRING, " +
				branchKey + " STRING, " + platformOrderKey + " SHORT)"*/
				String stopTag = stop.getStopTag();

				if (sharedStops.contains(stopTag) == false)
				{

					sharedStops.add(stopTag);

					{
						ContentValues values = new ContentValues();
						values.put(stopTagKey, stopTag);
						values.put(latitudeKey, stop.getLatitudeAsDegrees());
						values.put(longitudeKey, stop.getLongitudeAsDegrees());
						values.put(stopTitleKey, stop.getTitle());

						database.replace(verboseStops, null, values);
					}

					if (stop instanceof SubwayStopLocation)
					{
						SubwayStopLocation subwayStop = (SubwayStopLocation)stop;
						ContentValues values = new ContentValues();
						values.put(stopTagKey, stopTag);
						values.put(platformOrderKey, subwayStop.getPlatformOrder());
						values.put(branchKey, subwayStop.getBranch());

						database.replace(subwaySpecificTable, null, values);
					}
				}

				{
					//show that there's a relationship between the stop and this route
					ContentValues values = new ContentValues();
					values.put(routeKey, route);
					values.put(stopTagKey, stopTag);
					values.put(dirTagKey, stop.getDirTagForRoute(route));
					database.replace(stopsRoutesMap, null, values);
				}
			}
				}

		public synchronized boolean checkFreeSpace() {
			SQLiteDatabase database = getReadableDatabase();
			try
			{
				String path = database.getPath();

				StatFs statFs = new StatFs(path);
				long freeSpace = (long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize(); 

				Log.v("BostonBusMap", "free database space: " + freeSpace);
				return freeSpace >= 1024 * 1024 * 4;
			}
			catch (Exception e)
			{
				//if for some reason we don't have permission to check free space available, just hope that everything's ok
				return true;
			}
		}


		public synchronized ArrayList<String> getAllStopTagsAtLocation(String stopTag)
		{
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			try
			{
				if (database.isOpen() == false)
				{
					Log.e("BostonBusMap", "SERIOUS ERROR: database didn't save data properly");
					return null;
				}

				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				builder.setTables(verboseStops + " as s1, " + verboseStops + " as s2");
				cursor = builder.query(database, new String[] {"s2." + stopTagKey},
						"s1." + stopTagKey + " = ? AND s1." + latitudeKey + " = s2." + latitudeKey +
						" AND s1." + longitudeKey + " = s2." + longitudeKey + "", new String[]{stopTag}, null, null, null);

				ArrayList<String> ret = new ArrayList<String>();
				cursor.moveToFirst();
				while (cursor.isAfterLast() == false)
				{
					String tag = cursor.getString(0);
					ret.add(tag);

					cursor.moveToNext();
				}

				return ret;
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}

		private void storeFavorite(ArrayList<String> stopTags)
		{
			if (stopTags == null || stopTags.size() == 0)
			{
				return;
			}

			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();
			for (String tag : stopTags)
			{
				ContentValues values = new ContentValues();
				values.put(stopTagKey, tag);

				database.replace(verboseFavorites, null, values);
			}

			database.setTransactionSuccessful();
			database.endTransaction();
		}


		public synchronized void saveFavorite(String stopTag, ArrayList<String> stopTags, boolean isFavorite) {
			if (isFavorite)
			{
				storeFavorite(stopTags);
			}
			else
			{
				//delete all stops at location

				SQLiteDatabase database = getWritableDatabase();

				if (database.isOpen() == false)
				{
					Log.e("BostonBusMap", "SERIOUS ERROR: database didn't save data properly");
					return;
				}

				database.beginTransaction();
				//delete all tags from favorites where the lat/lon of stopTag matches those tags
				database.delete(verboseFavorites, verboseFavorites + "." + stopTagKey + 
						" IN (SELECT s2." + stopTagKey + " FROM " + verboseStops + " as s1, " + verboseStops + " as s2 WHERE " +
						"s1." + latitudeKey + " = s2." + latitudeKey + " AND s1." + longitudeKey +
						" = s2." + longitudeKey + " AND s1." + stopTagKey + " = ?)", new String[]{stopTag});
				database.setTransactionSuccessful();
				database.endTransaction();
			}
		}

		public synchronized RouteConfig getRoute(String routeToUpdate, MyHashMap<String, StopLocation> sharedStops,
				TransitSystem transitSystem) throws IOException {
			SQLiteDatabase database = getReadableDatabase();
			Cursor routeCursor = null;
			Cursor stopCursor = null;
			try
			{
				/*db.execSQL("CREATE TABLE IF NOT EXISTS " + verboseRoutes + " (" + routeKey + " STRING PRIMARY KEY, " + colorKey + 
						" INTEGER, " + oppositeColorKey + " INTEGER, " + pathsBlobKey + " BLOB)");*/

				//get the route-specific information, like the path outline and the color
				routeCursor = database.query(verboseRoutes, new String[]{colorKey, oppositeColorKey, pathsBlobKey, routeTitleKey}, routeKey + "=?",
						new String[]{routeToUpdate}, null, null, null);
				if (routeCursor.getCount() == 0)
				{
					return null;
				}

				routeCursor.moveToFirst();

				TransitSource source = transitSystem.getTransitSource(routeToUpdate);

				int color = routeCursor.getInt(0);
				int oppositeColor = routeCursor.getInt(1);
				byte[] pathsBlob = routeCursor.getBlob(2);
				String routeTitle = routeCursor.getString(3);
				Box pathsBlobBox = new Box(pathsBlob, CURRENT_DB_VERSION);

				RouteConfig routeConfig = new RouteConfig(routeToUpdate, routeTitle, color, oppositeColor, source, pathsBlobBox);



				//get all stops, joining in the subway stops, making sure that the stop references the route we're on
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				String tables = verboseStops +
						" JOIN " + stopsRoutesMap + " AS sm1 ON (" + verboseStops + "." + stopTagKey + " = sm1." + stopTagKey + ")" +
						" JOIN " + stopsRoutesMap + " AS sm2 ON (" + verboseStops + "." + stopTagKey + " = sm2." + stopTagKey + ")" +
						" LEFT OUTER JOIN " + subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
						subwaySpecificTable + "." + stopTagKey + ")";


				/* select stops.tag, lat, lon, title, platformorder, branch, stopmapping1.dirTag, stopmapping2.route 
				 * from stops inner join stopmapping as stopmapping1 on (stops.tag = stopmapping1.tag) 
				 * inner join stopmapping as stopmapping2 on (stops.tag = stopmapping2.tag)
				 * left outer join subway on (stops.tag = subway.tag) 
				 * where stopmapping1.route=71;*/ 
				builder.setTables(tables);

				String[] projectionIn = new String[] {verboseStops + "." + stopTagKey, latitudeKey, longitudeKey, 
						stopTitleKey, platformOrderKey, branchKey, "sm2." + dirTagKey, "sm2." + routeKey};
				String select = "sm1." + routeKey + "=?";
				String[] selectArray = new String[]{routeToUpdate};

				//Log.v("BostonBusMap", SQLiteQueryBuilder.buildQueryString(false, tables, projectionIn, "sm1." + routeKey + "=\"" + routeToUpdate + "\"",
				//		null, null, null, null));

				stopCursor = builder.query(database, projectionIn, select, selectArray, null, null, null);


				stopCursor.moveToFirst();
				while (stopCursor.isAfterLast() == false)
				{
					String stopTag = stopCursor.getString(0);
					String dirTag = stopCursor.getString(6);
					String route = stopCursor.getString(7);

					//we need to ensure this stop is in the sharedstops and the route
					StopLocation stop = sharedStops.get(stopTag);
					if (stop != null)
					{
						//make sure it exists in the route too
						StopLocation stopInRoute = routeConfig.getStop(stopTag);
						if (stopInRoute == null)
						{
							routeConfig.addStop(stopTag, stop);
						}
						stop.addRouteAndDirTag(route, dirTag);
					}
					else
					{
						stop = routeConfig.getStop(stopTag);

						if (stop == null)
						{
							float latitude = stopCursor.getFloat(1);
							float longitude = stopCursor.getFloat(2);
							String stopTitle = stopCursor.getString(3);
							String branch = stopCursor.getString(5);

							int platformOrder = stopCursor.getInt(4);

							stop = transitSystem.createStop(latitude, longitude, stopTag, stopTitle, platformOrder, branch, route, dirTag);

							routeConfig.addStop(stopTag, stop);
						}

						sharedStops.put(stopTag, stop);
					}
					stopCursor.moveToNext();
				}

				return routeConfig;
			}
			finally
			{
				if (routeCursor != null)
				{
					routeCursor.close();
				}
				if (stopCursor != null)
				{
					stopCursor.close();
				}
			}
		}

		public synchronized ArrayList<String> routeInfoNeedsUpdating(String[] supportedRoutes) {
			HashSet<String> routesInDB = new HashSet<String>();
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			try
			{
				cursor = database.query(verboseRoutes, new String[]{routeKey}, null, null, null, null, null);
				cursor.moveToFirst();
				while (cursor.isAfterLast() == false)
				{
					routesInDB.add(cursor.getString(0));

					cursor.moveToNext();
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}

			ArrayList<String> routesThatNeedUpdating = new ArrayList<String>();

			for (String route : supportedRoutes)
			{
				if (routesInDB.contains(route) == false)
				{
					routesThatNeedUpdating.add(route);
				}
			}

			return routesThatNeedUpdating;
		}

		/**
		 * Populate directions from the database
		 * 
		 * NOTE: these data structures are assumed to be synchronized
		 * @param indexes
		 * @param names
		 * @param titles
		 */
		public synchronized void refreshDirections(MyHashMap<String, Direction> directions) {
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			try
			{
				cursor = database.query(directionsTable, new String[]{dirTagKey, dirNameKey, dirTitleKey, dirRouteKey, dirUseAsUIKey},
						null, null, null, null, null);
				cursor.moveToFirst();
				while (cursor.isAfterLast() == false)
				{
					String dirTag = cursor.getString(0);
					String dirName = cursor.getString(1);
					String dirTitle = cursor.getString(2);
					String dirRoute = cursor.getString(3);
					boolean dirUseAsUI = cursor.getInt(4) == INT_TRUE;

					Direction direction = new Direction(dirName, dirTitle, dirRoute, dirUseAsUI);
					directions.put(dirTag, direction);

					cursor.moveToNext();
				}

				cursor.close();
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}

		public synchronized void writeDirections(boolean wipe, MyHashMap<String, Direction> directions) {
			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();
			if (wipe)
			{
				database.delete(directionsTable, null, null);
				database.delete(directionsStopsTable, null, null);
			}

			for (String dirTag : directions.keySet())
			{
				Direction direction = directions.get(dirTag);
				String name = direction.getName();
				String title = direction.getTitle();
				String route = direction.getRoute();
				boolean useAsUI = direction.isUseForUI();

				ContentValues values = new ContentValues();
				values.put(dirNameKey, name);
				values.put(dirRouteKey, route);
				values.put(dirTagKey, dirTag);
				values.put(dirTitleKey, title);
				values.put(dirUseAsUIKey, useAsUI ? INT_TRUE : INT_FALSE);

				if (wipe)
				{
					database.insert(directionsTable, null, values);
				}
				else
				{
					database.replace(directionsTable, null, values);
				}

				for (String stopTag : direction.getStopTags()) {
					ContentValues stopValues = new ContentValues();
					stopValues.put(stopTagKey, stopTag);
					stopValues.put(dirTagKey, dirTag);
					if (wipe) {
						database.insert(directionsStopsTable, null, stopValues);
					}
					else
					{
						database.replace(directionsStopsTable, null, stopValues);
					}
				}

			}
			database.setTransactionSuccessful();
			database.endTransaction();
		}

		public synchronized void saveFavorites(HashSet<String> favoriteStops, MyHashMap<String, StopLocation> sharedStops) {
			SQLiteDatabase database = getWritableDatabase();
			database.beginTransaction();

			database.delete(verboseFavorites, null, null);

			for (String stopTag : favoriteStops)
			{
				StopLocation stopLocation = sharedStops.get(stopTag);

				if (stopLocation != null)
				{
					ContentValues values = new ContentValues();
					values.put(stopTagKey, stopTag);
					database.replace(verboseFavorites, null, values);
				}
			}

			database.setTransactionSuccessful();
			database.endTransaction();
		}

		public synchronized Cursor getCursorForRoutes() {
			SQLiteDatabase database = getReadableDatabase();

			return database.query(verboseRoutes, new String[]{routeKey}, null, null, null, null, null);
		}

		public synchronized Cursor getCursorForSearch(String search, int mode) {
			String[] columns = new String[] {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_QUERY, SearchManager.SUGGEST_COLUMN_TEXT_2};
			MatrixCursor ret = new MatrixCursor(columns);

			SQLiteDatabase database = getReadableDatabase();
			addSearchRoutes(database, search, ret);
			addSearchStops(database, search, ret);


			return ret;
		}

		private void addSearchRoutes(SQLiteDatabase database, String search, MatrixCursor ret)
		{
			if (search == null)
			{
				return;
			}

			Cursor cursor = null;
			try
			{
				cursor = database.query(verboseRoutes, new String[]{routeTitleKey, routeKey}, routeTitleKey + " LIKE ?",
						new String[]{"%" + search + "%"}, null, null, routeTitleKey);
				if (cursor.moveToFirst() == false)
				{
					return;
				}

				while (!cursor.isAfterLast())
				{
					String routeTitle = cursor.getString(0);
					String routeKey = cursor.getString(1);

					ret.addRow(new Object[]{ret.getCount(), routeTitle, "route " + routeKey, "Route"});

					cursor.moveToNext();
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}

		private void addSearchStops(SQLiteDatabase database, String search, MatrixCursor ret)
		{
			if (search == null)
			{
				return;
			}

			Cursor cursor = null;
			try
			{
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

				String tables = verboseStops +
						" JOIN " + stopsRoutesMap + " AS sm1 ON (" + verboseStops + "." + stopTagKey + " = sm1." + stopTagKey + ")" +
						" JOIN " + verboseRoutes + " AS r1 ON (sm1." + routeKey + " = r1." + routeKey + ")";


				builder.setTables(tables);


				String thisStopTitleKey = verboseStops + "." + stopTitleKey;
				String[] projectionIn = new String[] {thisStopTitleKey, verboseStops + "." + stopTagKey, "r1." + routeTitleKey};
				String select = thisStopTitleKey + " LIKE ?";
				String[] selectArray = new String[]{"%" + search + "%"};

				cursor = builder.query(database, projectionIn, select, selectArray, null, null, thisStopTitleKey);

				if (cursor.moveToFirst() == false)
				{
					return;
				}

				int count = 0;
				String prevStopTag = null;
				String prevStopTitle = null;
				StringBuilder routes = new StringBuilder();
				int routeCount = 0;
				while (!cursor.isAfterLast())
				{
					String stopTitle = cursor.getString(0);
					String stopTag = cursor.getString(1);
					String routeTitle = cursor.getString(2);

					if (prevStopTag == null)
					{
						// do nothing, first row
						prevStopTag = stopTag;
						prevStopTitle = stopTitle;
						routeCount++;
						routes.append(routeTitle);
					}
					else if (!prevStopTag.equals(stopTag))
					{
						// change in row. write out this row
						String routeString = routeCount == 0 ? "Stop" 
								: routeCount == 1 ? ("Stop on route " + routes.toString())
										: ("Stop on routes " + routes);
								ret.addRow(new Object[]{count, prevStopTitle, "stop " + prevStopTag, routeString});
								prevStopTag = stopTag;
								prevStopTitle = stopTitle;
								routeCount = 1;
								routes.setLength(0);
								routes.append(routeTitle);
					}
					else
					{
						// just add a new route
						routes.append(", ");
						routes.append(routeTitle);
						routeCount++;
					}


					cursor.moveToNext();
					count++;
				}

				if (prevStopTag != null)
				{
					// at least one row
					String routeString = routeCount == 0 ? "Stop" 
							: routeCount == 1 ? ("Stop on route " + routes.toString())
									: ("Stop on routes " + routes);
							ret.addRow(new Object[]{count, prevStopTitle, "stop " + prevStopTag, routeString});
				}
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}
		public synchronized ArrayList<StopLocation> getClosestStops(double currentLat, double currentLon, TransitSystem transitSystem, MyHashMap<String, StopLocation> sharedStops, int limit)
		{
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			try
			{
				// what we should scale longitude by for 1 unit longitude to roughly equal 1 unit latitude
				double lonFactor = Math.cos(currentLat * Geometry.degreesToRadians);


				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

				String tables = verboseStops;

				builder.setTables(tables);

				final String distanceKey = "distance";
				String[] projectionIn = new String[] {stopTagKey, distanceKey};
				HashMap<String, String> projectionMap = new HashMap<String, String>();
				projectionMap.put(stopTagKey, stopTagKey);

				String latDiff = "(" + latitudeKey + " - " + currentLat + ")";
				String lonDiff = "((" + longitudeKey + " - " + currentLon + ")*" + lonFactor + ")";
				projectionMap.put("distance", latDiff + "*" + latDiff + " + " + lonDiff + "*" + lonDiff + " AS " + distanceKey);
				builder.setProjectionMap(projectionMap);
				cursor = builder.query(database, projectionIn, null, null, null, null, distanceKey, Integer.valueOf(limit).toString());

				if (cursor.moveToFirst() == false)
				{
					return new ArrayList<StopLocation>();
				}

				ArrayList<String> stopTags = new ArrayList<String>();
				while (!cursor.isAfterLast())
				{
					String id = cursor.getString(0);
					stopTags.add(id);

					cursor.moveToNext();
				}

				getStops(stopTags, transitSystem, sharedStops);

				ArrayList<StopLocation> ret = new ArrayList<StopLocation>();
				for (String stopTag : stopTags)
				{
					ret.add(sharedStops.get(stopTag));
				}

				return ret;
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
			}
		}

		/*	public synchronized List<StopLocation> getClosestStopsWithDirTag(double currentLat, double currentLon, 
				TransitSystem transitSystem, MyHashMap<String, StopLocation> sharedStops,
				int limit, MyHashMap<String, Direction> directionsToUpdate)
		{
			if (directionsToUpdate.size() == 0) {
				return Collections.emptyList();
			}

			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			try
			{
				// what we should scale longitude by for 1 unit longitude to roughly equal 1 unit latitude
				double lonFactor = Math.cos(currentLat * Geometry.degreesToRadians);


				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

				String tables = verboseStops;

				builder.setTables(tables);

				String select;
				String[] selectArray;
				if (directionsToUpdate.keySet().size() == 1) {
					select = stop + " LIKE ?";
					selectArray = new String[]{"%" + search + "%"};
				}

				final String distanceKey = "distance";
				String[] projectionIn = new String[] {stopTagKey, distanceKey};
				HashMap<String, String> projectionMap = new HashMap<String, String>();
				projectionMap.put(stopTagKey, stopTagKey);

				String latDiff = "(" + latitudeKey + " - " + currentLat + ")";
				String lonDiff = "((" + longitudeKey + " - " + currentLon + ")*" + lonFactor + ")";
				projectionMap.put("distance", latDiff + "*" + latDiff + " + " + lonDiff + "*" + lonDiff + " AS " + distanceKey);
				builder.setProjectionMap(projectionMap);
				cursor = builder.query(database, projectionIn, select, selectArray, null, null, distanceKey, Integer.valueOf(limit).toString());

				if (cursor.moveToFirst() == false)
				{
					return new ArrayList<StopLocation>();
				}

				ArrayList<String> stopTags = new ArrayList<String>();
				while (!cursor.isAfterLast())
				{
					String id = cursor.getString(0);
					stopTags.add(id);

					cursor.moveToNext();
				}

				getStops(stopTags, transitSystem, sharedStops);

				ArrayList<StopLocation> ret = new ArrayList<StopLocation>();
				for (String stopTag : stopTags)
				{
					ret.add(sharedStops.get(stopTag));
				}

				return ret;
			}
			finally
			{
				if (cursor != null)
				{
					cursor.close();
				}
				database.close();
			}
		}*/

		public synchronized Cursor getCursorForDirection(String dirTag) {
			SQLiteDatabase database = getReadableDatabase();

			return database.query(directionsTable, new String[]{dirTagKey, dirTitleKey, dirRouteKey}, dirTagKey + "=?", 
					new String[]{dirTag}, null, null, null);

		}

		public synchronized Cursor getCursorForDirections() {
			SQLiteDatabase database = getReadableDatabase();

			return database.query(directionsTable, new String[]{dirTagKey, dirTitleKey, dirRouteKey}, null, 
					null, null, null, null);
		}

		public synchronized void upgradeIfNecessary() {
			//trigger an upgrade so future calls of getReadableDatabase won't complain that you can't upgrade a read only db
			getWritableDatabase();

		}



		public StopLocation getStopByTagOrTitle(String tagQuery, String titleQuery, TransitSystem transitSystem)
		{
			SQLiteDatabase database = getReadableDatabase();
			Cursor stopCursor = null;
			try
			{
				//TODO: we should have a factory somewhere to abstract details away regarding subway vs bus

				//get stop with name stopTag, joining with the subway table
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				String tables = verboseStops + " JOIN " + stopsRoutesMap + " ON (" + verboseStops + "." + stopTagKey + " = " +
						stopsRoutesMap + "." + stopTagKey + ") LEFT OUTER JOIN " +
						subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
						subwaySpecificTable + "." + stopTagKey + ")";


				builder.setTables(tables);

				String[] projectionIn = new String[] {verboseStops + "." + stopTagKey, latitudeKey, longitudeKey, 
						stopTitleKey, platformOrderKey, branchKey, stopsRoutesMap + "." + routeKey, stopsRoutesMap + "." + dirTagKey};

				//if size == 1, where clause is tag = ?. if size > 1, where clause is "IN (tag1, tag2, tag3...)"
				StringBuilder select;
				String[] selectArray;

				select = new StringBuilder(verboseStops + "." + stopTagKey + "=? OR " + verboseStops + "." + stopTitleKey + "=?");
				selectArray = new String[]{tagQuery, titleQuery};

				stopCursor = builder.query(database, projectionIn, select.toString(), selectArray, null, null, null);

				stopCursor.moveToFirst();

				if (stopCursor.isAfterLast() == false)
				{
					String stopTag = stopCursor.getString(0);

					String route = stopCursor.getString(6);
					String dirTag = stopCursor.getString(7);

					float lat = stopCursor.getFloat(1);
					float lon = stopCursor.getFloat(2);
					String title = stopCursor.getString(3);

					int platformOrder = 0;
					String branch = null;
					if (stopCursor.isNull(4) == false)
					{
						platformOrder = stopCursor.getInt(4);
						branch = stopCursor.getString(5);
					}

					StopLocation stop = transitSystem.createStop(lat, lon, stopTag, title, platformOrder, branch, route, dirTag);
					return stop;
				}
				else
				{
					return null;
				}

			}
			finally
			{
				if (stopCursor != null)
				{
					stopCursor.close();
				}
			}
		}

		/**
		 * Read stops from the database and return a mapping of the stop tag to the stop object
		 * @param stopTag
		 * @param transitSystem
		 * @return
		 */
		public void getStops(List<String> stopTags, TransitSystem transitSystem, MyHashMap<String, StopLocation> outputMapping) {
			if (stopTags == null || stopTags.size() == 0)
			{
				return;
			}

			SQLiteDatabase database = getReadableDatabase();
			Cursor stopCursor = null;
			try
			{
				//TODO: we should have a factory somewhere to abstract details away regarding subway vs bus

				//get stop with name stopTag, joining with the subway table
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				String tables = verboseStops + " JOIN " + stopsRoutesMap + " ON (" + verboseStops + "." + stopTagKey + " = " +
						stopsRoutesMap + "." + stopTagKey + ") LEFT OUTER JOIN " +
						subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
						subwaySpecificTable + "." + stopTagKey + ")";


				builder.setTables(tables);

				String[] projectionIn = new String[] {verboseStops + "." + stopTagKey, latitudeKey, longitudeKey, 
						stopTitleKey, platformOrderKey, branchKey, stopsRoutesMap + "." + routeKey, stopsRoutesMap + "." + dirTagKey};

				//if size == 1, where clause is tag = ?. if size > 1, where clause is "IN (tag1, tag2, tag3...)"
				StringBuilder select;
				String[] selectArray;
				if (stopTags.size() == 1)
				{
					String stopTag = stopTags.get(0);

					select = new StringBuilder(verboseStops + "." + stopTagKey + "=?");
					selectArray = new String[]{stopTag};

					//Log.v("BostonBusMap", SQLiteQueryBuilder.buildQueryString(false, tables, projectionIn, verboseStops + "." + stopTagKey + "=\"" + stopTagKey + "\"",
					//		null, null, null, null));
				}
				else
				{
					select = new StringBuilder(verboseStops + "." + stopTagKey + " IN (");

					for (int i = 0; i < stopTags.size(); i++)
					{
						String stopTag = stopTags.get(i);
						select.append('\'').append(stopTag);
						if (i != stopTags.size() - 1)
						{
							select.append("', ");
						}
						else
						{
							select.append("')");
						}
					}
					selectArray = null;

					//Log.v("BostonBusMap", select.toString());
				}

				stopCursor = builder.query(database, projectionIn, select.toString(), selectArray, null, null, null);

				stopCursor.moveToFirst();

				//iterate through the stops in the database and create new ones if necessary
				//stops will be repeated if they are on multiple routes. If so, just skip to the bottom and add the route and dirTag
				while (stopCursor.isAfterLast() == false)
				{
					String stopTag = stopCursor.getString(0);

					String route = stopCursor.getString(6);
					String dirTag = stopCursor.getString(7);

					StopLocation stop = outputMapping.get(stopTag);
					if (stop == null)
					{
						float lat = stopCursor.getFloat(1);
						float lon = stopCursor.getFloat(2);
						String title = stopCursor.getString(3);

						int platformOrder = 0;
						String branch = null;
						if (stopCursor.isNull(4) == false)
						{
							platformOrder = stopCursor.getInt(4);
							branch = stopCursor.getString(5);
						}

						stop = transitSystem.createStop(lat, lon, stopTag, title, platformOrder, branch, route, dirTag);
						outputMapping.put(stopTag, stop);
					}
					else
					{
						stop.addRouteAndDirTag(route, dirTag);
					}

					stopCursor.moveToNext();
				}
			}
			finally
			{
				if (stopCursor != null)
				{
					stopCursor.close();
				}
			}
		}

		public ArrayList<StopLocation> getStopsByDirtag(String dirTag, TransitSystem transitSystem) {
			SQLiteDatabase database = getReadableDatabase();
			Cursor stopCursor = null;
			ArrayList<StopLocation> ret = new ArrayList<StopLocation>();
			try
			{
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				String tables = verboseStops + " JOIN " + stopsRoutesMap + " ON (" + verboseStops + "." + stopTagKey + " = " +
						stopsRoutesMap + "." + stopTagKey + ") LEFT OUTER JOIN " +
						subwaySpecificTable + " ON (" + verboseStops + "." + stopTagKey + " = " + 
						subwaySpecificTable + "." + stopTagKey + ")";	

				builder.setTables(tables);

				String[] projectionIn = new String[] {verboseStops + "." + stopTagKey, latitudeKey, longitudeKey, 
						stopTitleKey, platformOrderKey, branchKey, stopsRoutesMap + "." + routeKey, stopsRoutesMap + "." + dirTagKey};

				//if size == 1, where clause is tag = ?. if size > 1, where clause is "IN (tag1, tag2, tag3...)"
				StringBuilder select;
				String[] selectArray;

				select = new StringBuilder(verboseStops + "." + dirTagKey + "=?");
				selectArray = new String[]{dirTag};

				stopCursor = builder.query(database, projectionIn, select.toString(), selectArray, null, null, null);

				stopCursor.moveToFirst();
				while (stopCursor.isAfterLast() == false)
				{
					String stopTag = stopCursor.getString(0);

					String route = stopCursor.getString(6);

					float lat = stopCursor.getFloat(1);
					float lon = stopCursor.getFloat(2);
					String title = stopCursor.getString(3);

					int platformOrder = 0;
					String branch = null;
					if (stopCursor.isNull(4) == false)
					{
						platformOrder = stopCursor.getInt(4);
						branch = stopCursor.getString(5);
					}

					StopLocation stop = transitSystem.createStop(lat, lon, stopTag, title, platformOrder, branch, route, dirTag);
					ret.add(stop);
					stopCursor.moveToNext();
				}
			}
			finally
			{
				if (stopCursor != null) {
					stopCursor.close();
				}
			}
			return ret;
		}

		public HashSet<String> getDirectionTagsForStop(String stopTag) {
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			HashSet<String> ret = new HashSet<String>();
			try
			{
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				builder.setTables(directionsStopsTable);
				cursor = builder.query(database, new String[] {dirTagKey},
						stopTagKey + " = ?", new String[] {stopTag}, null, null, null);

				cursor.moveToFirst();
				while (cursor.isAfterLast() == false) {
					String dirTag = cursor.getString(0);
					ret.add(dirTag);
					cursor.moveToNext();
				}
			}
			finally
			{
				if (cursor != null) {
					cursor.close();
				}
			}
			return ret;
		}

		public HashSet<String> getStopTagsForDirTag(String dirTag) {
			SQLiteDatabase database = getReadableDatabase();
			Cursor cursor = null;
			HashSet<String> ret = new HashSet<String>();
			try
			{
				SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
				builder.setTables(directionsStopsTable);
				cursor = builder.query(database, new String[] {stopTagKey},
						dirTagKey + " = ?", new String[] {dirTag}, null, null, null);

				cursor.moveToFirst();
				while (cursor.isAfterLast() == false) {
					String stopTag = cursor.getString(0);
					ret.add(stopTag);
					cursor.moveToNext();
				}
			}
			finally
			{
				if (cursor != null) {
					cursor.close();
				}
			}
			return ret;
		}

	}


	private DatabaseHelper helper;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "favorites/#", FAVORITES);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case FAVORITES:
			count = db.delete(verboseFavorites, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case FAVORITES:
			return FAVORITES_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = uriMatcher.match(uri);
		switch (match) {
		case FAVORITES:
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = helper.getWritableDatabase();
		switch (match) {
		case FAVORITES:
			long rowId = db.insert(verboseFavorites, null, values);
			if (rowId > 0) {
				Uri favoriteUri = ContentUris.withAppendedId(FAVORITES_URI, rowId);
				return favoriteUri;
			}
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		helper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		switch (uriMatcher.match(uri)) {
		case FAVORITES:
			builder.setTables(verboseFavorites);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}

		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor c = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = helper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case FAVORITES:
			count = db.update(verboseFavorites, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}