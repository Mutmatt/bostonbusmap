package boston.Bus.Map.parser;

import java.io.IOException;

import boston.Bus.Map.data.IAlerts;
import android.content.Context;

public interface IAlertsParser {
	/**
	 * Download alerts from the internet, then return them
	 * @param context
	 * @return
	 */
	public IAlerts obtainAlerts(Context context) throws IOException;
}
