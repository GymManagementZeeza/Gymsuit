import { authFetch } from "@/lib/auth";

export type Trainer = {
  id: number;
  gymId: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string;
  specialization: string | null;
  bio: string | null;
  hireDate: string | null;
  imageUrl: string | null;
  certificateUrl: string | null;
};

export type TrainerInput = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  specialization?: string;
  bio?: string;
  hireDate?: string;
  imageUrl?: string;
  certificateUrl?: string;
};

export function listTrainers(gymId: number) {
  return authFetch<Trainer[]>(`/api/gyms/${gymId}/trainers`);
}

export function getTrainer(gymId: number, id: number) {
  return authFetch<Trainer>(`/api/gyms/${gymId}/trainers/${id}`);
}

export function createTrainer(gymId: number, input: TrainerInput) {
  return authFetch<Trainer>(`/api/gyms/${gymId}/trainers`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateTrainer(gymId: number, id: number, input: TrainerInput) {
  return authFetch<Trainer>(`/api/gyms/${gymId}/trainers/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteTrainer(gymId: number, id: number) {
  return authFetch<void>(`/api/gyms/${gymId}/trainers/${id}`, {
    method: "DELETE",
  });
}
