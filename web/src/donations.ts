import { saveError, useServices } from '@orudesk/web-core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNgoApi, type NgoSchemas } from './api';

export type PaymentMode = 'CASH' | 'CHEQUE' | 'BANK_TRANSFER' | 'UPI' | 'CARD' | 'ONLINE';

export const PAYMENT_MODES: { value: PaymentMode; label: string; reference: string | null }[] = [
  { value: 'UPI', label: 'UPI', reference: 'UPI transaction id' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer (NEFT, RTGS, IMPS)', reference: 'UTR number' },
  { value: 'CHEQUE', label: 'Cheque', reference: 'Cheque number' },
  { value: 'CASH', label: 'Cash', reference: null },
  { value: 'CARD', label: 'Card', reference: 'Transaction id' },
  { value: 'ONLINE', label: 'Online (payment gateway)', reference: 'Payment id' },
];

export const modeLabel = (mode: string) =>
  PAYMENT_MODES.find((m) => m.value === mode)?.label ?? mode;

/** India's financial year of a date: "2026-27" for 1 April 2026 to 31 March 2027. */
export function financialYearOf(date: Date): string {
  const start = date.getMonth() >= 3 ? date.getFullYear() : date.getFullYear() - 1;
  return `${start}-${String((start + 1) % 100).padStart(2, '0')}`;
}

/** Today in the browser's time zone, as the API's dates are written: "2026-10-29". */
export function today(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Everything one donor gave, newest first, and the total (GET /api/v1/donors/{id}/donations). */
export function useDonorDonations(donorId: string) {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donations', 'of-donor', donorId],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/donors/{donorId}/donations', {
        params: { path: { donorId } },
      });
      if (error) throw new Error('Could not load the donations');
      return data;
    },
  });
}

/** Records a donation; resolves with its id. ValidationError for refused fields, as everywhere. */
export function useRecordDonation() {
  const api = useNgoApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (donation: NgoSchemas['RecordDonationRequest']) => {
      const { data, error, response } = await api.POST('/api/v1/donations', { body: donation });
      if (data) return data.id;
      throw saveError(response, error, 'The donation was not saved.');
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['donations'] }),
  });
}

/** The receipt as printed (GET /api/v1/donations/{id}/receipt); null when there's no such donation. */
export function useReceipt(donationId: string) {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donations', 'receipt', donationId],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/api/v1/donations/{donationId}/receipt', {
        params: { path: { donationId } },
      });
      if (response.status === 404 || response.status === 400) return null;
      if (error) throw new Error('Could not load the receipt');
      return data;
    },
  });
}

/** The donation itself, for its version: cancelling sends back the version that was read. */
export function useDonation(donationId: string) {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donations', 'one', donationId],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/donations/{donationId}', {
        params: { path: { donationId } },
      });
      if (error) throw new Error('Could not load the donation');
      return data;
    },
  });
}

export function useCancelDonation(donationId: string) {
  const api = useNgoApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (cancel: NgoSchemas['CancelDonationRequest']) => {
      const { data, error, response } = await api.POST('/api/v1/donations/{donationId}/cancel', {
        params: { path: { donationId } },
        body: cancel,
      });
      if (data) return data;
      throw saveError(response, error, 'The donation was not cancelled.');
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['donations'] }),
  });
}

export function useDonationSummary(financialYear: string) {
  const api = useNgoApi();
  return useQuery({
    queryKey: ['donations', 'summary', financialYear],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/donations/summary', {
        params: { query: { financialYear } },
      });
      if (error) throw new Error('Could not load the summary');
      return data;
    },
  });
}

/**
 * Downloads the Form 10BD CSV. A plain link can't carry the access token, so the file is fetched with the
 * core's fetch (token, org unit and all) and handed to the browser as a blob.
 */
export function useDownloadForm10BD() {
  const { fetch } = useServices();
  return useMutation({
    mutationFn: async (financialYear: string) => {
      const url = `${globalThis.location.origin}/api/v1/donations/form-10bd?financialYear=${encodeURIComponent(financialYear)}`;
      const response = await fetch(new Request(url, { headers: { Accept: 'text/csv' } }));
      if (!response.ok) {
        const problem = (await response.json().catch(() => null)) as { detail?: string } | null;
        throw new Error(problem?.detail ?? 'The file could not be made.');
      }
      const blob = await response.blob();
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `form-10bd-${financialYear}.csv`;
      link.click();
      URL.revokeObjectURL(link.href);
    },
  });
}
