import { useQuery, QueryFunctionContext } from "@tanstack/react-query";
import api, { QueryConfig } from "@/api/api";

export interface SessionUser {
  username: string;
  workspace_name: string;
}

const checkSession = async ({ signal }: QueryFunctionContext) => {
  const { data } = await api.get<SessionUser>("/v1/private/auth/check", {
    signal,
  });
  return data;
};

export default function useCheckSession(options?: QueryConfig<SessionUser>) {
  return useQuery({
    queryKey: ["session", {}],
    queryFn: (context) => checkSession(context),
    retry: false,
    ...options,
  });
}


