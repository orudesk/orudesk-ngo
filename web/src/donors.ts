import { saveError } from '@orudesk/web-core';
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNgoApi, type NgoSchemas } from './api';

export type DonorType = NgoSchemas['DonorListItem']['type'];

export const DONOR_TYPES: { value: DonorType; label: string }[] = [
  { value: 'INDIVIDUAL', label: 'Individual' },
  { value: 'CORPORATE', label: 'Corporate' },
  { value: 'FOUNDATION', label: 'Foundation' },
];

export const typeLabel = (type: DonorType) =>
  DONOR_TYPES.find((t) => t.value === type)?.label ?? type;

/** Donors, a page at a time, by name; optionally only one type (GET /api/v1/donors). */
export function useDonors(search: string, type: DonorType | null) {
  const api = useNgoApi();
  return useInfiniteQuery({
    queryKey: ['donors', 'list', search, type],
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET('/api/v1/donors', {
        params: {
          query: {
            size: 20,
            ...(search && { q: search }),
            ...(type && { type }),
            ...(pageParam && { after: pageParam }),
          },
        },
      });
      if (error) throw new Error('Could not load the donors');
      return data;
    },
    getNextPageParam: (page) => page.nextCursor ?? undefined,
  });
}

/** How many donors of each type (GET /api/v1/donors/counts, from the Day 23 database view). */
export function useDonorCounts() {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donors', 'counts'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/donors/counts');
      if (error) throw new Error('Could not load the counts');
      return data;
    },
  });
}

/** One donor; null when there is no such donor. */
export function useDonor(donorId: string) {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donors', 'one', donorId],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/donors/{donorId}', {
        params: { path: { donorId } },
      });
      if (response.status === 404 || response.status === 400) return null;
      if (error) throw new Error('Could not load this donor');
      return data;
    },
  });
}

export function useRegisterDonor() {
  const api = useNgoApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (donor: NgoSchemas['RegisterDonorRequest']) => {
      const { data, error, response } = await api.POST('/api/v1/donors', { body: donor });
      if (data) return data.id;
      throw saveError(response, error, 'The donor was not saved.');
    },
    // The list and the counts both change; "people" too, because a donor is also a person in the registry
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['donors'] }),
        queryClient.invalidateQueries({ queryKey: ['people'] }),
      ]),
  });
}

/**
 * Saves the donor form (PUT, Day 49) with both versions it read: the donor's and the person's. The core's
 * EditConflictError if either was saved by someone else in between, ValidationError for refused fields.
 */
export function useEditDonor(donorId: string) {
  const api = useNgoApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (donor: NgoSchemas['EditDonorRequest']) => {
      const { data, error, response } = await api.PUT('/api/v1/donors/{donorId}', {
        params: { path: { donorId } },
        body: donor,
      });
      if (data) return data;
      throw saveError(response, error, 'The changes were not saved.');
    },
    onSuccess: (donor) => {
      queryClient.setQueryData(['donors', 'one', donorId], donor);
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['donors', 'list'] }),
        queryClient.invalidateQueries({ queryKey: ['donors', 'counts'] }),
        queryClient.invalidateQueries({ queryKey: ['people'] }),
        queryClient.invalidateQueries({ queryKey: ['audit'] }),
      ]);
    },
  });
}
