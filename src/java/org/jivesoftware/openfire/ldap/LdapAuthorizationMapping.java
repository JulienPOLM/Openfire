/**
 * $RCSfile$
 * $Revision: $
 * $Date: 2006-04-07 09:28:54 -0500 (Fri, 07 Apr 2006) $
 *
 * Copyright (C) 2008 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution, or a commercial license
 * agreement with Jive.
 */

package org.jivesoftware.openfire.ldap;

import org.jivesoftware.openfire.ldap.LdapManager;
import org.jivesoftware.openfire.auth.AuthorizationMapping;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.JiveGlobals;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;



/**
 * Provider for authorization mapping using LDAP. If the authenticated
 * principal did not request a username, provide one via LDAP. Specify the
 * lookup field in the <tt>openfire.xml</tt> file. An entry in that file would
 * look like the following:
 * <p/>
 * <pre>
 *   &lt;ldap&gt;
 *     &lt;princField&gt; k5login &lt;/princField&gt;
 *     &lt;princSearchFilter&gt; princField={0}  &lt;/princSearchFilter&gt;
 *   &lt;/ldap&gt;</pre>
 * <p/>
 * Each ldap object that represents a user is expcted to have exactly one of
 * ldap.usernameField and ldap.princField, and they are both expected to be unique
 * over the search base.  A search will be performed over all objects where 
 * princField = principal, and the usernameField will be returned.
 * Note that it is expected this search return exactly one object. (There can
 * only be one default) If more than one is returned, the first entry 
 * encountered will be used, and no sorting is performed or requested.
 * If more control over the search is needed, you can specify the mapSearchFilter
 * used to perform the LDAP query. 
 * This implementation requires that LDAP be configured, obviously.
 *
 *
 * @author Jay Kline
 */
public class LdapAuthorizationMapping implements AuthorizationMapping {

    private LdapManager manager;
    private String usernameField;
    private String princField;
    private String princSearchFilter;

    public LdapAuthorizationMapping() {
        manager = LdapManager.getInstance();
        usernameField = manager.getUsernameField();
        princField = JiveGlobals.getXMLProperty("ldap.princField", "k5login");
        princSearchFilter = JiveGlobals.getXMLProperty("ldap.princSearchFilter");
        StringBuilder filter = new StringBuilder();
        if(princSearchFilter == null) {
            filter.append("(").append(princField).append("={0})");
        } else {
            filter.append("(&(").append(princField).append("={0})(");
            filter.append(princSearchFilter).append("))");
        }
        princSearchFilter = filter.toString();
    }

    public String map(String principal) {
        String username = principal;
        DirContext ctx = null;
        try {
            Log.debug("LdapAuthorizationMapping: Starting LDAP search...");
            String usernameField = manager.getUsernameField();
            String baseDN = manager.getBaseDN();
            boolean subTreeSearch = manager.isSubTreeSearch();
            ctx = manager.getContext();
            SearchControls constraints = new SearchControls();
            if (subTreeSearch) {
                constraints.setSearchScope
            (SearchControls.SUBTREE_SCOPE);
            }
            // Otherwise, only search a single level.
            else {
                constraints.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }
            constraints.setReturningAttributes(new String[] { usernameField });

            NamingEnumeration answer = ctx.search("", princSearchFilter, new String[] {principal},
                    constraints);
            Log.debug("LdapAuthorizationMapping: ... search finished");
            if (answer == null || !answer.hasMoreElements()) {
                Log.debug("LdapAuthorizationMapping: Username based on principal '" + principal + "' not found.");
                return principal;
            }
            Attributes atrs = ((SearchResult)answer.next()).getAttributes();
            Attribute usernameAttribute = atrs.get(usernameField);
            username = (String) usernameAttribute.get();
        }
        catch (Exception e) {
            // Ignore.
        }
        finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            }
            catch (Exception ignored) {
                // Ignore.
            }
        }
        return username;
    }

    /**
     * Returns the short name of the Policy
     *
     * @return The short name of the Policy
     */
    public String name() {
        return "LDAP Authorization Mapping";
    }

    /**
     * Returns a description of the Policy
     *
     * @return The description of the Policy.
     */
    public String description() {
        return "Provider for authorization using LDAP. Returns the principals default username using the attribute specified in ldap.princField.";
    }
}
