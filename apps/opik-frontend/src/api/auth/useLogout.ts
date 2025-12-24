import { useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/api/api";

const logout = async () => {
  await api.post("/v1/private/auth/logout");
};

export default function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      // Clear all queries on logout
      queryClient.clear();
      // Redirect to login page
      window.location.href = "/login";
    },
  });
}


