import React from "react";
import useAppStore from "@/store/AppStore";
import { DEFAULT_WORKSPACE_NAME } from "@/constants/user";
import { useWorkspaceNameFromURL } from "@/hooks/useWorkspaceNameFromURL";
import { Navigate } from "@tanstack/react-router";
import useCheckSession from "@/api/auth/useCheckSession";
import Loader from "@/components/shared/Loader/Loader";

type WorkspacePreloaderProps = {
  children: React.ReactNode;
};

const WorkspacePreloader: React.FunctionComponent<WorkspacePreloaderProps> = ({
  children,
}) => {
  const { isLoading, isError } = useCheckSession();

  useAppStore.getState().setActiveWorkspaceName(DEFAULT_WORKSPACE_NAME);
  const workspaceNameFromURL = useWorkspaceNameFromURL();

  // Show loading while checking session
  if (isLoading) {
    return <Loader />;
  }

  // Redirect to login if session is invalid
  if (isError) {
    window.location.href = "/login";
    return null;
  }

  if (workspaceNameFromURL && workspaceNameFromURL !== DEFAULT_WORKSPACE_NAME) {
    return (
      <Navigate
        to="/$workspaceName"
        params={{ workspaceName: DEFAULT_WORKSPACE_NAME }}
      />
    );
  }

  return children;
};

export default WorkspacePreloader;
