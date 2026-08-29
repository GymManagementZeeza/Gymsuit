import { authFetch } from "@/lib/auth";

export type ManagerAccessScope = "FINANCE" | "SETTINGS" | "TRAINERS";

export type TeamManager = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  scopes: ManagerAccessScope[];
};

export type InviteManagerInput = {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  scopes: ManagerAccessScope[];
};

export function listTeamManagers(gymId: number) {
  return authFetch<TeamManager[]>(`/api/gyms/${gymId}/team/managers`);
}

export function inviteManager(gymId: number, input: InviteManagerInput) {
  return authFetch<TeamManager>(`/api/gyms/${gymId}/team/managers`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateManagerAccess(gymId: number, managerId: number, scopes: ManagerAccessScope[]) {
  return authFetch<TeamManager>(`/api/gyms/${gymId}/team/managers/${managerId}/access`, {
    method: "PUT",
    body: JSON.stringify({ scopes }),
  });
}

export function removeManager(gymId: number, managerId: number) {
  return authFetch<void>(`/api/gyms/${gymId}/team/managers/${managerId}`, {
    method: "DELETE",
  });
}
