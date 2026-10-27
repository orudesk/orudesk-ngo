import { useServices } from '@orudesk/web-core';
import createClient, { type Client } from 'openapi-fetch';
import { useMemo } from 'react';
import type { components, paths } from './generated/schema';

/** The NGO edition's JSON shapes by name: `NgoSchemas['DonorDetails']`. */
export type NgoSchemas = components['schemas'];

/**
 * A client typed from the NGO edition's contract (the core's endpoints plus the donors'), sending through the
 * core's fetch: the access token, the org unit and the outage banner come with it.
 */
export function useNgoApi(): Client<paths> {
  const { fetch } = useServices();
  return useMemo(
    () => createClient<paths>({ baseUrl: globalThis.location?.origin ?? '', fetch }),
    [fetch],
  );
}
