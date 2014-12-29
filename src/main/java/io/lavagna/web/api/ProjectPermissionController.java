/**
 * This file is part of lavagna.
 *
 * lavagna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lavagna is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with lavagna.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lavagna.web.api;

import io.lavagna.model.Permission;
import io.lavagna.model.PermissionCategory;
import io.lavagna.model.Project;
import io.lavagna.model.Role;
import io.lavagna.model.RoleAndMetadata;
import io.lavagna.model.User;
import io.lavagna.service.EventEmitter;
import io.lavagna.service.PermissionService;
import io.lavagna.service.PermissionService.RoleAndPermissions;
import io.lavagna.service.ProjectService;
import io.lavagna.web.api.model.CreateRole;
import io.lavagna.web.api.model.UpdateRole;
import io.lavagna.web.api.model.Users;
import io.lavagna.web.helper.ExpectPermission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ExpectPermission(Permission.PROJECT_ADMINISTRATION)
public class ProjectPermissionController {

	private final PermissionService permissionService;
	private final EventEmitter eventEmitter;
	private final ProjectService projectService;

	@Autowired
	public ProjectPermissionController(PermissionService permissionService, EventEmitter eventEmitter,
			ProjectService projectService) {
		this.permissionService = permissionService;
		this.eventEmitter = eventEmitter;
		this.projectService = projectService;
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role", method = RequestMethod.GET)
	public Map<String, RoleAndPermissions> findAllRolesAndRelatedPermissions(
			@PathVariable("projectShortName") String projectShortName) {
		Project project = projectService.findByShortName(projectShortName);
		return permissionService.findAllRolesAndRelatedPermissionInProjectId(project.getId());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role", method = RequestMethod.POST)
	public int createRole(@PathVariable("projectShortName") String projectShortName, @RequestBody CreateRole newRole) {
		Project project = projectService.findByShortName(projectShortName);
		int res = permissionService.createRoleInProjectId(new Role(newRole.getName()), project.getId());
		eventEmitter.emitCreateRole(project.getShortName());
		return res;
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/{roleName}", method = RequestMethod.POST)
	public void updateRole(@PathVariable("projectShortName") String projectShortName,
			@PathVariable("roleName") String roleName, @RequestBody UpdateRole updateRole) {

		Project project = projectService.findByShortName(projectShortName);
		RoleAndMetadata role = permissionService.findRoleInProjectByName(project.getId(),
				roleName);

		Validate.isTrue(!role.isReadOnly());

		permissionService.updatePermissionsToRoleInProjectId(new Role(roleName), updateRole.getPermissions(),
				project.getId());
		eventEmitter.emitUpdatePermissionsToRole(project.getShortName());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/{roleName}", method = RequestMethod.DELETE)
	public void deleteRole(@PathVariable("projectShortName") String projectShortName,
			@PathVariable("roleName") String roleName) {
		Project project = projectService.findByShortName(projectShortName);

		RoleAndMetadata role = permissionService.findRoleInProjectByName(project.getId(),
				roleName);

		Validate.isTrue(role.isRemovable());

		permissionService.deleteRoleInProjectId(new Role(roleName), project.getId());
		eventEmitter.emitDeleteRole(project.getShortName());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/{roleName}/users/", method = RequestMethod.GET)
	public List<User> findUserByRole(@PathVariable("projectShortName") String projectShortName,
			@PathVariable("roleName") String roleName) {
		Project project = projectService.findByShortName(projectShortName);
		return permissionService.findUserByRoleAndProjectId(new Role(roleName), project.getId());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/{roleName}/users/", method = RequestMethod.POST)
	public void assignUsersToRole(@PathVariable("projectShortName") String projectShortName,
			@PathVariable("roleName") String roleName, @RequestBody Users usersToAdd) {

		Project project = projectService.findByShortName(projectShortName);
		permissionService.assignRoleToUsersInProjectId(new Role(roleName), usersToAdd.getUserIds(), project.getId());
		eventEmitter.emitAssignRoleToUsers(roleName, project.getShortName());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/{roleName}/remove/", method = RequestMethod.POST)
	public void removeRoleToUsers(@PathVariable("projectShortName") String projectShortName,
			@PathVariable("roleName") String roleName, @RequestBody Users usersToRemove) {

		Project project = projectService.findByShortName(projectShortName);
		permissionService.removeRoleToUsersInProjectId(new Role(roleName), usersToRemove.getUserIds(), project.getId());
		eventEmitter.emitRemoveRoleToUsers(roleName, project.getShortName());
	}

	@RequestMapping(value = "/api/project/{projectShortName}/role/available-permissions", method = RequestMethod.GET)
	public Map<PermissionCategory, List<Permission>> existingPermissions(
			@PathVariable("projectShortName") String projectShortName) {
		Map<PermissionCategory, List<Permission>> byCategory = new LinkedHashMap<>();
		for (PermissionCategory pc : PermissionCategory.values()) {
			if (!pc.isOnlyForBase()) {
				byCategory.put(pc, new ArrayList<Permission>());
			}
		}
		for (Permission permission : Permission.values()) {
			if (!permission.isOnlyForBase() && byCategory.containsKey(permission.getCategory())) {
				byCategory.get(permission.getCategory()).add(permission);
			}
		}
		return byCategory;
	}

}
