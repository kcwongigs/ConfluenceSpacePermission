package com.igsl;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.commons.csv.CSVPrinter;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igsl.model.AddPermission;
import com.igsl.model.AddProjectRoleActor;
import com.igsl.model.AddRestriction;
import com.igsl.model.ContentRestriction;
import com.igsl.model.ContentRestrictions;
import com.igsl.model.DCDataObject;
import com.igsl.model.DCPage;
import com.igsl.model.DCPages;
import com.igsl.model.DCRestriction;
import com.igsl.model.DCRestrictionTargetGroup;
import com.igsl.model.DCRestrictionTargetUser;
import com.igsl.model.DCRestrictionTargetUsers;
import com.igsl.model.DCRestrictionWrapper;
import com.igsl.model.DCRestrictions;
import com.igsl.model.DCRestrictionsWrapper;
import com.igsl.model.GetPermission;
import com.igsl.model.GetPermissions;
import com.igsl.model.Operation;
import com.igsl.model.Page;
import com.igsl.model.Pages;
import com.igsl.model.Principal;
import com.igsl.model.Project;
import com.igsl.model.ProjectRole;
import com.igsl.model.Projects;
import com.igsl.model.RestrictionGroup;
import com.igsl.model.RestrictionUser;
import com.igsl.model.SpaceObject;
import com.igsl.model.SpaceObjects;
import com.igsl.model.Subject;

/**
 * Utility to manage space permission for Confluence DC and Cloud.
 */
public class PermissionTool {	
	private static final Logger LOGGER = LogManager.getLogger(PermissionTool.class);
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	private static final String DEFAULT_SCHEME = "https";
	private static final String WILDCARD = "%";
	
	private static final String PATH_DC_ADD_SPACE_PERMISSION = "/rpc/json-rpc/confluenceservice-v2/addPermissionToSpace";
	private static final String PATH_DC_REMOVE_SPACE_PERMISSION = "/rpc/json-rpc/confluenceservice-v2/removePermissionFromSpace";
	private static final String PATH_DC_GET_SPACES = "/rpc/json-rpc/confluenceservice-v2/getSpaces";
	
	private static final String PATH_CLOUD_GET_SPACES = "/wiki/api/v2/spaces";
	private static final String PATH_CLOUD_GET_PERMISSION_ID = "/wiki/api/v2/spaces/{id}/permissions";	// GET
	private static final String PATH_CLOUD_ADD_PERMISSION = "/wiki/rest/api/space/{spaceKey}/permission";	// POST
	private static final String PATH_CLOUD_REMOVE_PERMISSION = "/wiki/rest/api/space/{spaceKey}/permission/{id}";	// DELETE
	
	private static final String PATH_CLOUD_GET_PROJECTS = "/rest/api/2/project/search";	// GET
	private static final String PATH_CLOUD_GET_PROJECT_ROLES = "/rest/api/2/role";
	private static final String PATH_CLOUD_PROJECT_ADD_ACTOR = "/rest/api/2/project/{projectIdOrKey}/role/{id}";	// POST
	private static final String PATH_CLOUD_PROJECT_REMOVE_ACTOR = "/rest/api/2/project/{projectIdOrKey}/role/{id}";	// DELETE
	
	private static final String PATH_CLOUD_GET_PAGES = "/wiki/api/v2/pages";	// GET
	private static final String PATH_CLOUD_RESTRICTION_ADD_GROUP = 
			"/wiki/rest/api/content/{id}/restriction/byOperation/{operationKey}/byGroupId/{groupId}";	// PUT
	private static final String PATH_CLOUD_RESTRICTION_REMOVE_GROUP = 
			"/wiki/rest/api/content/{id}/restriction/byOperation/{operationKey}/byGroupId/{groupId}";	// DELETE
	private static final String PATH_CLOUD_RESTRICTION_ADD_USER = 
			"/wiki/rest/api/content/{id}/restriction/byOperation/{operationKey}/user";	// PUT
	private static final String PATH_CLOUD_RESTRICTION_REMOVE_USER = 
			"/wiki/rest/api/content/{id}/restriction/byOperation/{operationKey}/user";	// DELETE
	private static final String PATH_CLOUD_GET_RESTRICTION = 
			"/wiki/rest/api/content/{id}/restriction";	// GET
	
	private static final String PATH_DC_GET_CONTENT = "/rest/api/content";	// GET
	private static final String PATH_DC_GET_RESTRICTION = "/rest/experimental/content/{id}/restriction";	// GET
	private static final String PATH_DC_SET_RESTRICTION = "/rest/experimental/content/{id}/restriction";	// PUT
	private static final String PATH_DC_GET_USER = "/rest/api/user";	// GET
	private static final String PATH_DC_GET_GROUP = "/rest/api/group/{groupName}";	// GET
	
	private static final String PATH_CLOUD_GET_PAGE_BY_ID = "/wiki/api/v2/pages/{id}";	// GET
	
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
	
	private static Options dcConfluenceOptions;
	private static Options dcContentOptions;
	private static Options cloudConfluenceOptions;
	private static Options cloudJiraOptions;
	private static Options cloudContentOptions;
	private static Options cloudContentListOptions;
	
	private static Option addUserOption;
	private static Option removeUserOption;
	private static Option addGroupOption;
	private static Option removeGroupOption;	
	private static Option schemeOption;
	private static Option hostOption;
	private static Option adminOption;
	private static Option passwordOption;
	private static Option spaceKeyOption;
	private static Option projectKeyOption;
	private static Option projectRoleOption;
	private static Option dcPermissionOption;
	private static Option cloudPermissionOption;
	private static Option contentKeyOption;
	private static Option contentRestrictionOption;
	private static Option dcRestrictionOption;
	
	static {
		contentKeyOption = Option.builder()
				.argName("Content key")
				.required()
				.longOpt("contentkey")
				.option("t")
				.hasArgs()
				.desc("Required. Multiple. Page or attachment id. Specify % for all pages and attachments")
				.build();
		
		contentRestrictionOption = Option.builder()
				.argName("Content restriction")
				.required()
				.longOpt("permission")
				.option("p")
				.hasArgs()
				.desc("Content restriction: " + getEnumOptions(CloudContentRestriction.class, "getDescription"))
				.build();
		
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
				.desc("Optional. Multiple. For Data Center/Server, specify group display name. For Cloud, specify group Id")
				.build();
		removeGroupOption = Option.builder()
				.argName("Remove group")
				.longOpt("removegroup")
				.option("rg")
				.hasArgs()
				.desc("Optional. Multiple. For Data Center/Server, specify group display name. For Cloud, specify group Id")
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
				.desc("Multiple. Space key, e.g. HOME. Specify a single item % for all spaces")
				.build();
		
		projectKeyOption = Option.builder()
				.argName("Project key")
				.required()
				.longOpt("projectkey")
				.option("k")
				.hasArgs()
				.desc("Multiple. Project key, e.g. PROJ-1. Specify a single item % for all projects")
				.build();
		
		projectRoleOption = Option.builder()
				.argName("Permission")
				.required()
				.longOpt("permission")
				.option("p")
				.hasArgs()
				.desc(	"Required. Multiple. \n" + 
						"Cloud project role: " + getEnumOptions(CloudJiraProjectRole.class, "getDescription"))
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
		
		dcRestrictionOption = Option.builder()
				.argName("Restriction")
				.required()
				.longOpt("permission")
				.option("p")
				.hasArgs()
				.desc(	"Required. Multiple. \n" + 
						"Restriction: " + getEnumOptions(DCContentRestriction.class, null))
				.build();

		dcConfluenceOptions = new Options();
		dcConfluenceOptions.addOption(Option.builder()
				.argName("Data Center/Server mode")
				.required()
				.longOpt("dcconfluence")
				.option("dc")
				.desc("Data Center/Server mode")
				.build());
		dcConfluenceOptions.addOption(schemeOption);
		dcConfluenceOptions.addOption(hostOption);
		dcConfluenceOptions.addOption(adminOption);
		dcConfluenceOptions.addOption(passwordOption);
		dcConfluenceOptions.addOption(spaceKeyOption);
		dcConfluenceOptions.addOption(dcPermissionOption);
		dcConfluenceOptions.addOption(addUserOption);
		dcConfluenceOptions.addOption(removeUserOption);
		dcConfluenceOptions.addOption(addGroupOption);
		dcConfluenceOptions.addOption(removeGroupOption);
		
		dcContentOptions = new Options();
		dcContentOptions.addOption(Option.builder()
				.argName("Data Center/Server content restriction")
				.required()
				.longOpt("dccontent")
				.option("do")
				.desc("Data Center/Server content restriction. Specify username in -au and -ru options.")
				.build());
		dcContentOptions.addOption(schemeOption);
		dcContentOptions.addOption(hostOption);
		dcContentOptions.addOption(adminOption);
		dcContentOptions.addOption(passwordOption);
		dcContentOptions.addOption(spaceKeyOption);
		dcContentOptions.addOption(contentKeyOption);
		dcContentOptions.addOption(dcRestrictionOption);
		dcContentOptions.addOption(addUserOption);
		dcContentOptions.addOption(removeUserOption);
		dcContentOptions.addOption(addGroupOption);
		dcContentOptions.addOption(removeGroupOption);
		
		cloudConfluenceOptions = new Options();
		cloudConfluenceOptions.addOption(Option.builder()
				.argName("Cloud Confluence mode")
				.required()
				.longOpt("cloudconfluence")
				.option("cc")
				.desc("Cloud Confluence mode")
				.build());
		cloudConfluenceOptions.addOption(schemeOption);
		cloudConfluenceOptions.addOption(hostOption);		
		cloudConfluenceOptions.addOption(adminOption);
		cloudConfluenceOptions.addOption(passwordOption);
		cloudConfluenceOptions.addOption(spaceKeyOption);
		cloudConfluenceOptions.addOption(cloudPermissionOption);
		cloudConfluenceOptions.addOption(addUserOption);
		cloudConfluenceOptions.addOption(removeUserOption);
		cloudConfluenceOptions.addOption(addGroupOption);
		cloudConfluenceOptions.addOption(removeGroupOption);
		
		cloudJiraOptions = new Options();
		cloudJiraOptions.addOption(Option.builder()
				.argName("Cloud Jira mode")
				.required()
				.longOpt("cloudjira")
				.option("cj")
				.desc("Cloud Jira mode")
				.build());
		cloudJiraOptions.addOption(schemeOption);
		cloudJiraOptions.addOption(hostOption);		
		cloudJiraOptions.addOption(adminOption);
		cloudJiraOptions.addOption(passwordOption);
		cloudJiraOptions.addOption(projectKeyOption);
		cloudJiraOptions.addOption(projectRoleOption);
		cloudJiraOptions.addOption(addUserOption);
		cloudJiraOptions.addOption(removeUserOption);
		cloudJiraOptions.addOption(addGroupOption);
		cloudJiraOptions.addOption(removeGroupOption);
		
		cloudContentOptions = new Options();
		cloudContentOptions.addOption(Option.builder()
				.argName("Cloud Confluence content restriction Mode")
				.required()
				.longOpt("content")
				.option("co")
				.desc("Cloud Confluence content restriction")
				.build());
		cloudContentOptions.addOption(schemeOption);
		cloudContentOptions.addOption(hostOption);		
		cloudContentOptions.addOption(adminOption);
		cloudContentOptions.addOption(passwordOption);
		cloudContentOptions.addOption(spaceKeyOption);
		cloudContentOptions.addOption(contentKeyOption);
		cloudContentOptions.addOption(contentRestrictionOption);
		cloudContentOptions.addOption(addUserOption);
		cloudContentOptions.addOption(removeUserOption);
		cloudContentOptions.addOption(addGroupOption);
		cloudContentOptions.addOption(removeGroupOption);
		
		cloudContentListOptions = new Options();
		cloudContentListOptions.addOption(Option.builder()
				.argName("Cloud Confluence content restriction list Mode")
				.required()
				.longOpt("contentlist")
				.option("cl")
				.desc("Cloud Confluence content restriction list")
				.build());
		cloudContentListOptions.addOption(schemeOption);
		cloudContentListOptions.addOption(hostOption);		
		cloudContentListOptions.addOption(adminOption);
		cloudContentListOptions.addOption(passwordOption);
		cloudContentListOptions.addOption(spaceKeyOption);
		cloudContentListOptions.addOption(contentKeyOption);
	}
	
	private static void logError(boolean add, Object obj, String msg) {
		Log.error(LOGGER, ((add)? "Failed to add " : "Failed to remove ") + obj.toString() + ": " + msg);
	}
	
	private static void logSkip(boolean add, Object obj, String msg) {
		Log.error(LOGGER, "Skipped " + ((add)? "adding " : "removing ") + obj.toString() + ": " + msg);
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
	
	private static boolean processDataCenterContent(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(dcContentOptions, args, true);
			String scheme = cmd.getOptionValue(schemeOption, DEFAULT_SCHEME);
			String host = cmd.getOptionValue(hostOption);
			String admin = cmd.getOptionValue(adminOption);
			// Get password 
			String password = null;
			if (cmd.hasOption(passwordOption)) {
				password = cmd.getOptionValue(passwordOption);
			} else {
				password = new String(Console.readPassword("Password: "));
			}
			// Content IDs
			String[] contentIds = cmd.getOptionValues(contentKeyOption);
			if (contentIds != null && contentIds.length == 1 && WILDCARD.equals(contentIds[0])) {
				// Find contents from space keys
				String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
				if (spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0])) {
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
				// Get contents
				List<String> contentIdList = new ArrayList<>();
				for (String spaceKey : spaceKeys) {
					try {
						Map<String, Object> query = new HashMap<>();
						query.put("spaceKey", spaceKey);
						List<DCPage> pages = WebRequest.fetchObjectsWithStartAt(
								scheme, host, PATH_DC_GET_CONTENT, null, 
								HttpMethod.GET, admin, password, null, null, query, "start", null, DCPages.class);
						for (DCPage page : pages) {
							contentIdList.add(page.getId());
							Log.info(LOGGER, 
									"Page found: " + page.getTitle() + " = " + page.getId() + " in space " + spaceKey);
						}
					} catch (Exception ex) {
						Log.error(LOGGER, "Failed to retrieve content list from space " + spaceKey, ex);
						return true;
					}
				}
				contentIds = contentIdList.toArray(new String[0]);
			} 
			// Restriction
			String[] restrictions = cmd.getOptionValues(dcRestrictionOption);
			List<DCContentRestriction> restrictionList = new ArrayList<>();
			for (String restriction : restrictions) {
				try {
					DCContentRestriction parsed = DCContentRestriction.valueOf(restriction);
					restrictionList.add(parsed);
				} catch (Exception ex) {
					Log.error(LOGGER, "Invalid permission ignored: " + restriction);
				}
			}
			String[] addUsers = cmd.getOptionValues(addUserOption);
			String[] removeUsers = cmd.getOptionValues(removeUserOption);
			String[] addGroups = cmd.getOptionValues(addGroupOption);
			String[] removeGroups = cmd.getOptionValues(removeGroupOption);
			for (String contentId : contentIds) {
				// Fetch original restrictions
				DCRestrictionsWrapper originalRestrictions = new DCRestrictionsWrapper();
				try {
					Map<String, String> replacements = new HashMap<>();
					replacements.put("id", contentId);
					// For get restriction, the start parameter is for nested objects
					// So the PagedWithNumer interface won't work with it
					// Instead we do it by translating the structure to a flatter design, 
					// and repeat calls until both users and groups come back at 0 item.
					boolean hasMore = true;
					int startAt = 0;
					while (hasMore) {
						Map<String, Object> query = new HashMap<>();
						query.put("start", startAt);
						Response resp = WebRequest.invoke(
								scheme, host, PATH_DC_GET_RESTRICTION, replacements, 
								HttpMethod.GET, admin, password, null, null, query, null);
						if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
							DCRestrictions res = resp.readEntity(DCRestrictions.class);
							int count = originalRestrictions.addData(res);
							hasMore = count != 0;
							startAt += count;
						} else {
							throw new Exception("Failed to retrieve original restrictions for " + contentId + ": " + 
									resp.readEntity(String.class));
						}						
					}					
				} catch (Exception ex) {
					Log.error(LOGGER, "Failed to retrieve original restrictions for " + contentId, ex);
					continue;
				}
				Log.debug(LOGGER, "Content: " + contentId + " Original restrictions: " +  
						new ObjectMapper().writeValueAsString(originalRestrictions.convert()));
				// Modify restrictions
				int pending = 0;
				for (DCContentRestriction permission : restrictionList) {
					if (originalRestrictions.getUserCount(permission.name()) == 0 && 
						originalRestrictions.getGroupCount(permission.name()) == 0) {
						Log.info(LOGGER, 
								"Content: " + contentId + 
								" Restriction: " + permission.name() + 
								" is not restricted. Actions will not be performed.");
					} else {
						total += 
								((addUsers != null)? addUsers.length : 0) + 
								((removeUsers != null)? removeUsers.length : 0) + 
								((addGroups != null)? addGroups.length : 0) + 
								((removeGroups != null)? removeGroups.length : 0);
						if (addUsers != null) {
							for (String addUser : addUsers) {
								// Check if it already exists
								if (!originalRestrictions.hasUser(permission.name(), addUser)) {
									if (validateUser(addUser, scheme, host, admin, password)) {
										originalRestrictions.addUser(permission.name(), addUser);
										Log.info(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" User: " + addUser + 
												" will be added");
										pending++;
									} else {
										Log.error(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" User: " + addUser + 
												" is invalid, it will be not be added");
									}
								} else {
									Log.info(LOGGER, 
											"Content: " + contentId + 
											" Restriction: " + permission.name() + 
											" User: " + addUser + 
											" already added");
									success++;
								}
							}
						}
						if (removeUsers != null) {
							for (String removeUser : removeUsers) {
								// Check if it exists
								if (originalRestrictions.hasUser(permission.name(), removeUser)) {
									if (validateUser(removeUser, scheme, host, admin, password)) {
										originalRestrictions.removeUser(permission.name(), removeUser);
										Log.info(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" User: " + removeUser + 
												" will be removed");
										pending++;
									} else {
										Log.error(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" User: " + removeUser + 
												" is invalid, it will not be removed");
									}
								} else {
									Log.info(LOGGER, 
											"Content: " + contentId + 
											" Restriction: " + permission.name() + 
											" User: " + removeUser + 
											" already removed");
									success++;
								}
							}
						}
						if (addGroups != null) {
							for (String addGroup : addGroups) {
								// Check if it already exists
								if (!originalRestrictions.hasGroup(permission.name(), addGroup)) {
									if (validateGroup(addGroup, scheme, host, admin, password)) {
										originalRestrictions.addGroup(permission.name(), addGroup);
										Log.info(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" Group: " + addGroup + 
												" will be added");
										pending++;
									} else {
										Log.error(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" Group: " + addGroup + 
												" is invalid, it will not be added");
									}
								} else {
									Log.info(LOGGER, 
											"Content: " + contentId + 
											" Restriction: " + permission.name() + 
											" Group: " + addGroup + 
											" already added");
									success++;
								}
							}
						}
						if (removeGroups != null) {
							for (String removeGroup : removeGroups) {
								// Check if it exists
								if (originalRestrictions.hasGroup(permission.name(), removeGroup)) {
									if (validateGroup(removeGroup, scheme, host, admin, password)) {
										originalRestrictions.removeGroup(permission.name(), removeGroup);
										Log.info(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" Group: " + removeGroup + 
												" will be removed");
										pending++;
									} else {
										Log.error(LOGGER, 
												"Content: " + contentId + 
												" Restriction: " + permission.name() + 
												" Group: " + removeGroup + 
												" is invalid, it will not be removed");
									}
								} else {
									Log.info(LOGGER, 
											"Content: " + contentId + 
											" Restriction: " + permission.name() + 
											" Group: " + removeGroup + 
											" already removed");
									success++;
								}							
							}
						}
					}	// If there are restrictions defined
				}	// For each permission
				
				// Check that remaining users/groups are valid, if not, remove them and print warning
				Set<String> removeInvalidUsers = new HashSet<String>();
				Set<String> removeInvalidGroups = new HashSet<String>();
				for (String permission : originalRestrictions.getPermissions()) {
					// Users
					for (String user : originalRestrictions.getUsers(permission)) {
						if (!validateUser(user, scheme, host, admin, password)) {
							removeInvalidUsers.add(user);
						}
					}
					// Groups
					for (String group : originalRestrictions.getGroups(permission)) {
						if (!validateGroup(group, scheme, host, admin, password)) {
							removeInvalidGroups.add(group);
						}
					}
				}
				// Remove from restrictions
				for (String permission : originalRestrictions.getPermissions()) {
					for (String user : removeInvalidUsers) {
						Log.info(LOGGER, "Removing existing invalid user: " + user + " for permission: " + permission);
						originalRestrictions.removeUser(permission, user);
						pending++;
						total++;
					}
					for (String group : removeInvalidGroups) {
						Log.info(LOGGER, "Removing existing invalid group: " + group + " for permission: " + permission);
						originalRestrictions.removeGroup(permission, group);
						pending++;
						total++;
					}
				}	
				// Update restrictions
				if (pending != 0) {
					Map<String, String> replacements = new HashMap<>();
					replacements.put("id", contentId);
					try {
						Log.debug(LOGGER, "Content: " + contentId + " New Restrictions: " +  
								new ObjectMapper().writeValueAsString(originalRestrictions.convert()));
						Response resp = WebRequest.invoke(
								scheme, host, PATH_DC_SET_RESTRICTION, replacements, 
								HttpMethod.PUT, admin, password, null, null, null, originalRestrictions.convert());
						if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
							Log.info(LOGGER, "Content: " + contentId + " updated " + pending + " item(s)");
							success += pending;
						} else {
							// Fail for all items
							Log.error(LOGGER, 
									"Content: " + contentId + " failed to update " + pending + " item(s): " + 
									resp.readEntity(String.class));
						}
					} catch (Exception ex) {
						Log.error(LOGGER, 
								"Content: " + contentId + " failed to update " + pending + " item(s): ", 
								ex);
					}
				} else {
					Log.info(LOGGER, "Content: " + contentId + " no updates required");
				}
			}	// For each content id
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		Log.info(LOGGER, "Success/total: " + success + "/" + total);
		return true;
	}
	
	private static boolean processDataCenterConfluence(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(dcConfluenceOptions, args, true);
			String scheme = cmd.getOptionValue(schemeOption, DEFAULT_SCHEME);
			String host = cmd.getOptionValue(hostOption);
			String admin = cmd.getOptionValue(adminOption);
			// Get password 
			String password = null;
			if (cmd.hasOption(passwordOption)) {
				password = cmd.getOptionValue(passwordOption);
			} else {
				password = new String(Console.readPassword("Password: "));
			}
			String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
			if (spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0])) {
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
	
	private static boolean processCloudJira(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(cloudJiraOptions, args, true);
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
			String[] projectKeys = cmd.getOptionValues(projectKeyOption);
			if (projectKeys.length == 1 && WILDCARD.equals(projectKeys[0])) {
				Log.info(LOGGER, "Wildcard project found, all projects will be processed");
				// Get all projects
				try {
					Map<String, Object> query = new HashMap<>();
					List<Project> projects = WebRequest.fetchObjectsWithStartAt(
							scheme, host, PATH_CLOUD_GET_PROJECTS, null, 
							HttpMethod.GET, admin, password, null, null, query, "startAt", null, Projects.class);
					List<String> result = new ArrayList<>();
					for (Project p : projects) {
						result.add(p.getKey());
						Log.info(LOGGER, "Project found: " + p.getKey() + " = " + p.getName());
					}
					projectKeys = result.toArray(new String[0]);
				} catch (Exception ex) {
					Log.error(LOGGER, "Failed to retrieve project list", ex);
					return true;
				}
			}
			// Get role parameter
			List<String> roleList = new ArrayList<>();
			String[] roles = cmd.getOptionValues(projectRoleOption);
			for (String role : roles) {
				CloudJiraProjectRole v = CloudJiraProjectRole.valueOf(role);
				if (v != null) {
					roleList.add(v.getDescription());
				} else {
					Log.error(LOGGER, "Invalid project role ignored: " + role);
				}
			}	
			// Translate to role IDs
			Map<String, String> roleIdList = new HashMap<>();
			try {
				Map<String, Object> query = new HashMap<>();
				Response resp = WebRequest.invoke(
						scheme, host, PATH_CLOUD_GET_PROJECT_ROLES, null, 
						HttpMethod.GET, admin, password, null, null, query, null);
				if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
					List<ProjectRole> rolesFound = resp.readEntity(new GenericType<List<ProjectRole>>(){});
					for (String requiredRole : roleList) {
						boolean roleFound = false;
						for (ProjectRole role : rolesFound) {
							if (requiredRole.equals(role.getName())) {
								Log.debug(LOGGER, "Role " + role.getName() + " = " + role.getId());
								roleIdList.put(role.getId(), role.getName());
								roleFound = true;
								break;
							}
						}
						if (!roleFound) {
							Log.error(LOGGER, "Role ID not found for role " + requiredRole + ", role ignored");
						}
					}
				} else {
					throw new Exception(resp.readEntity(String.class));
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to role ID", ex);
				return true;
			}				
			if (roleIdList.size() == 0) {
				Log.error(LOGGER, "No role ID found, cannot proceed");
				return true;
			}
			String[] addUsers = cmd.getOptionValues(addUserOption);
			String[] removeUsers = cmd.getOptionValues(removeUserOption);
			String[] addGroups = cmd.getOptionValues(addGroupOption);
			String[] removeGroups = cmd.getOptionValues(removeGroupOption);
			for (String projectKey : projectKeys) {
				for (Map.Entry<String, String> role : roleIdList.entrySet()) {
					Map<String, String> pathReplacement = new HashMap<>();
					pathReplacement.put("projectIdOrKey", projectKey);
					pathReplacement.put("id", role.getKey());
					AddProjectRoleActor add = new AddProjectRoleActor(projectKey, role.getValue());
					if (addUsers != null) {
						add.setUser(Arrays.asList(addUsers));
						total += addUsers.length;
					}
					if (addGroups != null) {
						add.setGroupId(Arrays.asList(addGroups));
						total += addGroups.length;
					}
					if (add.getGroupId().size() != 0 || add.getUser().size() != 0) {
						try {
							Response resp = WebRequest.invoke(
									scheme, host, PATH_CLOUD_PROJECT_ADD_ACTOR, pathReplacement, 
									HttpMethod.POST, admin, password, null, null, null, add);
							if (logResult(true, add, resp)) {
								success += add.getGroupId().size() + add.getUser().size();
							}
						} catch (Exception ex) {
							logError(true, add, ex.getMessage());
						}
					}
					AddProjectRoleActor remove = new AddProjectRoleActor(projectKey, role.getValue());
					if (removeUsers != null) {
						remove.setUser(Arrays.asList(removeUsers));
						total += removeUsers.length;
					}
					if (removeGroups != null) {
						remove.setGroupId(Arrays.asList(removeGroups));
						total += removeGroups.length;
					}
					if (remove.getGroupId().size() != 0 || remove.getUser().size() != 0) {
						for (String user: remove.getUser()) {
							try {
								Map<String, Object> query = new HashMap<>();
								query.put("user", user);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_PROJECT_REMOVE_ACTOR, pathReplacement, 
										HttpMethod.DELETE, admin, password, null, null, query, null);
								if (logResult(false, remove, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(false, remove, ex.getMessage());
							}
						}
						for (String group : remove.getGroupId()) {
							try {
								Map<String, Object> query = new HashMap<>();
								query.put("groupId", group);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_PROJECT_REMOVE_ACTOR, pathReplacement, 
										HttpMethod.DELETE, admin, password, null, null, query, null);
								if (logResult(false, remove, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(false, remove, ex.getMessage());
							}
						}
					}
				}	// For each roleId
			}	// For each projectKey
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		Log.info(LOGGER, "Success/total: " + success + "/" + total);
		return true;
	}
	
	private static boolean processCloudConfluence(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(cloudConfluenceOptions, args, true);
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
			// Get related spaces
			Map<String, String> spaceKeysToSpaceId = new HashMap<>();
			Map<String, Object> query = new HashMap<>();
			if (spaceKeys != null && !(spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0]))) {
				// Get only specified spaces
				StringBuilder sb = new StringBuilder();
				for (String spaceKey : spaceKeys) {
					sb.append(",").append(spaceKey);
				}
				if (sb.length() != 0) {
					query.put("keys", sb.toString().substring(1));
				}
			}
			try {
				List<SpaceObject> spaceObjects = WebRequest.fetchObjectsWithCursor(
						scheme, host, PATH_CLOUD_GET_SPACES, null, 
						HttpMethod.GET, admin, password, null, null, query, null, SpaceObjects.class);
				for (SpaceObject so : spaceObjects) {
					spaceKeysToSpaceId.put(so.getKey(), so.getId());
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve space list", ex);
				return true;
			}
			if (spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0])) {
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
					if (spaceKeysToSpaceId.containsKey(spaceKey)) {
						pathMap.put("id", spaceKeysToSpaceId.get(spaceKey));
					}
					try {
						permissionObjectList = WebRequest.fetchObjectsWithCursor(
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
	
	/**
	 * Note: Implementation is not complete. 
	 * Anonymous and known users (I assume is space owner) are not supported.
	 * Will do nothing if no restrictions exist in the first place.
	 */
	private static boolean processCloudContent(String[] args) {
		int total = 0;
		int success = 0;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(cloudContentOptions, args, true);
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
			List<CloudContentRestriction> restrictionList = new ArrayList<>();
			String[] restrictions = cmd.getOptionValues(contentRestrictionOption);
			for (String restriction : restrictions) {
				CloudContentRestriction v = CloudContentRestriction.valueOf(restriction);
				if (v != null) {
					restrictionList.add(v);
				} else {
					Log.error(LOGGER, "Invalid restriction ignored: " + restriction);
				}
			}
			// Convert space keys to space ids as comma-delimited list
			List<String> spaceIdList = new ArrayList<>();
			String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
			// Get spaces
			try {
				Map<String, Object> query = new HashMap<>();
				if (spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0])) {
					Log.info(LOGGER, "Wildcard space found, all spaces will be processed");
				}
				if (spaceKeys != null && !(spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0]))) {
					// Get only specified spaces
					StringBuilder sb = new StringBuilder();
					for (String spaceKey : spaceKeys) {
						sb.append(",").append(spaceKey);
					}
					if (sb.length() != 0) {
						query.put("keys", sb.toString().substring(1));
					}
				}
				List<SpaceObject> spaceObjects = WebRequest.fetchObjectsWithCursor(
						scheme, host, PATH_CLOUD_GET_SPACES, null, 
						HttpMethod.GET, admin, password, null, null, query, null, SpaceObjects.class);
				for (SpaceObject so : spaceObjects) {
					spaceIdList.add(so.getId());
					Log.info(LOGGER, "Space found: " + so.getKey() + " = " + so.getId());
				}
				if (spaceIdList.size() == 0) {
					Log.error(LOGGER, "No specified space(s) can be found");
					return true;
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve space list", ex);
				return true;
			}
			List<String> contentList = new ArrayList<>();
			String[] contentIds = cmd.getOptionValues(contentKeyOption);
			if (contentIds.length == 1 && WILDCARD.equals(contentIds[0])) {
				// Get all pages, one space at a time
				for (String spaceId : spaceIdList) {
					Map<String, Object> query = new HashMap<>();
					query.put("space-id", spaceId);
					try {
						List<Page> pages = WebRequest.fetchObjectsWithCursor(
							scheme, host, PATH_CLOUD_GET_PAGES, null, 
							HttpMethod.GET, admin, password, null, null, query, null, Pages.class);
						for (Page page : pages) {
							Log.info(LOGGER, 
									"Space: " + spaceId + " Page found: " + page.getTitle() + " = " + page.getId());
							contentList.add(page.getId());
						}
					} catch (Exception ex) {
						Log.error(LOGGER, "Unable to retrieve page list for space " + spaceId, ex);
					}
				}
			} else {
				for (String contentId : contentIds) {
					contentList.add(contentId);
				}
			}
			String[] addUsers = cmd.getOptionValues(addUserOption);
			String[] removeUsers = cmd.getOptionValues(removeUserOption);
			String[] addGroups = cmd.getOptionValues(addGroupOption);
			String[] removeGroups = cmd.getOptionValues(removeGroupOption);
			for (String contentId : contentList) {
				// Check if there is any restriction
				try {
					Map<String, String> replacements = new HashMap<>();
					replacements.put("id", contentId);
					Response resp = WebRequest.invoke(
							scheme, host, PATH_CLOUD_GET_RESTRICTION, replacements, 
							HttpMethod.GET, admin, password, null, null, null, null);
					if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
						ContentRestrictions contentRestrictions = resp.readEntity(ContentRestrictions.class);
						int targetCount = 0;
						for (ContentRestriction contentRestriction : contentRestrictions.getResults()) {
							targetCount += contentRestriction.getRestrictions().getGroup().getResults().size();
							targetCount += contentRestriction.getRestrictions().getUser().getResults().size();							
						}
						if (targetCount == 0) {
							// Nothing defined
							// Skip the actions, print message and count them as success
							if (addUsers != null) {
								for (String addUser: addUsers) { 
									for (CloudContentRestriction restriction : restrictionList) {
										total++;
										success++;
										AddRestriction data = new AddRestriction(
												contentId, restriction.name(), true, addUser);
										logSkip(true, data, "Content is not restricted");
									}
								}
							}
							if (addGroups != null) {
								for (String addGroup: addGroups) { 
									for (CloudContentRestriction restriction : restrictionList) {
										total++;
										success++;
										AddRestriction data = new AddRestriction(
												contentId, restriction.name(), false, addGroup);
										logSkip(true, data, "Content is not restricted");
									}
								}
							}
							if (removeUsers != null) {
								for (String removeUser: removeUsers) { 
									for (CloudContentRestriction restriction : restrictionList) {
										total++;
										success++;
										AddRestriction data = new AddRestriction(
												contentId, restriction.name(), true, removeUser);
										logSkip(false, data, "Content is not restricted");
									}
								}
							}
							if (removeGroups != null) {
								for (String removeGroup: removeGroups) { 
									for (CloudContentRestriction restriction : restrictionList) {
										total++;
										success++;
										AddRestriction data = new AddRestriction(
												contentId, restriction.name(), false, removeGroup);
										logSkip(false, data, "Content is not restricted");
									}
								}
							}
							continue;	// Skip this content ID
						}
					} else {
						Log.error(LOGGER, "Failed to check content restriction", resp.readEntity(String.class));
					}
				} catch (Exception ex) {
					Log.error(LOGGER, "Failed to check content restriction", ex);
				}
				if (addUsers != null) {
					for (String addUser: addUsers) { 
						for (CloudContentRestriction restriction : restrictionList) {
							total++;
							AddRestriction data = new AddRestriction(contentId, restriction.name(), true, addUser);
							try {
								Map<String, String> replacements = new HashMap<>();
								replacements.put("id", contentId);
								replacements.put("operationKey", restriction.name());
								Map<String, Object> query = new HashMap<>();
								query.put("accountId", addUser);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_RESTRICTION_ADD_USER, replacements, 
										HttpMethod.PUT, admin, password, null, null, query, null);
								if (logResult(true, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(true, data, ex.getMessage());
							}
						}
					}
				}
				if (addGroups != null) {
					for (String addGroup : addGroups) { 
						for (CloudContentRestriction restriction : restrictionList) {
							total++;
							AddRestriction data = new AddRestriction(contentId, restriction.name(), false, addGroup);
							try {
								Map<String, String> replacements = new HashMap<>();
								replacements.put("id", contentId);
								replacements.put("operationKey", restriction.name());
								replacements.put("groupId", addGroup);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_RESTRICTION_ADD_GROUP, replacements, 
										HttpMethod.PUT, admin, password, null, null, null, null);
								if (logResult(true, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(true, data, ex.getMessage());
							}
						}
					}
				}
				if (removeUsers != null) {
					for (String removeUser: removeUsers) { 
						for (CloudContentRestriction restriction : restrictionList) {
							total++;
							AddRestriction data = new AddRestriction(contentId, restriction.name(), true, removeUser);
							try {
								Map<String, String> replacements = new HashMap<>();
								replacements.put("id", contentId);
								replacements.put("operationKey", restriction.name());
								Map<String, Object> query = new HashMap<>();
								query.put("accountId", removeUser);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_RESTRICTION_REMOVE_USER, replacements, 
										HttpMethod.DELETE, admin, password, null, null, query, null);
								if (logResult(false, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(false, data, ex.getMessage());
							}
						}
					}
				}
				if (removeGroups != null) {
					for (String removeGroup : removeGroups) { 
						for (CloudContentRestriction restriction : restrictionList) {
							total++;
							AddRestriction data = new AddRestriction(contentId, restriction.name(), false, removeGroup);
							try {
								Map<String, String> replacements = new HashMap<>();
								replacements.put("id", contentId);
								replacements.put("operationKey", restriction.name());
								replacements.put("groupId", removeGroup);
								Response resp = WebRequest.invoke(
										scheme, host, PATH_CLOUD_RESTRICTION_REMOVE_GROUP, replacements, 
										HttpMethod.DELETE, admin, password, null, null, null, null);
								if (logResult(false, data, resp)) {
									success++;
								}
							} catch (Exception ex) {
								logError(false, data, ex.getMessage());
							}
						}
					}
				}
			}
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		Log.info(LOGGER, "Success/total: " + success + "/" + total);
		return true;
	}
	
	// Cached group and user names
	private static List<String> VALID_GROUPS = new ArrayList<String>();
	private static List<String> VALID_USERS = new ArrayList<String>();
	private static List<String> INVALID_GROUPS = new ArrayList<String>();
	private static List<String> INVALID_USERS = new ArrayList<String>();		
	
	private static boolean validateUser(
			String user, String scheme, String host, String admin, String password) {
		boolean result = true;
		if (VALID_USERS.contains(user)) {
			result = true;
		} else if (INVALID_USERS.contains(user)) {
			result = false;
		} else {
			// Validate with REST and cache result
			try {
				Map<String, Object> query = new HashMap<>();
				query.put("username", user);
				Response resp = WebRequest.invoke(
						scheme, host, PATH_DC_GET_USER, null, 
						HttpMethod.GET, admin, password, null, null, query, null);
				if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
					// Valid, cache result
					VALID_USERS.add(user);
					result = true;
				} else {
					// Invalid, cache result
					INVALID_USERS.add(user);
					// Remove it
					result = false;
				}
			} catch (Exception ex) {
				Log.error(LOGGER, 
						"Failed to validate user: " + user + ": " + ex.getMessage());
				result = true;
			}
		}
		return result;
	}
	
	private static boolean validateGroup(
			String group, String scheme, String host, String admin, String password) {
		boolean result = true;
		if (VALID_GROUPS.contains(group)) {
			result = true;
		} else if (INVALID_GROUPS.contains(group)) {
			result = false;
		} else {
			// Validate with REST and cache result
			try {
				Map<String, String> replacement = new HashMap<>();
				replacement.put("groupName", group);
				Response resp = WebRequest.invoke(
						scheme, host, PATH_DC_GET_GROUP, replacement, 
						HttpMethod.GET, admin, password, null, null, null, null);
				if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
					// Valid, cache result
					VALID_GROUPS.add(group);
					result = true;
				} else {
					// Invalid, cache result
					INVALID_GROUPS.add(group);
					// Remove it
					result = false;
				}
			} catch (Exception ex) {
				Log.error(LOGGER, 
						"Failed to validate group: " + group + ": " + ex.getMessage());
				result = true;
			}
		}
		return result;
	}
	
	private static boolean processCloudContentList(String[] args) {
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(cloudContentListOptions, args, true);
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
			// Convert space keys to space id = space key mapping
			Map<String, String> spaceIdToKeyMap = new HashMap<>();
			String[] spaceKeys = cmd.getOptionValues(spaceKeyOption);
			// Get spaces
			try {
				Map<String, Object> query = new HashMap<>();
				if (spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0])) {
					Log.info(LOGGER, "Wildcard space found, all spaces will be processed");
				}
				if (spaceKeys != null && !(spaceKeys.length == 1 && WILDCARD.equals(spaceKeys[0]))) {
					// Get only specified spaces
					StringBuilder sb = new StringBuilder();
					for (String spaceKey : spaceKeys) {
						sb.append(",").append(spaceKey);
					}
					if (sb.length() != 0) {
						query.put("keys", sb.toString().substring(1));
					}
				}
				List<SpaceObject> spaceObjects = WebRequest.fetchObjectsWithCursor(
						scheme, host, PATH_CLOUD_GET_SPACES, null, 
						HttpMethod.GET, admin, password, null, null, query, null, SpaceObjects.class);
				for (SpaceObject so : spaceObjects) {
					spaceIdToKeyMap.put(so.getId(), so.getKey());
					Log.info(LOGGER, "Space found: " + so.getKey() + " = " + so.getId());
				}
				if (spaceIdToKeyMap.size() == 0) {
					Log.error(LOGGER, "No specified space(s) can be found");
					return true;
				}
			} catch (Exception ex) {
				Log.error(LOGGER, "Failed to retrieve space list", ex);
				return true;
			}
			// Content ID to space ID map
			Map<Page, String> contentMap = new HashMap<>();
			String[] contentIds = cmd.getOptionValues(contentKeyOption);
			if (contentIds.length == 1 && WILDCARD.equals(contentIds[0])) {
				// Get all pages, one space at a time
				for (Map.Entry<String, String> space : spaceIdToKeyMap.entrySet()) {
					Map<String, Object> query = new HashMap<>();
					query.put("space-id", space.getKey());
					try {
						List<Page> pages = WebRequest.fetchObjectsWithCursor(
							scheme, host, PATH_CLOUD_GET_PAGES, null, 
							HttpMethod.GET, admin, password, null, null, query, null, Pages.class);
						for (Page page : pages) {
							Log.info(LOGGER, 
									"Space: " + space.getKey() + " = " + space.getValue() + 
									" Page found: " + page.getTitle() + " = " + page.getId());
							contentMap.put(page, space.getKey());
						}
					} catch (Exception ex) {
						Log.error(LOGGER, 
								"Unable to retrieve page list for space " + 
								space.getKey() + " = " + space.getValue(), 
								ex);
					}
				}
			} else {
				for (String contentId : contentIds) {
					// Resolve content id into Page objects
					try {
						Map<String, String> replacements = new HashMap<>();
						replacements.put("id", contentId);
						Response resp = WebRequest.invoke(
								scheme, host, PATH_CLOUD_GET_PAGE_BY_ID, replacements, 
								HttpMethod.GET, admin, password, null, null, null, null);
						if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
							Page page = resp.readEntity(Page.class);
							contentMap.put(page, page.getSpaceId());
							Log.info(LOGGER, "Page found: " + page.getTitle() + " = " + page.getId());
						} else {
							String msg = resp.getStatus() + ": " + resp.readEntity(String.class);
							Log.error(LOGGER, "Failed to locate page " + contentId + ": " + msg);
							break;
						}
					} catch (Exception ex) {
						Log.error(LOGGER, "Failed to locate page " + contentId + ", page will be excluded", ex);
					}
				}
			}
			// Get page restrictions
			Path output = Paths.get("CloudPageRestrictions." + SDF.format(new Date()) + ".csv");
			try (	FileWriter fw = CSV.getCSVFileWriter(output); 
					CSVPrinter printer = new CSVPrinter(fw, CSV.getCSVWriteFormat(Arrays.asList(
							"SPACE", "SPACEID", "PAGE", "PAGEID", "OPERATION", "TYPE", "NAME", "ID", "ERROR")))) {
				for (Map.Entry<Page, String> content : contentMap.entrySet()) {
					String pageId = content.getKey().getId();
					String pageTitle = content.getKey().getTitle();
					String spaceId = content.getValue();
					String spaceKey = spaceIdToKeyMap.get(spaceId);
					boolean hasMore = true;
					int startAt = 0;
					try {
						Map<String, String> replacements = new HashMap<>();
						replacements.put("id", pageId);
						while (hasMore) {
							Map<String, Object> query = new HashMap<>();
							query.put("start", startAt);
							Response resp = WebRequest.invoke(scheme, host, PATH_CLOUD_GET_RESTRICTION, replacements, 
									HttpMethod.GET, admin, password, null, null, query, null);
							if ((resp.getStatus() & HttpStatus.SC_OK) == HttpStatus.SC_OK) {
								ContentRestrictions restrictions = resp.readEntity(ContentRestrictions.class);
								int userCount = 0;
								int groupCount = 0;
								for (ContentRestriction restriction : restrictions.getResults()) {
									String operation = restriction.getOperation();
									if (restriction.getRestrictions().getUser() != null) {
										for (RestrictionUser user : restriction.getRestrictions().getUser().getResults()) {
											String userName = user.getDisplayName();
											String accountId = user.getAccountId();
											String type = user.getType();
											CSV.printRecord(printer, 
													spaceKey, spaceId, pageTitle, pageId, 
													operation, type, userName, accountId, "");
											Log.info(LOGGER, "Space: " + spaceKey + " = " + spaceId + 
													" Page: " + pageTitle + " = " + pageId + 
													" Operatoin: " + operation + 
													" Type: " + type + 
													" Name: " + userName + " = " + accountId);
											userCount++;
										}
									}
									if (restriction.getRestrictions().getGroup() != null) {
										for (RestrictionGroup group : restriction.getRestrictions().getGroup().getResults()) {
											String groupName = group.getName();
											String groupId = group.getId();
											String type = group.getType();
											CSV.printRecord(printer, 
													spaceKey, spaceId, pageTitle, pageId, 
													operation, type, groupName, groupId, "");
											Log.info(LOGGER, "Space: " + spaceKey + " = " + spaceId + 
													" Page: " + pageTitle + " = " + pageId + 
													" Operatoin: " + operation + 
													" Type: " + type + 
													" Name: " + groupName + " = " + groupId);
											groupCount++;
										}
									}
									hasMore = (groupCount != 0) || (userCount != 0);
									startAt += Math.max(userCount, groupCount);
								}
							} else {
								String msg = resp.getStatus() + ": " + resp.readEntity(String.class);
								CSV.printRecord(printer, 
										spaceKey, spaceId, pageTitle, pageId, 
										"N/A", "N/A", "N/A", "N/A", msg);
								Log.error(LOGGER, "Failed to read page restriction for Space " + 
										spaceKey + " = " + spaceId + 
										" Page " + pageTitle + " = " + pageId + ": " + msg);
								break;
							}	// Http status check
						}
					} catch (Exception ex) {
						CSV.printRecord(printer, 
								spaceKey, spaceId, pageTitle, pageId, 
								"N/A", "N/A", "N/A", "N/A", ex.getMessage());
						Log.error(LOGGER, "Failed to read page restriction for Space " + 
								spaceKey + " = " + spaceId + 
								" Page " + pageTitle + " = " + pageId,
								ex);
					}
				}	// For each content
			}	// Try resource
		} catch (ParseException pex) {
			// Ignore
			return false;
		} catch (IOException ioex) {
			Log.error(LOGGER, "Unable to read password", ioex);
		}
		return true;
	}
	
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args) {
			sb.append(" ").append(s);
		}
		Log.info(LOGGER, "Command line:" + sb.toString());
		if (processDataCenterConfluence(args)) {
			return;
		}
		if (processCloudConfluence(args)) {
			return;
		}
		if (processCloudJira(args)) {
			return;
		}
		if (processCloudContent(args)) {
			return;
		}
		if (processDataCenterContent(args)) {
			return;
		}
		if (processCloudContentList(args)) {
			return;
		}
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp("Manage space permission for Confluence Data Center/Server", dcConfluenceOptions);
		System.out.println("============================================================");
		hf.printHelp("Manage space permission for Confluence Cloud", cloudConfluenceOptions);
		System.out.println("============================================================");
		hf.printHelp("Manage content restriction for Confluence Cloud", cloudContentOptions);
		System.out.println("============================================================");
		hf.printHelp("Manage project permission for Jira Cloud", cloudJiraOptions);
		System.out.println("============================================================");
		hf.printHelp("Manage content restriction for Confluence Data Center/Server", dcContentOptions);
		System.out.println("============================================================");
		hf.printHelp("List content restriction for Confluence Cloud", cloudContentListOptions);
	}
}
