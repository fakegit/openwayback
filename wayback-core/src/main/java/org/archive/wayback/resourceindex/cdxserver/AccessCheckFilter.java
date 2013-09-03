package org.archive.wayback.resourceindex.cdxserver;

import org.archive.cdxserver.auth.AuthToken;
import org.archive.cdxserver.filter.CDXAccessFilter;
import org.archive.cdxserver.filter.FilenamePrefixFilter;
import org.archive.format.cdx.CDXLine;
import org.archive.wayback.core.FastCaptureSearchResult;
import org.archive.wayback.exception.AdministrativeAccessControlException;
import org.archive.wayback.exception.RobotAccessControlException;
import org.archive.wayback.resourceindex.filters.ExclusionFilter;

public class AccessCheckFilter implements CDXAccessFilter {
	
	protected ExclusionFilter adminFilter;
	protected ExclusionFilter robotsFilter;
	protected FilenamePrefixFilter prefixFilter;
	
	protected FastCaptureSearchResult resultTester;
	
	protected String lastKey;
	protected boolean cachedValue = false;

	public AccessCheckFilter(AuthToken token, 
			ExclusionFilter adminFilter,
			ExclusionFilter robotsFilter,
			FilenamePrefixFilter prefixFilter) {
	    
	    this.adminFilter = adminFilter;
	    this.robotsFilter = robotsFilter;
	    
	    this.prefixFilter = prefixFilter;
	    
	    this.resultTester = new FastCaptureSearchResult();
    }
	
	public boolean include(String urlKey, String originalUrl) {
		
		if ((lastKey != null) && lastKey.equals(urlKey)) {
			return cachedValue;
		}

		resultTester.setUrlKey(urlKey);
		resultTester.setOriginalUrl(originalUrl);
		
		int status = ExclusionFilter.FILTER_EXCLUDE;
		
		try {			
			// Admin Excludes
			if (adminFilter != null) {
				status = adminFilter.filterObject(resultTester);
			}
			
			if (status != ExclusionFilter.FILTER_INCLUDE) {
				throw new RuntimeException(new AdministrativeAccessControlException(originalUrl + " is not available in the Wayback Machine."));
			}
			
			// Robot Excludes
			if (robotsFilter != null) {
				status = robotsFilter.filterObject(resultTester);
			}
			
			if (status != ExclusionFilter.FILTER_INCLUDE) {
				throw new RuntimeException(new RobotAccessControlException(originalUrl + " is blocked by the sites robots.txt file"));
			}
					
		} catch (Exception e) {
			//Is this right?
			cachedValue = false;
		}
		
		cachedValue = true;
		lastKey = urlKey;
		
		return cachedValue;
    }

	@Override
    public boolean include(CDXLine line) {
		
	    if (!include(line.getUrlKey(), line.getOriginalUrl())) {
	    	return false;
	    }
	    
		// Custom Prefix Filter
		if (prefixFilter != null) {
			if (!prefixFilter.include(line)) {
				return false;
			}
		}
		
		return true;
    }
}