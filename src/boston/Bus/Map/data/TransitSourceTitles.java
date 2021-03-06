package boston.Bus.Map.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A limited version of RouteTitles which doesn't allow indexing (since it doesn't make sense with
 * only one transit source)
 * @author schneg
 *
 */
public class TransitSourceTitles {
	protected final ImmutableBiMap<String, String> map;
	
	public TransitSourceTitles(ImmutableBiMap<String, String> map) {
		this.map = map;
		
	}

	public Set<String> routeTags() {
		return map.keySet();
	}

	public Collection<String> routeTitles() {
		return map.values();
	}

	public String getTitle(String routeKey) {
		return map.get(routeKey);
	}

	public String getKey(String routeTitle) {
		return map.inverse().get(routeTitle);
	}

	public String[] titleArray() {
		return map.values().toArray(new String[0]);
	}

	public String[] tagArray() {
		return map.keySet().toArray(new String[0]);
	}

	public int size() {
		return map.size();
	}

	public boolean hasRoute(String tag) {
		return map.containsKey(tag);
	}

}
