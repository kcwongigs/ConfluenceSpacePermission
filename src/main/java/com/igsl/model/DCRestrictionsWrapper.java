package com.igsl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class to handle the complex structure mandated by Atlassian
 */
public class DCRestrictionsWrapper {
	private Set<String> operationSet = new HashSet<>();
	private Map<String, Map<String, DCRestrictionWrapper>> users = new HashMap<>();
	private Map<String, Map<String, DCRestrictionWrapper>> groups = new HashMap<>();
	
	/**
	 * Append data from provided DCRestrictions.
	 * @param restrictions DCRestrictions
	 * @return Number of users/groups added, whichever is higher
	 */
	public int addData(DCRestrictions restrictions) {
		int userCount = 0;
		int groupCount = 0;
		for (DCRestriction restriction : restrictions.getResults()) {
			String operation = restriction.getOperation();
			operationSet.add(operation);
			for (DCRestrictionTargetGroup group : restriction.getRestrictions().getGroup().getResults()) {
				addGroup(operation, group.getName());
				userCount++;
			}
			for (DCRestrictionTargetUser user : restriction.getRestrictions().getUser().getResults()) {
				addUser(operation, user.getUsername());
				groupCount++;
			}
		}
		return Math.max(userCount, groupCount);
	}
	
	/**
	 * Convert to format used by restriction update
	 * @return
	 */
	public List<DCRestrictionUpdate> convert() {
		List<DCRestrictionUpdate> result = new ArrayList<>();
		for (String operation : operationSet) {
			DCRestrictionUpdate restriction = new DCRestrictionUpdate(operation);
			if (users.containsKey(operation)) {
				for (DCRestrictionWrapper wrapper : users.get(operation).values()) {
					restriction.getRestrictions().getUser().add(new DCRestrictionUpdateTargetUser(wrapper.getName()));
				}
			}
			if (groups.containsKey(operation)) {
				for (DCRestrictionWrapper wrapper : groups.get(operation).values()) {
					restriction.getRestrictions().getGroup().add(new DCRestrictionUpdateTargetGroup(wrapper.getName()));
				}
			}
			result.add(restriction);
		}
		return result;
	}

	public int getUserCount(String permission) {
		if (users.containsKey(permission)) {
			return users.get(permission).size();
		}
		return 0;
	}

	public int getGroupCount(String permission) {
		if (groups.containsKey(permission)) {
			return groups.get(permission).size();
		}
		return 0;
	}

	public boolean hasUser(String permission, String name) {
		return users.containsKey(permission) && 
				users.get(permission).containsKey(name);
	}
	
	public boolean hasGroup(String permission, String name) {
		return groups.containsKey(permission) && 
				groups.get(permission).containsKey(name);
	}
	
	public void addUser(String permission, String name) {
		if (!users.containsKey(permission)) {
			operationSet.add(permission);
			users.put(permission, new HashMap<>());
		}
		users.get(permission).put(name, new DCRestrictionWrapper(name));
	}

	public void removeUser(String permission, String name) {
		if (users.containsKey(permission) && 
			users.get(permission).containsKey(name)) {
			users.get(permission).remove(name);
		}
	}
	
	public void addGroup(String permission, String name) {
		if (!groups.containsKey(permission)) {
			operationSet.add(permission);
			groups.put(permission, new HashMap<>());
		}
		groups.get(permission).put(name, new DCRestrictionWrapper(name));
	}
	
	public void removeGroup(String permission, String name) {
		if (groups.containsKey(permission) && 
			groups.get(permission).containsKey(name)) {
			groups.get(permission).remove(name);
		}
	}
}
