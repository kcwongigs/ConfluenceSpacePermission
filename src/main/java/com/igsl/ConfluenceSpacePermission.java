package com.igsl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.igsl.model.AddPermission;
import com.igsl.model.DCDataObject;
import com.igsl.model.GetPermission;
import com.igsl.model.GetPermissions;
import com.igsl.model.Operation;
import com.igsl.model.Principal;
import com.igsl.model.SpaceObject;
import com.igsl.model.SpaceObjects;
import com.igsl.model.Subject;

/**
 * Utility to manage space permission for Confluence DC and Cloud.
 */
public class ConfluenceSpacePermission {	
	private static final Logger LOGGER = LogManager.getLogger(ConfluenceSpacePermission.class);
	
	private static final String DEFAULT_SCHEME = "https";
	private static final String ALL_SPACES = "%";
	
	private static final String PATH_DC_ADD_SPACE_PERMISSION = "/rpc/json-rpc/confluenceservice-v2/addPermissionToSpace";
	private static final String PATH_DC_REMOVE_SPACE_PERMISSION = "/rpc/json-rpc/confluenceservice-v2/removePermissionFromSpace";
	private static final String PATH_DC_GET_SPACES = "/rpc/json-rpc/confluenceservice-v2/getSpaces";
	
	private static final String PATH_CLOUD_GET_SPACES = "/wiki/api/v2/spaces";
	private static final String PATH_CLOUD_GET_PERMISSION_ID = "/wiki/api/v2/spaces/{id}/permissions";	// GET
	private static final String PATH_CLOUD_ADD_PERMISSION = "/wiki/rest/api/space/{spaceKey}/permission";	// POST
	private static final String PATH_CLOUD_REMOVE_PERMISSION = "/wiki/rest/api/space/{spaceKey}/permission/{id}";	// DELETE
	
	@SuppressWarnings("rawtypes")
	public static <T extends Enum> String getEnumOptions(Class<T> enumClass, String descriptionMethod) {
		StringBuilder sb = new StringBuilder();
		try {
			Method valuesMethod = enumClass.getMethod("values");
			Method descMethod = null;
			if (descriptionMethod != null) {
				descMethod = enumClass.getMethod(descriptionMethod);
			}
			@SuppressWarnings("unchecked")
			T[] list = (T[]) valuesMethod.invoke(null);
			for (T item : list) {
				sb.append("\r\n").append(item.name());
				if (descMethod != null) {
					sb.append(" - ").append(descMethod.invoke(item));
				}
			}
		} catch (Exception ex) {
			Log.error(LOGGER, "Failed to get list of enum values", ex);
		}
		return sb.toString();
	}
	
	private static Options dcOptions;
	private static Options cloudOptions;
	
	private static Option addUserOption;
	private static Option removeUserOption;
	private static Option addGroupOption;
	private static Option removeGroupOption;	
	private static Option schemeOption;
	private static Option hostOption;
	private static Option adminOption;
	private static Option passwordOption;
	private static Option spaceKeyOption;
	private static Option dcPermissionOption;
	private static Option cloudPermissionOption;
	
	static {
		addUserOption = Option.builder()
				.argName("Add user")
				.longOpt("adduser")
				.option("au")
				.hasArgs()
				.desc("Optional. Multiple. For Data Center/Server, specify user email address. For Cloud, specify user account Id")
				.build();
		removeUserOption = Option.builder()
				.argName("Remove user")
				.longOpt("removeuser")
				.option("ru")
				.hasArgs()
				.desc("Optional. Multiple. For Data Center/Server, specify user email address. For Cloud, specify user account Id")
				.build();
		addGroupOption = Option.builder()
				.argName("Add group")
				.longOpt("addgroup")
				.option("ag")
				.hasArgs()
				.desc("Optional. Multiple. Group display name")
				.build();
		removeGroupOption = Option.builder()
				.argName("Remove group")
				.longOpt("removegroup")
				.option("rg")
				.hasArgs()
				.desc("Optional. Multiple. Group display name")
				.build();
		
		schemeOption = Option.builder()
				.argName("Scheme")
				.longOpt("scheme")
				.option("s")
				.hasArg().numberOfArgs(1)
				.desc("Scheme, e.g. https or http. Default is https")
				.build();
		hostOption = Option.builder()
				.argName("Confluence Host")
				.required()
				.longOpt("host")
				.option("h")
				.hasArg().numberOfArgs(1)
				.desc("Required. Confluence host, e.g. localhost:8090 or my.atlassian.net")
				.build();
		
		adminOption = Option.builder()
				.argName("Administrator")
				.required()
				.longOpt("user")
				.option("u")
				.hasArg().numberOfArgs(1)
				.desc(	"For Data Center/Server, administrator user name\n" + 
						"For Cloud, administrator email")
				.build();
		
		passwordOption = Option.builder()
				.argName("Password/API token")
				.longOpt("password")
				.option("w")
				.hasArg().numberOfArgs(1)
				.desc(	"For Data Center/Server, administrator password\n" + 
						"For Cloud, administrator's API token") 
				.build();
		
		spaceKeyOption = Option.builder()
				.argName("Space key")
				.required()
				.longOpt("spacekey")
				.option("k")
				.hasArgs()
				.desc("Required. Multiple. Space key, e.g. HOME. Specify a single item % for all spaces")
				.build();
		
		dcPermissionOption = Option.builder()
				.argName("Permission")
				.required()
				.longOpt("permission")
				.option("p")
				.hasArgs()
				.desc(	"Required. Multiple. \n" + 
						"Data Center/Server permission: " + getEnumOptions(DCSpacePermission.class, "getDescription"))
				.build();

		cloudPermissionOption = Option.builder()
				.argName("Permission")
				.required()
				.longOpt("permission")
				.option("p")
				.hasArgs()
				.desc(	"Required. Multiple. \n" + 
						"Cloud permission: " + getEnumOptions(CloudSpacePermission.class, "getDescription"))
				.build();

		dcOptions = new Options();
		dcOptions.addOption(Option.builder()
				.argName("Data Center/Server mode")
				.required()
				.longOpt("dc")
				.option("d")
				.desc("Data Center/Server mode")
				.build());
		dcOptions.addOption(schemeOption);
		dcOptions.addOption(hostOption);
		dcOptions.addOption(adminOption);
		dcOptions.addOption(passwordOption);
		dcOptions.addOption(spaceKeyOption);
		dcOptions.addOption(dcPermissionOption);
		dcOptions.addOption(addUserOption);
		dcOptions.addOption(removeUserOption);
		dcOptions.addOption(addGroupOption);
		dcOptions.addOption(removeGroupOption);
		
		cloudOptions = new Options();
		cloudOptions.addOption(Option.builder()
				.argName("Cloud mode")
				.required()
				.longOpt("cloud")
				.option("c")
				.desc("Cloud mode")
				.build());
		cloudOptions.addOption(schemeOption);
		cloudOptions.addOption(hostOption);		
		cloudOptions.addOption(adminOption);
		cloudOptions.addOption(passwordOption);
		cloudOptions.addOption(spaceKeyOption);
		cloudOptions.addOption(cloudPermissionOption);
		cloudOptions.addOption(addUserOption);
		cloudOptions.addOption(removeUserOption);
		cloudOptions.addOption(addGroupOption);
		cloudOptions.addOption(removeGroupOption);
	}
	
	private static void logError(boolean add, Object obj, String msg) {
		Log.error(LOGGER, ((add)? "Failed to add " : "Failed to remove ") + obj.toString() + ": " + msg);
	}
	
	private static boolean logResult(boolean add, Object obj, Response resp) {
		if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
			Log.info(LOGGER, ((add)? "Added " : "Removed ") + obj.toString());
			return true;
		} else {
			String msg = null;
			try {
				msg = resp.getStatus() + " " + resp.readEntity(String.class);
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to read web request response", ex);
			}
			Log.error(LOGGER, ((add)? "Failed to add " : "Failed to remove ") + obj.toString() + ": " + msg);
			return false;
		}
	}
	
	private static boolean processDataCenter(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(dcOptions, args, true);
			String scheme = cmd.getOptionValue(schemeOption, DEFAULT_SCHEME);
			String host = cmd.getOptionValue(hostOption);
			String admin = cmd.getOptionValue(adminOption);
			// Get password 
			String password = null;
			if (cmd.hasOption(passwordOption)) {
				password = cmd.getOptionValue(passwordOption);
			} else {
				password = new String(Console.readPassword("Administrator API token: "));
			}
			String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
			if (spaceKeys.length == 1 && ALL_SPACES.equals(spaceKeys[0])) {
				Log.info(LOGGER, "Wildcard space found, all spaces will be processed");
				// Get all spaces
				try {
					Response resp = WebRequest.invoke(
							scheme, host, PATH_DC_GET_SPACES, null, 
							HttpMethod.POST, admin, password, null, null, null, null);
					if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
						List<SpaceObject> soList = resp.readEntity(new GenericType<List<SpaceObject>>() {});
						List<String> list = new ArrayList<>();
						for (SpaceObject so : soList) {
							list.add(so.getKey());
							Log.info(LOGGER, "Space found: " + so.getKey());
						}
						spaceKeys = list.toArray(new String[0]);
					} else {
						Log.error(LOGGER, 
							"Failed to retrieve space list: " + resp.getStatus());
						return true;
					}
				} catch (Exception ex) {
					Log.error(LOGGER, "Failed to retrieve space list", ex);
					return true;
				}
			}
			String[] permissions = cmd.getOptionValues(dcPermissionOption);
			List<DCSpacePermission> permissionList = new ArrayList<>();
			for (String permission : permissions) {
				try {
					DCSpacePermission parsed = DCSpacePermission.valueOf(permission);
					permissionList.add(parsed);
				} catch (Exception ex) {
					Log.error(LOGGER, "Invalid permission ignored: " + permission);
				}
			}
			String[] addUsers = cmd.getOptionValues(addUserOption);
			String[] removeUsers = cmd.getOptionValues(removeUserOption);
			String[] addGroups = cmd.getOptionValues(addGroupOption);
			String[] removeGroups = cmd.getOptionValues(removeGroupOption);
			// For each space
			for (String spaceKey : spaceKeys) {
				// For each permission
				for (String permission : permissions) {
					// For each action & target
					if (addUsers != null) {
						for (String addUser : addUsers) {
							total++;
							DCDataObject obj = new DCDataObject(spaceKey, permission, addUser);
							try {
								Response resp = WebRequest.invoke(
										scheme, host, PATH_DC_ADD_SPACE_PERMISSION, null,
										HttpMethod.POST, admin, password, null, null, null, obj.format());
								if (logResult(true, obj, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(true, obj, ex.getMessage());
							}
						}
					}
					if (removeUsers != null) {
						for (String removeUser : removeUsers) {
							total++;
							DCDataObject obj = new DCDataObject(spaceKey, permission, removeUser);
							try {
								Response resp = WebRequest.invoke(
										scheme, host, PATH_DC_REMOVE_SPACE_PERMISSION, null, 
										HttpMethod.POST, admin, password, null, null, null, obj.format());
								if (logResult(false, obj, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(false, obj, ex.getMessage());
							}
						}
					}
					if (addGroups != null) {
						for (String addGroup : addGroups) {
							total++;
							DCDataObject obj = new DCDataObject(spaceKey, permission, addGroup);
							try {
								Response resp = WebRequest.invoke(
										scheme, host, PATH_DC_ADD_SPACE_PERMISSION, null, 
										HttpMethod.POST, admin, password, null, null, null, obj.format());
								if (logResult(true, obj, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(true, obj, ex.getMessage());
							}
						}
					}
					if (removeGroups != null) {
						for (String removeGroup : removeGroups) {
							total++;
							DCDataObject obj = new DCDataObject(spaceKey, permission, removeGroup);
							try {
								Response resp = WebRequest.invoke(
										scheme, host, PATH_DC_REMOVE_SPACE_PERMISSION, null, 
										HttpMethod.POST, admin, password, null, null, null, obj.format());
								if (logResult(false, obj, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(false, obj, ex.getMessage());
							}
						}	
					}
					// for each action & target
				}	// for each permission
			}	// For each space key
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		Log.info(LOGGER, "Success/total: " + success + "/" + total);
		return true;
	}
	
	private static boolean processCloud(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(cloudOptions, args, true);
			String scheme = cmd.getOptionValue(schemeOption, DEFAULT_SCHEME);
			String host = cmd.getOptionValue(hostOption);
			String admin = cmd.getOptionValue(adminOption);
			// Get api token
			String password = null;
			if (cmd.hasOption(passwordOption)) {
				password = cmd.getOptionValue(passwordOption);
			} else {
				password = new String(Console.readPassword("Administrator API token: "));
			}
			String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
			// Get all spaces
			Map<String, String> spaceKeysToSpaceId = new HashMap<>();
			try {
				Map<String, Object> query = new HashMap<>();
				List<SpaceObject> spaceObjects = WebRequest.fetchObjects(
						scheme, host, PATH_CLOUD_GET_SPACES, null, 
						HttpMethod.GET, admin, password, null, null, query, null, SpaceObjects.class);
				for (SpaceObject so : spaceObjects) {
					spaceKeysToSpaceId.put(so.getKey(), so.getId());
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve space list", ex);
				return true;
			}
			if (spaceKeys.length == 1 && ALL_SPACES.equals(spaceKeys[0])) {
				Log.info(LOGGER, "Wildcard space found, all spaces will be processed");
				for (Map.Entry<String, String> entry : spaceKeysToSpaceId.entrySet()) {
					Log.info(LOGGER, "Space found: " + entry.getKey() + " = " + entry.getValue());
				}
				spaceKeys = spaceKeysToSpaceId.keySet().toArray(new String[0]);
			}
			List<CloudSpacePermission> permissionList = new ArrayList<>();
			String[] permissions = cmd.getOptionValues(dcPermissionOption);
			for (String permission : permissions) {
				try {
					CloudSpacePermission parsed = CloudSpacePermission.valueOf(permission);
					permissionList.add(parsed);
				} catch (Exception ex) {
					Log.error(LOGGER, "Invalid permission ignored: " + permission);
				}
			}
			String[] addUsers = cmd.getOptionValues(addUserOption);
			String[] removeUsers = cmd.getOptionValues(removeUserOption);
			String[] addGroups = cmd.getOptionValues(addGroupOption);
			String[] removeGroups = cmd.getOptionValues(removeGroupOption);
			// For each space
			for (String spaceKey : spaceKeys) {
				List<GetPermission> permissionObjectList = new ArrayList<>();
				if (removeUsers != null || removeGroups != null) {
					Map<String, String> pathMap = new HashMap<>();
					pathMap.put("id", spaceKeysToSpaceId.get(spaceKey));
					try {
						permissionObjectList = WebRequest.fetchObjects(
							scheme, host, PATH_CLOUD_GET_PERMISSION_ID, pathMap, 
							HttpMethod.GET, admin, password, null, null, null, null, GetPermissions.class);
					} catch (Exception ex) {
						Log.error(LOGGER, "Unable to retrieve permission list", ex);
						continue;
					}
				}
				// For each permission
				for (CloudSpacePermission permission : permissionList) {
					// For each action & target
					if (addUsers != null) {
						for (String addUser : addUsers) {
							total++;
							String data = "Target: " + addUser + 
									" Permission: " + permission.getTarget() + ":" + permission.getKey() + 
									" Space: " + spaceKey;
							AddPermission obj = new AddPermission();
							Operation operation = new Operation();
							operation.setKey(permission.getKey());
							operation.setTarget(permission.getTarget());
							obj.setOperation(operation);
							Subject subject = new Subject();
							subject.setIdentifier(addUser);
							subject.setType(Subject.SUBJECT_TYPE_USER);
							obj.setSubject(subject);
							try {
								Map<String, String> pathMap = new HashMap<>();
								pathMap.put("spaceKey", spaceKey);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_ADD_PERMISSION, pathMap,
										HttpMethod.POST, admin, password, null, null, null, obj);
								if (logResult(true, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(true, data, ex.getMessage());
							}
						}
					}
					if (removeUsers != null) {
						for (String removeUser : removeUsers) {
							total++;
							String data = "Target: " + removeUser + 
									" Permission: " + permission.getTarget() + ":" + permission.getKey() + 
									" Space: " + spaceKey;
							// Get the permission ids and locate a match
							String permissionId = null;
							for (GetPermission item : permissionObjectList) {
//								Log.debug(LOGGER, "Found permission: " + 
//										item.getOperation().getKey() + " -> " + item.getOperation().getTargetType() + " for " + 
//										item.getPrincipal().getType() + ": " + item.getPrincipal().getId()
//										);									
								if (removeUser.equals(item.getPrincipal().getId()) && 
									Principal.TYPE_USER.equals(item.getPrincipal().getType()) &&
									permission.getTarget().equals(item.getOperation().getTargetType()) &&
									permission.getKey().equals(item.getOperation().getKey())) {
									permissionId = item.getId();
									break;
								}
							}
							if (permissionId != null) {
								// Remove it
								Map<String, String> removePathMap = new HashMap<>();
								removePathMap.put("spaceKey", spaceKey);
								removePathMap.put("id", permissionId);
								try {
									Response resp = WebRequest.invoke(
											scheme, host, PATH_CLOUD_REMOVE_PERMISSION, removePathMap, 
											HttpMethod.DELETE, admin, password, null, null, null, null);
									if (logResult(false, data, resp)) {
										success++;
									}
								} catch (Exception ex) {
									logError(false, data, ex.getMessage());
								}
							} else {
								logError(false, data, "Permission not found");
							}
						}
					}
					if (addGroups != null) {
						for (String addGroup : addGroups) {
							total++;
							String data = "Target: " + addGroup + 
									" Permission: " + permission.getTarget() + ":" + permission.getKey() + 
									" Space: " + spaceKey;
							AddPermission obj = new AddPermission();
							Operation operation = new Operation();
							operation.setKey(permission.getKey());
							operation.setTarget(permission.getTarget());
							obj.setOperation(operation);
							Subject subject = new Subject();
							subject.setIdentifier(addGroup);
							subject.setType(Subject.SUBJECT_TYPE_GROUP);
							obj.setSubject(subject);
							try {
								Map<String, String> pathMap = new HashMap<>();
								pathMap.put("spaceKey", spaceKey);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_ADD_PERMISSION, pathMap,
										HttpMethod.POST, admin, password, null, null, null, obj);
								if (logResult(true, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								Log.error(LOGGER, "Error", ex);
								logError(true, data, ex.getMessage());
							}
						}
					}
					if (removeGroups != null) {
						for (String removeGroup : removeGroups) {
							total++;
							String data = "Target: " + removeGroup + 
									" Permission: " + permission.getTarget() + ":" + permission.getKey() + 
									" Space: " + spaceKey;
							// Get the permission ids and locate a match
							String permissionId = null;
							for (GetPermission item : permissionObjectList) {
								if (removeGroup.equals(item.getPrincipal().getId()) && 
									Principal.TYPE_GROUP.equals(item.getPrincipal().getType()) &&
									permission.getTarget().equals(item.getOperation().getTargetType()) &&
									permission.getKey().equals(item.getOperation().getKey())) {
									permissionId = item.getId();
									break;
								}
							}
							if (permissionId != null) {
								// Remove it
								Map<String, String> removePathMap = new HashMap<>();
								removePathMap.put("spaceKey", spaceKey);
								removePathMap.put("id", permissionId);
								try {
									Response resp = WebRequest.invoke(
											scheme, host, PATH_CLOUD_REMOVE_PERMISSION, removePathMap, 
											HttpMethod.DELETE, admin, password, null, null, null, null);
									if (logResult(false, data, resp)) {
										success++;
									}
								} catch (Exception ex) {
									logError(false, data, ex.getMessage());
								}
							} else {
								logError(false, data, "Permission not found");
							}
						}	
					}
					// for each action & target
				}	// for each permission
			}	// For each space key
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		Log.info(LOGGER, "Success/total: " + success + "/" + total);
		return true;
	}
	
	public static void main(String[] args) {
		if (processDataCenter(args)) {
			return;
		}
		if (processCloud(args)) {
			return;
		}
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("Manage space permission for Confluence Data Center/Server", dcOptions);
		hf.printHelp("Manage space permission for Confluence Cloud", cloudOptions);
	}
}
