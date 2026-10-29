import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App, buildRoutes, coreModule, createServices } from '@orudesk/web-core';
import { fakeApi, json, tokens } from '@orudesk/web-core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ngoModule } from './ngoModule';

const manager = {
  id: 'u1',
  tenantId: 't1',
  email: 'meera@helping-hands.org',
  roles: ['USER', 'DONOR_MANAGER'],
  grants: [{ role: 'USER' }, { role: 'DONOR_MANAGER' }],
  enabled: true,
};
const asha = {
  id: 'd1',
  firstName: 'Asha',
  lastName: 'Iyer',
  phone: '+91 98765 43210',
  type: 'INDIVIDUAL',
  wantsTaxReceipt: true,
};

function openAt(path: string, handlers: Parameters<typeof fakeApi>[0]) {
  window.history.pushState({}, '', path);
  const api = fakeApi({
    'POST /api/v1/auth/refresh': () => tokens(),
    'GET /api/v1/users/me': () => json(manager),
    'GET /api/v1/org-units': () =>
      json({ id: 'g', name: 'Helping Hands', code: 'HH', type: 'GROUP', children: [] }),
    'GET /api/v1/donors/counts': () =>
      json([
        { type: 'INDIVIDUAL', donors: 12, wantingTaxReceipts: 5 },
        { type: 'CORPORATE', donors: 2, wantingTaxReceipts: 2 },
      ]),
    ...handlers,
  });
  render(<App services={createServices()} modules={[coreModule, ngoModule]} />);
  return api;
}

describe('the NGO edition', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  it("fits next to the core's screens", () => {
    expect(() => buildRoutes([coreModule, ngoModule])).not.toThrow();
  });

  it('shows donor managers the Donors menu, the counts and the list', async () => {
    openAt('/donors', {
      'GET /api/v1/donors': () => json({ items: [asha], nextCursor: null }),
    });

    expect(await screen.findByRole('link', { name: 'Iyer, Asha' })).toHaveAttribute(
      'href',
      '/donors/d1',
    );
    expect(screen.getByRole('link', { name: 'Donors' })).toBeInTheDocument();
    // a donor manager is not an admin: the core's admin screens stay hidden
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument();
    const counts = screen.getByLabelText('Donors by type');
    expect(within(counts).getByText('12')).toBeInTheDocument();
    expect(within(counts).getByText('0')).toBeInTheDocument(); // no foundations yet
  });

  it('filters by type', async () => {
    const api = openAt('/donors', {
      'GET /api/v1/donors': () => json({ items: [asha], nextCursor: null }),
    });
    await screen.findByRole('link', { name: 'Iyer, Asha' });

    await userEvent.click(screen.getByRole('radio', { name: 'Corporate' }));

    await vi.waitFor(() =>
      expect(
        api.requests.some((r) => new URL(r.url).searchParams.get('type') === 'CORPORATE'),
      ).toBe(true),
    );
  });

  it('adds a donor, upper-casing the PAN, and shows the server’s field errors', async () => {
    let attempt = 0;
    const api = openAt('/donors/new', {
      'POST /api/v1/donors': () =>
        ++attempt === 1
          ? json({ title: 'Validation failed', errors: { pan: 'must look like ABCDE1234F' } }, 400)
          : json({ id: 'd2' }, 201),
      'GET /api/v1/donors/d2': () =>
        json({
          id: 'd2',
          personId: 'p2',
          firstName: 'Ravi',
          lastName: 'Shah',
          phone: null,
          email: null,
          type: 'INDIVIDUAL',
          panMasked: 'XXXXXX234F',
          wantsTaxReceipt: true,
          receivesUpdates: false,
          registeredOn: '2026-10-26',
        }),
    });

    await userEvent.type(await screen.findByLabelText(/First name/), 'Ravi');
    await userEvent.type(screen.getByLabelText(/Last name/), 'Shah');
    await userEvent.type(screen.getByLabelText(/PAN/), 'abc');
    await userEvent.click(screen.getByLabelText('Wants 80G tax receipts'));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('must look like ABCDE1234F')).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText(/PAN/));
    await userEvent.type(screen.getByLabelText(/PAN/), 'abcde1234f');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('heading', { name: 'Ravi Shah' })).toBeInTheDocument();
    expect(screen.getByText('XXXXXX234F')).toBeInTheDocument();
    const sent = api.requests.filter((r) => r.method === 'POST').at(-1)!;
    expect(await sent.json()).toMatchObject({
      pan: 'ABCDE1234F',
      type: 'INDIVIDUAL',
      wantsTaxReceipt: true,
      receivesUpdates: false,
    });
  });

  describe('editing a donor', () => {
    const meera = {
      id: 'd2',
      personId: 'p2',
      firstName: 'Meera',
      lastName: 'Iyer',
      dateOfBirth: '1980-05-01',
      phone: '+91 98450 12345',
      email: null,
      type: 'INDIVIDUAL',
      panMasked: 'XXXXXX234F',
      wantsTaxReceipt: true,
      receivesUpdates: false,
      registeredOn: '2026-10-01',
      version: 3,
      personVersion: 7,
    };

    it('sends both versions, keeps the PAN when left empty, and shows the saved donor', async () => {
      const api = openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () =>
          json({ ...meera, type: 'CORPORATE', version: 4, personVersion: 7 }),
      });

      await userEvent.click(await screen.findByRole('combobox', { name: 'Type' }));
      await userEvent.click(await screen.findByRole('option', { name: 'Corporate', hidden: true }));
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      expect(await screen.findByRole('heading', { name: 'Meera Iyer' })).toBeInTheDocument();
      expect(window.location.pathname).toBe('/donors/d2');
      const sent = api.requests.find((r) => r.method === 'PUT')!;
      expect(await sent.json()).toEqual({
        version: 3,
        personVersion: 7,
        firstName: 'Meera',
        lastName: 'Iyer',
        dateOfBirth: '1980-05-01',
        phone: '+91 98450 12345',
        email: null,
        type: 'CORPORATE',
        pan: null,
        wantsTaxReceipt: true,
        receivesUpdates: false,
      });
    });

    it('sends an empty PAN to remove it', async () => {
      const api = openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () => json({ ...meera, panMasked: null, wantsTaxReceipt: false }),
      });

      await userEvent.click(await screen.findByLabelText('Remove the PAN'));
      await userEvent.click(screen.getByLabelText('Wants 80G tax receipts'));
      await userEvent.click(screen.getByRole('button', { name: 'Save' }));

      await screen.findByRole('heading', { name: 'Meera Iyer' });
      const sent = (await api.requests.find((r) => r.method === 'PUT')!.json()) as {
        pan: string | null;
        wantsTaxReceipt: boolean;
      };
      expect(sent.pan).toBe('');
      expect(sent.wantsTaxReceipt).toBe(false);
    });

    it('offers a reload when someone else saved first', async () => {
      openAt('/donors/d2/edit', {
        'GET /api/v1/donors/d2': () => json(meera),
        'PUT /api/v1/donors/d2': () =>
          json({ title: 'Edit conflict', detail: 'donor d2 was changed', currentVersion: 4 }, 409),
      });

      await userEvent.click(await screen.findByRole('button', { name: 'Save' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('Changed by someone else');
      expect(screen.getByRole('button', { name: 'Reload' })).toBeInTheDocument();
    });
  });

  describe('donations', () => {
    const ravi = {
      id: 'd3',
      personId: 'p3',
      firstName: 'Ravi',
      lastName: 'Shah',
      dateOfBirth: null,
      phone: null,
      email: null,
      type: 'INDIVIDUAL',
      panMasked: 'XXXXXX234F',
      wantsTaxReceipt: true,
      receivesUpdates: false,
      registeredOn: '2026-10-01',
      version: 0,
      personVersion: 0,
    };
    const donation = {
      id: 'g1',
      donorId: 'd3',
      donorName: 'Ravi Shah',
      amount: '150000.00',
      amountFormatted: '₹1,50,000.00',
      receivedOn: '2026-10-28',
      mode: 'UPI',
      reference: 'UPI123',
      purpose: null,
      receiptNumber: '2026-27/000001',
      cancelledOn: null,
      cancelReason: null,
      version: 0,
    };
    const receipt = {
      donationId: 'g1',
      receiptNumber: '2026-27/000001',
      financialYear: '2026-27',
      issuedOn: '2026-10-28',
      organisationName: 'Helping Hands Trust',
      organisationAddress: 'Pune',
      organisationPan: 'AAATH1234C',
      registration80G: 'AAATH1234CF20214',
      donorName: 'Ravi Shah',
      donorPan: 'ABCDE1234F',
      amount: '150000.00',
      amountFormatted: '₹1,50,000.00',
      amountInWords: 'Rupees One Lakh Fifty Thousand Only',
      receivedOn: '2026-10-28',
      mode: 'UPI',
      reference: 'UPI123',
      purpose: null,
      taxDeductible: true,
      notDeductibleBecause: '',
      cancelledOn: null,
      cancelReason: null,
    };

    it("lists a donor's donations with their receipts and the total", async () => {
      openAt('/donors/d3', {
        'GET /api/v1/donors/d3': () => json(ravi),
        'GET /api/v1/donors/d3/donations': () =>
          json({ donations: [donation], total: '150000.00', totalFormatted: '₹1,50,000.00' }),
      });

      const table = await screen.findByRole('table', { name: 'Donations' });
      expect(within(table).getByRole('link', { name: '2026-27/000001' })).toHaveAttribute(
        'href',
        '/donations/g1/receipt',
      );
      expect(within(table).getAllByText('₹1,50,000.00')).toHaveLength(2); // the row and the total
      expect(screen.getByRole('link', { name: 'Record a donation' })).toHaveAttribute(
        'href',
        '/donors/d3/donations/new',
      );
    });

    it('records a donation, sending the amount as typed without commas, and opens its receipt', async () => {
      const api = openAt('/donors/d3/donations/new', {
        'GET /api/v1/donors/d3': () => json(ravi),
        'POST /api/v1/donations': () => json({ id: 'g1' }, 201),
        'GET /api/v1/donations/g1/receipt': () => json(receipt),
        'GET /api/v1/donations/g1': () => json(donation),
      });

      await userEvent.type(await screen.findByLabelText(/Amount/), '1,50,000.00');
      await userEvent.type(screen.getByLabelText(/UPI transaction id/), 'UPI123');
      await userEvent.click(screen.getByRole('button', { name: 'Save and show receipt' }));

      expect(await screen.findByText('Rupees One Lakh Fifty Thousand Only')).toBeInTheDocument();
      expect(window.location.pathname).toBe('/donations/g1/receipt');
      const sent = (await api.requests
        .find((r) => r.method === 'POST' && r.url.endsWith('/donations'))!
        .json()) as {
        amount: string;
        mode: string;
        reference: string;
      };
      expect(sent).toMatchObject({
        donorId: 'd3',
        amount: '150000.00',
        mode: 'UPI',
        reference: 'UPI123',
      });
    });

    it('cancels a donation with the version that was read and a reason', async () => {
      let cancelled = false;
      const api = openAt('/donations/g1/receipt', {
        'GET /api/v1/donations/g1/receipt': () =>
          json(
            cancelled
              ? { ...receipt, cancelledOn: '2026-10-29', cancelReason: 'Cheque bounced' }
              : receipt,
          ),
        'GET /api/v1/donations/g1': () => json(donation),
        'POST /api/v1/donations/g1/cancel': () => {
          cancelled = true;
          return json({
            ...donation,
            cancelledOn: '2026-10-29',
            cancelReason: 'Cheque bounced',
            version: 1,
          });
        },
      });

      await userEvent.type(await screen.findByLabelText(/Reason/), 'Cheque bounced');
      await userEvent.click(screen.getByRole('button', { name: 'Cancel donation' }));

      expect(await screen.findByText(/Cancelled on .*Cheque bounced/)).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Cancel donation' })).not.toBeInTheDocument();
      const sent = await api.requests.find((r) => r.url.endsWith('/cancel'))!.json();
      expect(sent).toEqual({ version: 0, reason: 'Cheque bounced' });
    });

    it('shows the year by month and by mode', async () => {
      openAt('/donations/reports', {
        'GET /api/v1/donations/summary': () =>
          json({
            financialYear: '2026-27',
            total: '150000.00',
            totalFormatted: '₹1,50,000.00',
            count: 1,
            cancelledCount: 0,
            byMonth: [
              { key: '2026-04', count: 0, total: '0.00' },
              { key: '2026-10', count: 1, total: '150000.00' },
            ],
            byMode: [{ key: 'UPI', count: 1, total: '150000.00' }],
          }),
      });

      expect(await screen.findByText('Oct 2026')).toBeInTheDocument();
      const byMode = screen.getByRole('table', { name: 'By payment mode' });
      expect(within(byMode).getByText('UPI')).toBeInTheDocument();
      expect(within(byMode).getByText('₹1,50,000.00')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Donation reports' })).toBeInTheDocument();
    });
  });
});
