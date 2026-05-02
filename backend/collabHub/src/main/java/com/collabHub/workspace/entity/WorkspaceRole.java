package com.collabHub.workspace.entity;

/*
 * WorkspaceRole - Workspace-specific roles
 */
public enum WorkspaceRole {
    /**
     * Regular member of the workspace
     * Cannot manage members or workspace settings
     */
    MEMBER,

    /**
     * Owner of the workspace
     * Full control within this workspace
     */
    OWNER
}
